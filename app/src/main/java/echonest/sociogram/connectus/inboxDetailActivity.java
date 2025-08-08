package echonest.sociogram.connectus;

import static com.google.firebase.database.FirebaseDatabase.getInstance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.connectus.R;
import com.example.connectus.databinding.ActivityInboxDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class inboxDetailActivity extends AppCompatActivity {
    @Override
    protected void onResume() {
        super.onResume();
        updateBlockButtonUI(); // Update button state whenever activity resumes
    }

    ActivityInboxDetailBinding binding;
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseStorage storage;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;
    StorageReference storageReference;
    String hisUid, myUid;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(this.getResources().getColor(R.color.black));
        }

        binding = ActivityInboxDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        user = firebaseAuth.getCurrentUser();
        databaseReference = firebaseDatabase.getReference("Users");
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference(); // Firebase storage reference

        // Get hisUid from the Intent
        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        if (hisUid != null) {
            // Fetch user data
            fetchUserData(hisUid);
        } else {
            Toast.makeText(this, "No user ID passed.", Toast.LENGTH_SHORT).show();
        }
        database = FirebaseDatabase.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Set up delete button click listener
        binding.deleteButton.setOnClickListener(v -> deleteConversation());
        binding.blockButton.setOnClickListener(v -> toggleBlockUser());


    }

    private void toggleBlockUser() {
        DatabaseReference blockRef = database.getReference("BlockedUsers");

        blockRef.child(myUid).child(hisUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Check if the current user initiated the block
                    String blockInitiator = snapshot.getValue(String.class);

                    if (blockInitiator != null && blockInitiator.equals("blocked_by_" + myUid)) {
                        // Allow unblocking
                        blockRef.child(myUid).child(hisUid).removeValue();
                        blockRef.child(hisUid).child(myUid).removeValue();

                        // Update UI
                        binding.blockButton.setText("Block");
                        binding.blockButton.setBackgroundTintList(ContextCompat.getColorStateList(inboxDetailActivity.this, R.color.black));

                        Toast.makeText(inboxDetailActivity.this, "User unblocked", Toast.LENGTH_SHORT).show();
                    } else {
                        // Other user cannot unblock
                        Toast.makeText(inboxDetailActivity.this, "You cannot unblock this user", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // No block exists, proceed to block
                    blockRef.child(myUid).child(hisUid).setValue("blocked_by_" + myUid);
                    blockRef.child(hisUid).child(myUid).setValue("blocked_by_" + myUid);

                    // Update UI
                    binding.blockButton.setText("Unblock");
                    binding.blockButton.setBackgroundTintList(ContextCompat.getColorStateList(inboxDetailActivity.this, R.color.red));

                    Toast.makeText(inboxDetailActivity.this, "User blocked", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(inboxDetailActivity.this, "Failed to toggle block status.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateBlockButtonUI() {
        DatabaseReference blockRef = database.getReference("BlockedUsers");

        blockRef.child(myUid).child(hisUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String blockInitiator = snapshot.getValue(String.class);

                    if (blockInitiator != null && blockInitiator.equals("blocked_by_" + myUid)) {
                        // Current user initiated the block
                        binding.blockButton.setText("Unblock");
                        binding.blockButton.setBackgroundTintList(ContextCompat.getColorStateList(inboxDetailActivity.this, R.color.red));
                    } else {
                        // Current user is blocked by the other user, show "Blocked"
                        binding.blockButton.setText("Blocked");
                        binding.blockButton.setEnabled(false);
                        binding.blockButton.setBackgroundTintList(ContextCompat.getColorStateList(inboxDetailActivity.this, R.color.red));
                    }
                } else {
                    // No block exists
                    binding.blockButton.setText("Block");
                    binding.blockButton.setEnabled(true);
                    binding.blockButton.setBackgroundTintList(ContextCompat.getColorStateList(inboxDetailActivity.this, R.color.black));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(inboxDetailActivity.this, "Failed to load block status.", Toast.LENGTH_SHORT).show();
            }
        });
    }




    private void fetchUserData(String hisUid) {
        DatabaseReference userRef = databaseReference.child(hisUid);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Fetch user data
                    String name = "" + snapshot.child("name").getValue();
                    String email = "" + snapshot.child("email").getValue();
                    String profession = "" + snapshot.child("profession").getValue();
                    String image = "" + snapshot.child("profilePhoto").getValue();
                    String cover = "" + snapshot.child("coverPhoto").getValue();

                    // Bind data to views
                    binding.nameTv.setText(name);
                    binding.emailTv.setText(email);
                    binding.professionTv.setText(profession);

                    // Load profile photo
                    try {
                        Glide.with(inboxDetailActivity.this)
                                .load(image)
                                .placeholder(R.drawable.avatar)
                                .into(binding.avatarIv);
                    } catch (Exception e) {
                        Glide.with(inboxDetailActivity.this)
                                .load(R.drawable.avatar)
                                .into(binding.avatarIv);
                    }

                    // Load cover photo
                    try {
                        Glide.with(inboxDetailActivity.this)
                                .load(cover)
                                .into(binding.coverIv);
                    } catch (Exception e) {
                        Glide.with(inboxDetailActivity.this)
                                .load(R.drawable.avatar)
                                .into(binding.coverIv);
                    }
                } else {
                    Toast.makeText(inboxDetailActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //    Toast.makeText(inboxDetailActivity.this, "Failed to fetch data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteConversation() {
        // Create an AlertDialog for confirmation
        new AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("Are you sure you want to delete own conversation?")
                .setIcon(R.drawable.baseline_warning_24) // Use a warning icon if available
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Perform deletion upon confirmation
                    DatabaseReference chatRef = firebaseDatabase.getReference("Chats");
                    chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()) {
                                String sender = ds.child("sender").getValue(String.class);
                                String receiver = ds.child("receiver").getValue(String.class);

                                // Only remove messages sent by the current user
                                if (sender != null && receiver != null) {
                                    if (sender.equals(myUid) && receiver.equals(hisUid)) {
                                        ds.getRef().removeValue(); // Delete message only from your side
                                    }
                                } else {
                                    Log.w("DeleteConversation", "Skipping message: sender or receiver is null. Key: " + ds.getKey());
                                }
                            }
                            Toast.makeText(inboxDetailActivity.this, "Own conversation deleted successfully.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(inboxDetailActivity.this, "Failed to delete conversation: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Dismiss dialog
                    dialog.dismiss();
                })
                .show();
    }




    @Override
    public void onBackPressed() {
        finish();
    }
}
