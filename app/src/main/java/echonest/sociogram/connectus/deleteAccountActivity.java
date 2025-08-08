package echonest.sociogram.connectus;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.example.connectus.R;
import com.example.connectus.databinding.ActivityDeleteAccountBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class deleteAccountActivity extends AppCompatActivity {
ActivityDeleteAccountBinding binding;
ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // for  status bar color
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        ((Window) window).setStatusBarColor(this.getResources().getColor(R.color.black));

        setContentView(R.layout.activity_delete_account);
        binding= ActivityDeleteAccountBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



        binding.deleteAccountButton.setOnClickListener(v -> {
            Intent intent = new Intent(deleteAccountActivity.this, finallyDeleteAcc.class);
            startActivity(intent);

        });

    }

}