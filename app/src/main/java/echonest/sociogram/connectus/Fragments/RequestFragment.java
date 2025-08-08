package echonest.sociogram.connectus.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import echonest.sociogram.connectus.Adapters.AdapterRequestList;
import echonest.sociogram.connectus.Models.ModelUser;

//RequestFragment.java
public class RequestFragment extends Fragment {
    private RecyclerView recyclerView;
    private AdapterRequestList adapter;
    private List<ModelUser> requestList;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_request, container, false);

        recyclerView = view.findViewById(R.id.requestRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestList = new ArrayList<>();
        adapter = new AdapterRequestList(getContext(), requestList);
        recyclerView.setAdapter(adapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        loadRequests();

        return view;
    }

    private void loadRequests() {
        DatabaseReference reqRef = FirebaseDatabase.getInstance().getReference("ChatRequests").child(currentUser.getUid());
        reqRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> senderUids = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    senderUids.add(ds.getKey());
                }

                // Now batch load all users
                fetchUsers(senderUids);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchUsers(List<String> uids) {
        requestList.clear(); // clear once

        if (uids.isEmpty()) {
            adapter.notifyDataSetChanged();
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users");
        for (String uid : uids) {
            userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    ModelUser user = snapshot.getValue(ModelUser.class);
                    if (user != null) {
                        // Only add if not already added (safety check)
                        boolean exists = false;
                        for (ModelUser existing : requestList) {
                            if (existing.getUserId().equals(user.getUserId())) {
                                exists = true;
                                break;
                            }
                        }
                        if (!exists) {
                            requestList.add(user);
                            adapter.notifyDataSetChanged(); // refresh entire list once safely
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }


    private void loadUserInfo(String uid) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users");
        userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ModelUser user = snapshot.getValue(ModelUser.class);
                if (user != null) {
                    boolean alreadyExists = false;

                    for (ModelUser existing : requestList) {
                        if (existing.getUserId().equals(user.getUserId())) {
                            alreadyExists = true;
                            break;
                        }
                    }

                    if (!alreadyExists) {
                        requestList.add(user);
                        adapter.notifyItemInserted(requestList.size() - 1);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

}
