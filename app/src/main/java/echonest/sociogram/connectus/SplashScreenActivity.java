package echonest.sociogram.connectus;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;

import com.example.connectus.R;
import com.google.firebase.auth.FirebaseAuth;

@SuppressLint("CustomSplashScreen")
public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_splash_screen2);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                // Redirect to SignUpActivity if no user is signed in
                startActivity(new Intent(SplashScreenActivity.this, SignInActivity.class));
            } else {
                // Otherwise, go to MainActivity
                startActivity(new Intent(SplashScreenActivity.this, MainActivity.class));
            }
            finish(); // Close SplashScreenActivity
        }, 2000);

    }
}