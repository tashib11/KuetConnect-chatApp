package echonest.sociogram.connectus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.connectus.R;
import com.example.connectus.databinding.ActivityFinallyDeleteAccBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class finallyDeleteAcc extends AppCompatActivity {
ActivityFinallyDeleteAccBinding binding;
FirebaseAuth auth;
ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_finally_delete_acc);
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        ((Window) window).setStatusBarColor(this.getResources().getColor(R.color.black));

        binding= ActivityFinallyDeleteAccBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        auth = FirebaseAuth.getInstance();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        progressDialog=new ProgressDialog(finallyDeleteAcc.this);
        progressDialog.setMessage("Deleting Account");// box ar heading
        progressDialog.setTitle("Delete your account");

        binding.dltFinalAccbutton.setOnClickListener(view -> {
            progressDialog.show();
            String enteredPassword = binding.etPassword.getText().toString();

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String email = user.getEmail();
            AuthCredential credential = EmailAuthProvider.getCredential(email, enteredPassword);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.delete().addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            progressDialog.dismiss();
                            Toast.makeText(finallyDeleteAcc.this, "Account deleted successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(finallyDeleteAcc.this, SignInActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(finallyDeleteAcc.this, "Try again to delete", Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(finallyDeleteAcc.this, "Wrong  Password", Toast.LENGTH_LONG).show();
                    binding.etPassword.setText("");
                }
            });
        });



        binding.recoverPassTv.setOnClickListener(view -> {
            showRecoverPasswordDialog();
        });

    }
    private void showRecoverPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder((this));
        builder.setTitle("Recover Password");
        LinearLayout linearLayout=new LinearLayout(this);
        final EditText emailEt = new EditText(this);
        emailEt.setHint("Email");
        emailEt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        emailEt.setMinEms(16);

        linearLayout.addView(emailEt);
        linearLayout.setPadding(10,10,10,10);
        builder.setView(linearLayout);
        //buttons recover
        builder.setPositiveButton("Recover", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //input email
                String  email= emailEt.getText().toString().trim();
                beginRecovery(email);
            }
        });
        //buttons cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                dialogInterface.dismiss();
            }
        });
        //show dialog
        builder.create().show();
    }
    private void beginRecovery(String email) {
//        progressDialog.setTitle("Login Account");// box ar heading
        progressDialog.setMessage("Sending email...");
        progressDialog.show();

        auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressDialog.dismiss();
                if(task.isSuccessful()){
                    Toast.makeText(finallyDeleteAcc.this, "Email sent", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(finallyDeleteAcc.this, "Failed to recover...", Toast.LENGTH_SHORT).show();

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                progressDialog.dismiss();
                Toast.makeText(finallyDeleteAcc.this,"+e.getMessage",Toast.LENGTH_SHORT).show();
            }
        });
    }


}