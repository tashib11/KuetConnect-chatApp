package echonest.sociogram.connectus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.connectus.R;
import com.example.connectus.databinding.ActivitySignInBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SignInActivity extends AppCompatActivity {
    ActivitySignInBinding binding;
    private FirebaseAuth auth;
    ProgressDialog progressDialog;
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

        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(SignInActivity.this);
        progressDialog.setTitle("Login Account");
        progressDialog.setMessage("Logging in your account");
        database = FirebaseDatabase.getInstance();

        binding.btnSignIN.setOnClickListener(view -> {
            if (binding.etEmail.getText().toString().isEmpty()) {
                binding.etEmail.setError("Enter your email");
                return;
            }
            if (binding.etPassword.getText().toString().isEmpty()) {
                binding.etPassword.setError("Enter your password");
                return;
            }
            progressDialog.show();
            auth.signInWithEmailAndPassword(binding.etEmail.getText().toString(),
                    binding.etPassword.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();
                        // Check if private key exists locally
                        SharedPreferences prefs = getSharedPreferences("secure_prefs", MODE_PRIVATE);
                        String privateKeyStr = prefs.getString("privateKey", null);

                        if (privateKeyStr == null) {
                            Toast.makeText(SignInActivity.this, "Private key not found! You may need to restore backup.", Toast.LENGTH_LONG).show();
                            return;
                            }

                        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignInActivity.this, "Email or password invalid", Toast.LENGTH_SHORT).show();
                    }

            }
            });
        });

        // Recover password
        binding.recoverPassTv.setOnClickListener(view -> {
            showRecoverPasswordDialog();
        });

        // Create a new account

        binding.btnCreateAcc.setOnClickListener(view -> {
            Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
            startActivity(intent);
            finish();
//            Intent intent = new Intent("com.facebookapp.SIGN_UP");
//            if (intent.resolveActivity(getPackageManager()) != null) {
//                startActivity(intent);
//            } else {
//                Toast.makeText(this, "Sociogram app is not installed or cannot handle this action", Toast.LENGTH_SHORT).show();
//            }
        });

    }

    private void showRecoverPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recover Your EchoNest Password");

        // Create a linear layout for the dialog
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        // Set up TextInputLayout to improve EditText appearance
        TextInputLayout textInputLayout = new TextInputLayout(this);
        textInputLayout.setHint("Email Address");

        final EditText emailEt = new EditText(this);
        emailEt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailEt.setMinEms(16);
        emailEt.setPadding(16, 16, 16, 16);

        // Add the EditText inside the TextInputLayout
        textInputLayout.addView(emailEt);

        // Add the TextInputLayout to the LinearLayout
        linearLayout.addView(textInputLayout);

        // Set dialog padding
        linearLayout.setPadding(20, 20, 20, 20);

        // Set the custom layout to the dialog
        builder.setView(linearLayout);

        // Recover button
        builder.setPositiveButton("Send Recovery Email", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                String email = emailEt.getText().toString().trim();
                if (!email.isEmpty()) {
                    beginRecovery(email);
                } else {
                    Toast.makeText(SignInActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Cancel button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
            }
        });

        // Show the dialog
        builder.create().show();
    }

    private void beginRecovery(String email) {
        progressDialog.setMessage("Sending recovery email...");
        progressDialog.show();

        // Firebase password reset logic
        auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    Toast.makeText(SignInActivity.this, "Recovery email sent! Please check your inbox.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SignInActivity.this, "Failed to send email. Please try again.", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(SignInActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
        finish();
    }
}
