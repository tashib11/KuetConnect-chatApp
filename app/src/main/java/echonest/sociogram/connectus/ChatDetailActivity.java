package echonest.sociogram.connectus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import echonest.sociogram.connectus.Adapters.AdapterChat;
import echonest.sociogram.connectus.Models.ModelChat;
import echonest.sociogram.connectus.Models.ModelChatlist;

import com.example.connectus.R;
import com.example.connectus.databinding.ActivityChatDetailBinding;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.SecretKey;


public class ChatDetailActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        checkBlockStatus(); // Recheck the block status when the activity resumes
    }
    private static final String SENT_MESSAGES_PREFS = "SentMessagesPrefs";

    private String hisImage = "";
    private boolean isLoadingMoreMessages = false;
    private String earliestMessageTimestamp = null; // To track the earliest message

    private CustomItemAnimator itemAnimator;
    private ActivityChatDetailBinding binding;
    private FirebaseDatabase database;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference usersDbRef;
    private String hisUid, myUid;
    //    private String hisImage;
    private ValueEventListener messagesListener;

    private AdapterChat adapterChat;
    private final List<ModelChat> chatList = new ArrayList<>();

    private static final int GALLERY_REQUEST_CODE = 400;
    private static final int VIDEO_REQUEST_CODE = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarColor(R.color.black);

        binding = ActivityChatDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeChatRecyclerView();
        initializeTextWatcher();
        preloadDatabaseConnection();

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");
        myUid = firebaseAuth.getCurrentUser().getUid(); // Ensure `myUid` is set

        // Check block status early
        checkBlockStatus();
        loadUserDetails();
        loadMessages();

        binding.sendbtn.setOnClickListener(v -> sendMessage());
        binding.attachBtn.setOnClickListener(v -> pickImageFromGallery());
        binding.attachBtnVideo.setOnClickListener(v -> pickVideoFromGallery());
        binding.backArrow.setOnClickListener(v -> finish());

        // Navigate to InboxDetailActivity
        binding.headbar.setOnClickListener(v -> {
            Intent intent1 = new Intent(ChatDetailActivity.this, inboxDetailActivity.class);
            intent1.putExtra("hisUid", hisUid);
            startActivity(intent1);
        });

    }

    private void checkBlockStatus() {
        DatabaseReference blockRef = database.getReference("BlockedUsers");

        // Check if `hisUid` is blocked by `myUid` (Person A has blocked Person B)
        blockRef.child(myUid).child(hisUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Blocker View
                    binding.chatLayout.setVisibility(View.GONE);

                } else {
                    // Check if `myUid` is blocked by `hisUid` (Person B is blocked by Person A)
                    blockRef.child(hisUid).child(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // Blocked View
                                binding.chatLayout.setVisibility(View.GONE);

                            } else {
                                // Normal Chat View
                                binding.chatLayout.setVisibility(View.VISIBLE);

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(ChatDetailActivity.this, "Failed to check block status.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatDetailActivity.this, "Failed to check block status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setStatusBarColor(int colorId) {
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(getResources().getColor(colorId));
        }
    }

    private void preloadDatabaseConnection() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Dummy");
        dbRef.setValue("init").addOnCompleteListener(task -> dbRef.removeValue());
    }

    private void initializeChatRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);

        // Initialize adapter with default or placeholder image
        adapterChat = new AdapterChat(this, chatList, hisImage);
        binding.chatRecyclerView.setAdapter(adapterChat);
        binding.chatRecyclerView.setHasFixedSize(true);
        binding.chatRecyclerView.setLayoutManager(linearLayoutManager);

        // Add scroll listener for pagination
        binding.chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!binding.chatRecyclerView.canScrollVertically(-1) && !isLoadingMoreMessages) {
                    loadOlderMessages();
                }
            }
        });

        Log.d("ChatDetailActivity", "Chat RecyclerView initialized.");
    }


    private void initializeTextWatcher() {
        binding.messageEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    binding.attachmentLayout.setVisibility(View.GONE);
                    binding.likebtn.setVisibility(View.GONE);
                    binding.sendbtn.setVisibility(View.VISIBLE);
                } else {
                    binding.attachmentLayout.setVisibility(View.VISIBLE);
                    binding.likebtn.setVisibility(View.VISIBLE);
                    binding.sendbtn.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUserDetails() {
        usersDbRef = database.getReference("Users");
        Query userQuery = usersDbRef.orderByChild("userId").equalTo(hisUid);

        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String name = "" + ds.child("name").getValue();
                    hisImage = "" + ds.child("profilePhoto").getValue();
                    String onlineStatus = "" + ds.child("onlineStatus").getValue();

                    binding.nameTv.setText(name);
                    updateUserStatus(onlineStatus);

                    if (!isFinishing() && !isDestroyed()) {
                        Glide.with(ChatDetailActivity.this)
                                .load(hisImage.isEmpty() ? R.drawable.avatar : hisImage)
                                .placeholder(R.drawable.avatar)
                                .into(binding.profileIv);
                    }
                }

                if (adapterChat != null) {
                    adapterChat.updateProfileImage(hisImage);
                    adapterChat.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }


    private void updateUserStatus(String onlineStatus) {
        if (onlineStatus.equals("online")) {
            binding.userStatusTv.setText(onlineStatus);
        } else {
            try {
                Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                cal.setTimeInMillis(Long.parseLong(onlineStatus));
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm aa", Locale.ENGLISH);
                String dateTime = sdf.format(cal.getTime());
                binding.userStatusTv.setText("Last seen: " + dateTime);
            } catch (NumberFormatException e) {
                Log.e("TimeStamp Error", "Invalid timestamp format", e);
            }
        }
    }

    private void sendMessage() {
        DatabaseReference blockRef = database.getReference("BlockedUsers");

        blockRef.child(myUid).child(hisUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(ChatDetailActivity.this, "You cannot send messages to this user.", Toast.LENGTH_SHORT).show();
                } else {
                    proceedToSendMessage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatDetailActivity.this, "Failed to check block status.", Toast.LENGTH_SHORT).show();
            }
        });
    }




