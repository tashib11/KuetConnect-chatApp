package echonest.sociogram.connectus;

import static com.google.firebase.database.FirebaseDatabase.getInstance;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;
import com.example.connectus.R;
import com.example.connectus.databinding.ActivitySettingsBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
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

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private DatabaseReference databaseReference;
    private StorageReference storageReference;
    private ProgressDialog progressDialog;
    private Uri imageUri;
    private String profileOrCover;
    private ValueEventListener valueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        applyTheme(); // Apply dark mode based on preferences
        super.onCreate(savedInstanceState);
        setStatusBarColor(); // Set custom status bar color

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebase();
        setupUI();
        loadUserData();
        setupListeners();
    }

//    private void applyTheme() {
//        SharedPreferences preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
//        boolean isDarkMode = preferences.getBoolean("DarkMode", true);
//        setTheme(isDarkMode ? R.style.DarkTheme : R.style.LightTheme);
//    }

    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = this.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        }
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");
        storageReference = FirebaseStorage.getInstance().getReference("Users_Profile_Cover_Imgs");
        progressDialog = new ProgressDialog(this);
    }

    private void setupUI() {
        binding.mainLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.blacklight));


//        binding.mainLayout.setBackgroundColor(ContextCompat.getColor(this,
//                isDarkMode ? R.color.blacklight : R.color.white));
//        binding.darkModeSwitch.setChecked(isDarkMode);
//        binding.darkModeStatus.setText(isDarkMode ? "Enabled" : "Disabled");

        if (currentUser != null) {
            binding.emailTv.setText(currentUser.getEmail());
        }
    }

    private void loadUserData() {
        if (currentUser == null) return;

        Query query = databaseReference.orderByChild("email").equalTo(currentUser.getEmail());
        valueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    binding.nameTv.setText(ds.child("name").getValue(String.class));
                    binding.professionTv.setText(ds.child("profession").getValue(String.class));
                    loadProfileImage(ds.child("profilePhoto").getValue(String.class));
                    loadCoverImage(ds.child("coverPhoto").getValue(String.class));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SettingsActivity.this, "Failed to load data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };
        query.addValueEventListener(valueEventListener);
    }

    private void loadProfileImage(String url) {
        Glide.with(this)
                .load(url)
                .placeholder(R.drawable.avatar)
                .into(binding.avatarIv);
    }

    private void loadCoverImage(String url) {
        Glide.with(this)
                .load(url)
                .into(binding.coverIv);
    }

    private void setupListeners() {
//        binding.darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            SharedPreferences.Editor editor = getSharedPreferences("AppSettings", MODE_PRIVATE).edit();
//            editor.putBoolean("DarkMode", isChecked).apply();
//            recreate(); // Restart activity to apply theme
//        });

        binding.fab.setOnClickListener(view -> showEditProfileDialog());

//        binding.aboutTxt.setOnClickListener(view ->
//                startActivity(new Intent(SettingsActivity.this, aboutActivity.class)));
    }

    private void showEditProfileDialog() {
        String[] options = {"Edit Profile Picture", "Edit Cover Photo", "Edit Name", "Change Password", "Change Profession"};
        new AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            profileOrCover = "profilePhoto";
                            pickImageFromGallery("Updating Profile Picture");
                            break;
                        case 1:
                            profileOrCover = "coverPhoto";
                            pickImageFromGallery("Updating Cover Picture");
                            break;
                        case 2:
                            showNameUpdateDialog();
                            break;
                        case 3:
                            showChangePasswordDialog();
                            break;
                        case 4:
                            showProfessionUpdateDialog();
                            break;
                    }
                }).create().show();
    }



    private void pickImageFromGallery(String message) {
        progressDialog.setMessage(message);
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 300);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == 300 && data != null) {
            imageUri = data.getData();
            uploadImage();
        }
    }

    private void uploadImage() {
        if (imageUri == null || profileOrCover == null) return;

        progressDialog.setMessage("Uploading...");
        progressDialog.show();

        String filePath = profileOrCover + "_" + currentUser.getUid();
        StorageReference ref = storageReference.child(filePath);

        // Add a listener to track upload progress
        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        // Update database with the new image URL
                        HashMap<String, Object> updates = new HashMap<>();
                        updates.put(profileOrCover, uri.toString());
                        databaseReference.child(currentUser.getUid()).updateChildren(updates)
                                .addOnSuccessListener(unused -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(this, "Image updated successfully", Toast.LENGTH_SHORT).show();

                                    // Update the ImageView with the new image
                                    Glide.with(SettingsActivity.this)
                                            .load(uri.toString())
                                            .signature(new ObjectKey(System.currentTimeMillis())) // Force Glide to fetch the new image
                                            .placeholder(R.drawable.avatar) // Loading placeholder
                                            .into(profileOrCover.equals("profilePhoto") ? binding.avatarIv : binding.coverIv);
                                })
                                .addOnFailureListener(e -> showError("Failed to update database: " + e.getMessage()));
                    });
                })
                .addOnFailureListener(e -> showError("Failed to upload image: " + e.getMessage()))
                .addOnProgressListener(snapshot -> {
                    // Show upload progress
                    int progress = (int) (100 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                    progressDialog.setMessage("Uploading: " + progress + "%");
                });
    }

    private void showProfessionUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Profession");

        // Inflate custom layout for input
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(20, 20, 20, 20);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText professionInput = new EditText(this);
        professionInput.setHint("Enter new profession");
        layout.addView(professionInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newProfession = professionInput.getText().toString().trim();
            if (TextUtils.isEmpty(newProfession)) {
                Toast.makeText(SettingsActivity.this, "Profession cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            updateProfession(newProfession);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }
    private void showNameUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Update Name");

        // Inflate custom layout for input
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(20, 20, 20, 20);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Enter new name");
        layout.addView(nameInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newName = nameInput.getText().toString().trim();
            if (TextUtils.isEmpty(newName)) {
                Toast.makeText(SettingsActivity.this, "Name cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            updateName(newName);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void updateName(String newName) {
        progressDialog.setMessage("Updating Name");
        progressDialog.show();

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("name", newName);

        databaseReference.child(currentUser.getUid()).updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(SettingsActivity.this, "Name updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(SettingsActivity.this, "Failed to update name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfession(String newProfession) {
        progressDialog.setMessage("Updating Profession");
        progressDialog.show();

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("profession", newProfession);

        databaseReference.child(currentUser.getUid()).updateChildren(updates)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(SettingsActivity.this, "Profession updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(SettingsActivity.this, "Failed to update profession: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        // Inflate custom layout for input
        LinearLayout layout = new LinearLayout(this);
        layout.setPadding(20, 20, 20, 20);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText currentPasswordInput = new EditText(this);
        currentPasswordInput.setHint("Current Password");
        currentPasswordInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(currentPasswordInput);

        EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint("New Password");
        newPasswordInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(newPasswordInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();

            if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword)) {
                Toast.makeText(SettingsActivity.this, "Please fill out both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(currentPassword, newPassword);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        progressDialog.setMessage("Changing Password");
        progressDialog.show();

        // Reauthenticate user with current password
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);
        currentUser.reauthenticate(credential)
                .addOnSuccessListener(unused -> {
                    currentUser.updatePassword(newPassword)
                            .addOnSuccessListener(unused1 -> {
                                progressDialog.dismiss();
                                Toast.makeText(SettingsActivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(SettingsActivity.this, "Failed to update password: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(SettingsActivity.this, "Wrong Old password ", Toast.LENGTH_LONG).show();
                });
    }

    private void showError(String message) {
        progressDialog.dismiss();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (valueEventListener != null) {
            databaseReference.removeEventListener(valueEventListener);
        }
    }
}
