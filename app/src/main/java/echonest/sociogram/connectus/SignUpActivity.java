package echonest.sociogram.connectus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.connectus.R;
import com.example.connectus.databinding.ActivitySignUpBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import echonest.sociogram.connectus.Models.ModelUser;


public class SignUpActivity extends AppCompatActivity {

    ActivitySignUpBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;
    ProgressDialog progressDialog;

    private static final String TAG = "SignUpActivity";
    // Allowed email domain (KUET student)
    private static final String ALLOWED_DOMAIN = "@stud.kuet.ac.bd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        setupProgressDialog();

        binding.signupBtn.setOnClickListener(v -> validateAndRegisterUser());

        // keep your existing navigation behaviour (you used SignInActivity in your current code)
        binding.gotoLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, SignInActivity.class));
            finish();
        });
    }

    private void setupProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Creating Account");
        progressDialog.setMessage("Please wait — we're creating your account and sending a verification link.");
        progressDialog.setCancelable(false);
    }

    private void validateAndRegisterUser() {
        String name = binding.nameET.getText() != null ? binding.nameET.getText().toString().trim() : "";
        String profession = binding.professionET.getText() != null ? binding.professionET.getText().toString().trim() : "";
        String email = binding.emailET.getText() != null ? binding.emailET.getText().toString().trim() : "";
        String password = binding.passwordET.getText() != null ? binding.passwordET.getText().toString().trim() : "";

        // validation (preserve your style & add domain restriction)
        if (!isValid(name, profession, email, password)) return;

        // show progress
        progressDialog.show();

        // create firebase account
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = auth.getCurrentUser();
                if (firebaseUser == null) {
                    progressDialog.dismiss();
                    Toast.makeText(SignUpActivity.this, "Unable to get user after registration.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Generate RSA Key Pair (keep your existing flow)
                KeyPair keyPair;
                try {
                    keyPair = RSAUtils.generateRSAKeyPair();
                } catch (NoSuchAlgorithmException e) {
                    progressDialog.dismiss();
                    Log.e(TAG, "RSA generation failed", e);
                    Toast.makeText(SignUpActivity.this, "Key generation failed.", Toast.LENGTH_SHORT).show();
                    // Optionally delete user here to avoid orphan accounts
                    firebaseUser.delete();
                    return;
                }

                final String publicKeyStr = RSAUtils.keyToString(keyPair.getPublic());
                final String privateKeyStr = RSAUtils.keyToString(keyPair.getPrivate());

                // Save private key locally (Encrypted SharedPreferences or normal SharedPreferences as you had)
                SharedPreferences.Editor editor = getSharedPreferences("secure_prefs", MODE_PRIVATE).edit();
                editor.putString("privateKey", privateKeyStr);
                editor.apply();

                // Now send verification email — when successful, save user to DB and sign out
                sendVerificationEmail(firebaseUser, name, profession, email, publicKeyStr);
            } else {
                progressDialog.dismiss();
                handleRegistrationFailure(task.getException());
            }
        });
    }

    private void sendVerificationEmail(FirebaseUser firebaseUser, String name, String profession, String email, String publicKeyStr) {
        firebaseUser.sendEmailVerification().addOnCompleteListener(verificationTask -> {
            if (verificationTask.isSuccessful()) {
                Log.d(TAG, "Verification email sent successfully.");
                // Prepare ModelUser exactly like your style (preserve fields)
                String id = firebaseUser.getUid();
                // Use same model fields you used before (ModelUser)
                ModelUser user = new ModelUser("", "", email, "", id,
                        name, "",
                        profession, 0, publicKeyStr);

                // Save user to database
                database.getReference().child("Users").child(id).setValue(user)
                        .addOnCompleteListener(dbTask -> {
                            progressDialog.dismiss();
                            // sign out so user must verify email before logging in
                            auth.signOut();
                            showVerificationAlert(email);
                        }).addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            // DB save failed — show message and try to cleanup
                            Log.e(TAG, "Failed to save user to DB", e);
                            Toast.makeText(SignUpActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });

            } else {
                progressDialog.dismiss();
                Log.e(TAG, "Failed to send verification email.", verificationTask.getException());
                // Optionally delete freshly created user to avoid unverified, unusable accounts
                try {
                    if (firebaseUser != null) firebaseUser.delete();
                } catch (Exception ignore) {
                }
                Toast.makeText(SignUpActivity.this, "Failed to send verification email. Please try again.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Validation: preserves your model names & style but adds domain restriction
     */
    private boolean isValid(String name, String profession, String email, String password) {
        // clear previous errors
        binding.nameET.setError(null);
        binding.professionET.setError(null);
        binding.emailET.setError(null);
        binding.passwordET.setError(null);

        if (name.isEmpty()) {
            binding.nameET.setError("Name is required.");
            binding.nameET.requestFocus();
            return false;
        }
        if (profession.isEmpty()) {
            binding.professionET.setError("Profession is required.");
            binding.professionET.requestFocus();
            return false;
        }
        if (email.isEmpty()) {
            binding.emailET.setError("Email is required.");
            binding.emailET.requestFocus();
            return false;
        }
        // general format
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailET.setError("Please enter a valid email address.");
            binding.emailET.requestFocus();
            return false;
        }
        // domain restriction (case-insensitive)
        if (!email.toLowerCase().endsWith(ALLOWED_DOMAIN)) {
            binding.emailET.setError("Only KUET student emails (" + ALLOWED_DOMAIN + ") are allowed.");
            binding.emailET.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            binding.passwordET.setError("Password is required.");
            binding.passwordET.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            binding.passwordET.setError("Password must be at least 6 characters long.");
            binding.passwordET.requestFocus();
            return false;
        }
        return true;
    }

    private void handleRegistrationFailure(Exception exception) {
        try {
            throw exception;
        } catch (FirebaseAuthWeakPasswordException e) {
            binding.passwordET.setError("Password is too weak. Please use at least 6 characters.");
            binding.passwordET.requestFocus();
        } catch (FirebaseAuthUserCollisionException e) {
            binding.emailET.setError("This email is already registered.");
            binding.emailET.requestFocus();
        } catch (Exception e) {
            Log.e(TAG, "Registration Failed: ", e);
            Toast.makeText(this, "Registration Failed: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
        }
    }

    private void showVerificationAlert(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Registration Almost Complete")
                .setMessage("A verification link has been sent to " + email + ".\n\nPlease check your email inbox (and spam folder!) to complete your registration.")
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    // go to sign-in screen (user must verify first)
                    Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .setCancelable(false)
                .show();
    }
}