private void proceedToSendMessage() {
    String message = binding.messageEt.getText().toString().trim();
    if (!TextUtils.isEmpty(message)) {
        String timestamp = String.valueOf(System.currentTimeMillis());

        // Store plaintext message in SharedPreferences
        SharedPreferences prefs = getSharedPreferences(SENT_MESSAGES_PREFS, MODE_PRIVATE);
        prefs.edit().putString(timestamp, message).apply();

        // Show the message in UI immediately for sender
        ModelChat tempMessage = new ModelChat(
                message, hisUid, myUid, timestamp, "text", false, null, "Sending", null
        );
        chatList.add(tempMessage);
        adapterChat.notifyItemInserted(chatList.size() - 1);
        binding.chatRecyclerView.scrollToPosition(chatList.size() - 1);
        binding.messageEt.setText("");

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(hisUid);
        userRef.child("publicKey").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                String recipientPublicKeyStr = task.getResult().getValue(String.class);
                try {
                    PublicKey recipientPublicKey = RSAUtils.stringToPublicKey(recipientPublicKeyStr);
                    SecretKey aesKey = EncryptionUtils.generateAESKey();

                    String encryptedMessage = EncryptionUtils.encryptAES(message, aesKey);
                    String encryptedAESKey = EncryptionUtils.encryptAESKeyWithRSA(aesKey, recipientPublicKey);

                    DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("Chats");
                    String messageKey = chatRef.push().getKey();

                    HashMap<String, Object> messageMap = new HashMap<>();
                    messageMap.put("sender", myUid);
                    messageMap.put("receiver", hisUid);
                    messageMap.put("message", encryptedMessage);
                    messageMap.put("aesKey", encryptedAESKey);
                    messageMap.put("timestamp", timestamp);
                    messageMap.put("isSeen", false);
                    messageMap.put("type", "text");
                    messageMap.put("messageStatus", "Sending");

                    assert messageKey != null;
                    chatRef.child(messageKey).setValue(messageMap).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            // ✅ Add to sender's chatlist
                            addToChatList(myUid, hisUid);

// ✅ Check if receiver has already accepted (i.e., you're in *their* chatlist)
                            DatabaseReference chatListRef = FirebaseDatabase.getInstance().getReference("Chatlist").child(hisUid);
                            chatListRef.child(myUid).get().addOnCompleteListener(checkTask -> {
                                if (!checkTask.getResult().exists()) {
                                    // Not accepted yet → send a request
                                    DatabaseReference reqRef = FirebaseDatabase.getInstance().getReference("ChatRequests");
                                    reqRef.child(hisUid).child(myUid).setValue("pending");
                                }
                            });


                            // ✅ Update message status to "Sent"
                            chatRef.child(messageKey).child("messageStatus").setValue("Sent");

                            // ✅ Update UI message status
                            for (ModelChat chat : chatList) {
                                if (chat.getTimestamp().equals(timestamp)) {
                                    chat.setMessageStatus("Sent");
                                    break;
                                }
                            }
                            adapterChat.notifyDataSetChanged();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Recipient's public key not found", Toast.LENGTH_SHORT).show();
            }
        });

    } else {
        Toast.makeText(this, "Cannot send an empty message", Toast.LENGTH_SHORT).show();
    }
}





    // Adds the recipient ID to the Chatlist of the sender
    private void addToChatList(String senderId, String receiverId) {
        DatabaseReference chatListRef = FirebaseDatabase.getInstance().getReference("Chatlist").child(senderId);
        chatListRef.child(receiverId).setValue(new ModelChatlist(receiverId));
    }








    private void loadMessages() {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query chatQuery = dbRef.orderByChild("timestamp");

        messagesListener = chatQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    chat.setMessageId(ds.getKey()); // Set the unique ID
                    if (chat != null && chat.getSender() != null && chat.getReceiver() != null) {
                        boolean isChatRelevant = (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) ||
                                (chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid));

                        if (isChatRelevant) {
                            if (chat.getSender().equals(myUid)) {
                                // Plaintext for sender from SharedPreferences
                                SharedPreferences prefs = getSharedPreferences(SENT_MESSAGES_PREFS, MODE_PRIVATE);
                                String plaintext = prefs.getString(chat.getTimestamp(), null);
                                if (plaintext != null) {
                                    chat.setMessage(plaintext);
                                }
                            } else if (chat.getReceiver().equals(myUid)) {
                                dbRef.child(chat.getMessageId()).child("isSeen").setValue(true);
                                dbRef.child(chat.getMessageId()).child("messageStatus").setValue("Seen");

                                if (chat.getType().equals("text")) {
                                    // 🔐 Decrypt only text messages
                                    try {
                                        SharedPreferences prefs = getSharedPreferences("secure_prefs", MODE_PRIVATE);
                                        String privateKeyStr = prefs.getString("privateKey", null);
                                        if (privateKeyStr != null) {
                                            PrivateKey privateKey = RSAUtils.stringToPrivateKey(privateKeyStr);
                                            SecretKey aesKey = DecryptionUtils.decryptAESKeyWithRSA(chat.getAesKey(), privateKey);
                                            String decryptedMessage = DecryptionUtils.decryptAES(chat.getMessage(), aesKey);
                                            chat.setMessage(decryptedMessage);
                                        } else {
                                            chat.setMessage("[Encrypted Message]");
                                        }
                                    } catch (Exception e) {
                                        chat.setMessage("[Decryption Failed]");
                                        e.printStackTrace();
                                    }
                                } else if (chat.getType().equals("image") || chat.getType().equals("video")) {
                                    // 🔓 Decrypt the URL for image/video
                                    try {
                                        SharedPreferences prefs = getSharedPreferences("secure_prefs", MODE_PRIVATE);
                                        String privateKeyStr = prefs.getString("privateKey", null);
                                        if (privateKeyStr != null) {
                                            PrivateKey privateKey = RSAUtils.stringToPrivateKey(privateKeyStr);
                                            SecretKey aesKey = DecryptionUtils.decryptAESKeyWithRSA(chat.getAesKey(), privateKey);
                                            String decryptedUrl = DecryptionUtils.decryptAES(chat.getMessage(), aesKey);
                                            chat.setMessage(decryptedUrl); // ✅ Replace encrypted URL with decrypted one
                                        } else {
                                            chat.setMessage(null); // 🔐 If no key, don’t load placeholder
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        chat.setMessage(null); // Load error placeholder if needed
                                    }
                                }
                            }


                            chatList.add(chat);
                        }
                    }
                }

                if (!chatList.isEmpty()) {
                    earliestMessageTimestamp = chatList.get(0).getTimestamp();
                }

                if (adapterChat != null) {
                    adapterChat.notifyDataSetChanged();
                    binding.chatRecyclerView.scrollToPosition(chatList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatDetailActivity.this, "Failed to load messages.", Toast.LENGTH_SHORT).show();
            }
        });
    }





    private void loadOlderMessages() {
        if (earliestMessageTimestamp == null) return;

        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query olderChatQuery = dbRef.orderByChild("timestamp").endBefore(earliestMessageTimestamp).limitToLast(20);

        olderChatQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ModelChat> olderMessages = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    chat.setMessageId(ds.getKey()); // Set the unique ID
                    if (chat != null && chat.getSender() != null && chat.getReceiver() != null) {
                        boolean isChatRelevant = (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)) ||
                                (chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid));

                        if (isChatRelevant) {
                            if (chat.getSender().equals(myUid)) {
                                // Retrieve plaintext from SharedPreferences
                                SharedPreferences prefs = getSharedPreferences(SENT_MESSAGES_PREFS, MODE_PRIVATE);
                                String plaintext = prefs.getString(chat.getTimestamp(), null);
                                if (plaintext != null) {
                                    chat.setMessage(plaintext);
                                }

                            } else if (chat.getReceiver().equals(myUid)) {
                                if (chat.getType().equals("text")) {
                                    // 🔐 Decrypt text messages
                                    try {
                                        SharedPreferences prefs = getSharedPreferences("secure_prefs", MODE_PRIVATE);
                                        String privateKeyStr = prefs.getString("privateKey", null);
                                        if (privateKeyStr != null) {
                                            PrivateKey privateKey = RSAUtils.stringToPrivateKey(privateKeyStr);
                                            SecretKey aesKey = DecryptionUtils.decryptAESKeyWithRSA(chat.getAesKey(), privateKey);
                                            String decryptedMessage = DecryptionUtils.decryptAES(chat.getMessage(), aesKey);
                                            chat.setMessage(decryptedMessage);
                                        } else {
                                            chat.setMessage("[Encrypted Message]");
                                        }
                                    } catch (Exception e) {
                                        chat.setMessage("[Decryption Failed]");
                                        e.printStackTrace();
                                    }
                                } else if (chat.getType().equals("image") || chat.getType().equals("video")) {
                                    // 🔓 Decrypt the image/video URL
                                    try {
                                        SharedPreferences prefs = getSharedPreferences("secure_prefs", MODE_PRIVATE);
                                        String privateKeyStr = prefs.getString("privateKey", null);
                                        if (privateKeyStr != null) {
                                            PrivateKey privateKey = RSAUtils.stringToPrivateKey(privateKeyStr);
                                            SecretKey aesKey = DecryptionUtils.decryptAESKeyWithRSA(chat.getAesKey(), privateKey);
                                            String decryptedUrl = DecryptionUtils.decryptAES(chat.getMessage(), aesKey);
                                            chat.setMessage(decryptedUrl);
                                        } else {
                                            chat.setMessage(null);
                                        }
                                    } catch (Exception e) {
                                        chat.setMessage(null);
                                        e.printStackTrace();
                                    }
                                }
                            }

                            olderMessages.add(chat);
                        }
                    }
                }
                chatList.addAll(0, olderMessages);
                if (!olderMessages.isEmpty()) {
                    earliestMessageTimestamp = olderMessages.get(0).getTimestamp();
                }
                adapterChat.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatDetailActivity.this, "Failed to load older messages.", Toast.LENGTH_SHORT).show();
            }
        });
    }





    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
            dbRef.removeEventListener(messagesListener);
        }
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_REQUEST_CODE);
    }

    private void pickVideoFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("video/*");
        startActivityForResult(intent, VIDEO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == GALLERY_REQUEST_CODE) {
                Uri imageUri = data.getData();
                sendImageMessage(imageUri);
            } else if (requestCode == VIDEO_REQUEST_CODE) {
                Uri videoUri = data.getData();
                sendVideoMessage(videoUri);
            }
        }
    }


    private void sendImageMessage(Uri imageUri) {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String fileNameAndPath = "ChatImages/" + timeStamp;

        // Declare tempChat inside the method
        ModelChat tempChat = new ModelChat("loading", hisUid, myUid, timeStamp, "image", false, null,"Sending","");
        tempChat.setLocalImageUri(imageUri.toString());
        tempChat.setUploading(true);
        tempChat.setUploadProgress(0);

        // Add tempChat to the chatList and update the adapter
        chatList.add(tempChat);
        runOnUiThread(() -> {
            adapterChat.notifyItemInserted(chatList.size() - 1);
            binding.chatRecyclerView.scrollToPosition(chatList.size() - 1);
        });

        new Thread(() -> {
            try {
                Bitmap bitmap = decodeSampledBitmapFromUri(imageUri, 800, 800);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
                byte[] data = baos.toByteArray();

                // Firebase Storage reference
                StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
                ref.putBytes(data)
                        .addOnProgressListener(taskSnapshot -> {
                            // Update progress
                            int progress = (int) (100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                            tempChat.setUploadProgress(progress);
                            runOnUiThread(() -> adapterChat.notifyItemChanged(chatList.indexOf(tempChat)));
                        })
                        .addOnSuccessListener(taskSnapshot -> {
                            // Get download URL and save to database
                            taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(uri -> {
                                tempChat.setMessage(uri.toString());
                                tempChat.setUploading(false);
                                tempChat.setUploadProgress(100);

                                runOnUiThread(() -> adapterChat.notifyItemChanged(chatList.indexOf(tempChat)));
                                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(hisUid);
                                userRef.child("publicKey").get().addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && task.getResult().exists()) {
                                        String recipientPublicKeyStr = task.getResult().getValue(String.class);
                                        try {
                                            PublicKey recipientPublicKey = RSAUtils.stringToPublicKey(recipientPublicKeyStr);
                                            SecretKey aesKey = EncryptionUtils.generateAESKey();

                                            String encryptedUrl = EncryptionUtils.encryptAES(uri.toString(), aesKey);
                                            String encryptedAESKey = EncryptionUtils.encryptAESKeyWithRSA(aesKey, recipientPublicKey);

                                            SharedPreferences prefs = getSharedPreferences(SENT_MESSAGES_PREFS, MODE_PRIVATE);
                                            prefs.edit().putString(timeStamp, uri.toString()).apply(); // Save actual URL locally

                                            saveMessageToDatabase(encryptedUrl, timeStamp, encryptedAESKey);


                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(this, "Recipient's public key not found", Toast.LENGTH_SHORT).show();
                                    }
                                });

                            });
                        })
                        .addOnFailureListener(e -> {
                            // Remove tempChat on failure
                            chatList.remove(tempChat);
                            runOnUiThread(adapterChat::notifyDataSetChanged);
                        });

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private Bitmap decodeSampledBitmapFromUri(Uri imageUri, int reqWidth, int reqHeight) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        InputStream input = getContentResolver().openInputStream(imageUri);
        BitmapFactory.decodeStream(input, null, options);
        input.close();

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        input = getContentResolver().openInputStream(imageUri);
        Bitmap sampledBitmap = BitmapFactory.decodeStream(input, null, options);
        input.close();

        return sampledBitmap;
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void saveMessageToDatabase(String encryptedUrl, String timeStamp, String encryptedAESKey) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", encryptedUrl);
        hashMap.put("aesKey", encryptedAESKey);
        hashMap.put("timestamp", timeStamp);
        hashMap.put("type", "image");
        hashMap.put("isSeen", false);
        databaseReference.child("Chats").push().setValue(hashMap);
    }




private void sendVideoMessage(Uri videoUri) {
    String timeStamp = String.valueOf(System.currentTimeMillis());
    String filePath = "ChatVideos/" + timeStamp + ".mp4";

    ModelChat tempChat = new ModelChat("loading", hisUid, myUid, timeStamp, "video", false, null,"Sending","");
    tempChat.setLocalImageUri(videoUri.toString());
    tempChat.setUploading(true);
    tempChat.setUploadProgress(0);

    chatList.add(tempChat);
    adapterChat.notifyItemInserted(chatList.size() - 1);
    binding.chatRecyclerView.scrollToPosition(chatList.size() - 1);

    StorageReference videoRef = FirebaseStorage.getInstance().getReference().child(filePath);
    UploadTask uploadTask = videoRef.putFile(videoUri);

    uploadTask.addOnProgressListener(taskSnapshot -> {
        int progress = (int) (100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
        tempChat.setUploadProgress(progress);
        adapterChat.notifyItemChanged(chatList.indexOf(tempChat));
    }).addOnSuccessListener(taskSnapshot -> {
        videoRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
            tempChat.setMessage(downloadUri.toString());
            tempChat.setUploading(false);
            tempChat.setUploadProgress(100);
            adapterChat.notifyItemChanged(chatList.indexOf(tempChat));

            // 🛡 Encrypt and save video message
            encryptAndSendVideo(downloadUri.toString(), timeStamp);
        });
    }).addOnFailureListener(e -> {
        chatList.remove(tempChat);
        adapterChat.notifyDataSetChanged();
        Toast.makeText(this, "Video upload failed.", Toast.LENGTH_SHORT).show();
    });
}
    private void encryptAndSendVideo(String videoUrl, String timeStamp) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Chats");

        FirebaseDatabase.getInstance().getReference("Users").child(hisUid).child("publicKey")
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String recipientPublicKeyStr = task.getResult().getValue(String.class);
                        try {
                            PublicKey recipientPublicKey = RSAUtils.stringToPublicKey(recipientPublicKeyStr);
                            SecretKey aesKey = EncryptionUtils.generateAESKey();

                            String encryptedUrl = EncryptionUtils.encryptAES(videoUrl, aesKey);
                            String encryptedAESKey = EncryptionUtils.encryptAESKeyWithRSA(aesKey, recipientPublicKey);

                            // Save original video URL locally for sender's view
                            SharedPreferences prefs = getSharedPreferences(SENT_MESSAGES_PREFS, MODE_PRIVATE);
                            prefs.edit().putString(timeStamp, videoUrl).apply();

                            // 🔄 Save encrypted video message
                            HashMap<String, Object> messageMap = new HashMap<>();
                            messageMap.put("sender", myUid);
                            messageMap.put("receiver", hisUid);
                            messageMap.put("message", encryptedUrl);
                            messageMap.put("aesKey", encryptedAESKey);
                            messageMap.put("timestamp", timeStamp);
                            messageMap.put("type", "video");
                            messageMap.put("isSeen", false);

                            databaseReference.push().setValue(messageMap);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Encryption failed.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Recipient's public key not found.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    protected void onStart() {
        super.onStart();
        myUid = firebaseAuth.getCurrentUser().getUid();
    }


}
