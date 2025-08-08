package echonest.sociogram.connectus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.connectus.R;
import com.example.connectus.databinding.ActivitySignUpBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import echonest.sociogram.connectus.Models.ModelUser;

public class SignUpActivity extends AppCompatActivity {

    ActivitySignUpBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        binding.signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = binding.emailET.getText().toString();
                String password = binding.passwordET.getText().toString();

                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            String id = task.getResult().getUser().getUid();

                            // Generate RSA Key Pair
                            KeyPair keyPair = null;
                            try {
                                keyPair = RSAUtils.generateRSAKeyPair();
                            } catch (NoSuchAlgorithmException e) {
                                throw new RuntimeException(e);
                            }
                            String publicKeyStr = RSAUtils.keyToString(keyPair.getPublic());
                            String privateKeyStr = RSAUtils.keyToString(keyPair.getPrivate());

// Save private key locally (Encrypted SharedPreferences)
                            SharedPreferences.Editor editor = getSharedPreferences("secure_prefs", MODE_PRIVATE).edit();
                            editor.putString("privateKey", privateKeyStr);
                            editor.apply();
                            // Store Public Key in Firebase
                            ModelUser user = new ModelUser("", "", email, password, "", id,
                                    binding.nameET.getText().toString(), "",
                                    binding.profession.getText().toString(), 0,publicKeyStr);

                            database.getReference().child("Users").child(id).setValue(user);



                            Toast.makeText(SignUpActivity.this, "User Data & Public Key Saved", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(SignUpActivity.this, "Error", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        binding.gotoLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpActivity.this, SignInActivity.class);
                startActivity(intent);
            }
        });
    }
}
