package echonest.sociogram.connectus.Fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import echonest.sociogram.connectus.Adapters.AdapterChatlist;
import echonest.sociogram.connectus.Adapters.AdapterUsers;
import echonest.sociogram.connectus.DecryptionUtils;
import echonest.sociogram.connectus.Models.ModelChat;
import echonest.sociogram.connectus.Models.ModelChatlist;
import echonest.sociogram.connectus.Models.ModelUser;
import com.example.connectus.R;

import echonest.sociogram.connectus.RSAUtils;
import echonest.sociogram.connectus.SignInActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.crypto.SecretKey;


public class ChatsFragment extends Fragment {
    FirebaseAuth firebaseAuth;
    DatabaseReference chatListRef, usersRef, chatsRef;
    FirebaseUser currentUser;

    RecyclerView recyclerView;
    List<ModelUser> userList;
    List<ModelUser> originalUserList; // Master list to retain original data
    List<ModelChatlist> chatlistList;
    AdapterChatlist adapterChatlist;
    ValueEventListener chatListListener, usersListener, chatsListener;

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);

        // Initialize Firebase and views
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        recyclerView = view.findViewById(R.id.usersRecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        chatlistList = new ArrayList<>();
        userList = new ArrayList<>();
        originalUserList = new ArrayList<>(); // Initialize master list
        adapterChatlist = new AdapterChatlist(getContext(), userList);
        recyclerView.setAdapter(adapterChatlist);

        // Check if the current user is logged in
        if (currentUser != null) {
//

            // Load chat list
            loadChatList();
        } else {
            redirectToSignIn();
        }

        return view;
    }
    private void loadChatList() {
        String currentUserId = currentUser.getUid();
        chatListRef = FirebaseDatabase.getInstance().getReference("Chatlist").child(currentUserId);

        chatListListener = chatListRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatlistList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelChatlist chatlist = ds.getValue(ModelChatlist.class);
                    if (chatlist != null) {
                        chatlistList.add(chatlist);
                    }
                }
                loadUsers();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
             //   Toast.makeText(getContext(), "Failed to load chat list.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadUsers() {
        usersRef = FirebaseDatabase.getInstance().getReference("Users");

        usersListener = usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                originalUserList.clear(); // Clear master list before repopulating
                userList.clear(); // Clear current displayed list
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelUser user = ds.getValue(ModelUser.class);
                    if (user != null) {
                        for (ModelChatlist chatlist : chatlistList) {
                            if (user.getUserId() != null && user.getUserId().equals(chatlist.getId())) {
                                originalUserList.add(user); // Add to master list
                                userList.add(user); // Add to displayed list
                                break;
                            }
                        }
                    }
                }
                adapterChatlist.notifyDataSetChanged();
                loadLastMessages();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
              //  Toast.makeText(getContext(), "Failed to load users.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadLastMessages() {
        chatsRef = FirebaseDatabase.getInstance().getReference("Chats");

        chatsListener = chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                HashMap<String, String> lastMessageMap = new HashMap<>();
                HashMap<String, Long> lastMessageTimestampMap = new HashMap<>();

                // Retrieve the user's private key from SharedPreferences
                SharedPreferences prefs = requireContext().getSharedPreferences("secure_prefs", Context.MODE_PRIVATE);
                String privateKeyStr = prefs.getString("privateKey", null);
                PrivateKey privateKey = null;

                if (privateKeyStr != null) {
                    try {
                        privateKey = RSAUtils.stringToPrivateKey(privateKeyStr);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat != null && currentUser != null) {
                        String sender = chat.getSender();
                        String receiver = chat.getReceiver();

                        if (sender != null && receiver != null) {
                            if (sender.equals(currentUser.getUid()) || receiver.equals(currentUser.getUid())) {
                                String chatPartnerId = sender.equals(currentUser.getUid()) ? receiver : sender;
                                String lastMessage = "[Encrypted Message]";

                                if (chat.getType() != null) {
                                    switch (chat.getType()) {
                                        case "image":
                                            lastMessage = "A photo";
                                            break;
                                        case "video":
                                            lastMessage = "A video";
                                            break;
                                        default:
                                            // Decrypt the message if possible
                                            if (privateKey != null && chat.getAesKey() != null && chat.getMessage() != null) {
                                                try {
                                                    SecretKey aesKey = DecryptionUtils.decryptAESKeyWithRSA(chat.getAesKey(), privateKey);
                                                    lastMessage = DecryptionUtils.decryptAES(chat.getMessage(), aesKey);
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    lastMessage = "[Encrypted message]";
                                                }
                                            }
                                            break;
                                    }
                                }

                                long timestamp = Long.parseLong(chat.getTimestamp());
                                lastMessageMap.put(chatPartnerId, lastMessage);
                                lastMessageTimestampMap.put(chatPartnerId, timestamp);
                            }
                        }
                    }
                }

                adapterChatlist.setLastMessageMap(lastMessageMap);
                adapterChatlist.setLastMessageTimestampMap(lastMessageTimestampMap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Toast.makeText(getContext(), "Failed to load last messages.", Toast.LENGTH_SHORT).show();
            }
        });
    }




    private void redirectToSignIn() {
        Intent intent = new Intent(getActivity(), SignInActivity.class);
        startActivity(intent);
        getActivity().finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint("Search by name");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (!TextUtils.isEmpty(s.trim())) {
                    searchUsers(s);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (!TextUtils.isEmpty(s.trim())) {
                    searchUsers(s);
                } else {
                    restoreOriginalList(); // Restore the original list when query is cleared
                }
                return false;
            }
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void searchUsers(String query) {
        userList.clear(); // Clear displayed list
        for (ModelUser user : originalUserList) {
            if (user.getName().toLowerCase().contains(query.toLowerCase())) {
                userList.add(user); // Add matching users to displayed list
            }
        }
        adapterChatlist.notifyDataSetChanged();
    }

    private void restoreOriginalList() {
        userList.clear();
        userList.addAll(originalUserList); // Reset displayed list to the original list
        adapterChatlist.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Ensure all listeners are removed
        if (chatListRef != null && chatListListener != null) {
            chatListRef.removeEventListener(chatListListener);
        }
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
        if (chatsRef != null && chatsListener != null) {
            chatsRef.removeEventListener(chatsListener);
        }
    }


}
