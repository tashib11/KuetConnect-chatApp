package echonest.sociogram.connectus;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import echonest.sociogram.connectus.Adapters.FragmentsAdapter;
import com.example.connectus.R;
import com.example.connectus.databinding.ActivityMainBinding;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private boolean isAppClosing = false; // Track if the app is being closed
    DrawerLayout drawerLayout;
    NavigationView navigationView;
    BottomNavigationView bottomNavigationView;
    Toolbar toolbar;
    ActivityMainBinding binding;
    FirebaseAuth auth;
    FirebaseUser currentUser;
    DatabaseReference userRef;
    ActionBarDrawerToggle toggle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RSATest.runTest();

        // Set the status bar color
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.black));

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId);

            // Set the user as online when the app is running
            userRef.child("onlineStatus").setValue("online")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Configure onDisconnect to set status to last active time
                            userRef.child("onlineStatus").onDisconnect().setValue(String.valueOf(System.currentTimeMillis()));
                        }
                    });
        }

        // Navigation code remains unchanged
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.nav_drawer);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.OpenDrawer, R.string.CloseDrawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        Drawable drawerArrow = toggle.getDrawerArrowDrawable();
        ((androidx.appcompat.graphics.drawable.DrawerArrowDrawable) drawerArrow).setColor(ContextCompat.getColor(this, R.color.white));

        getSupportActionBar().setTitle("Chats");

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();


            if (id == R.id.logout) {
                if (currentUser != null) {
//                    updateRequestBadge();
                    userRef.child("onlineStatus").setValue(String.valueOf(System.currentTimeMillis()))
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    auth.signOut();
                                    Intent intent = new Intent(MainActivity.this, SignInActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(MainActivity.this, "Failed to update status. Please try again.", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } else if (id == R.id.settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else if (id == R.id.deleteAccount) {
                startActivity(new Intent(MainActivity.this, deleteAccountActivity.class));
            } else if (id == R.id.about) {
                startActivity(new Intent(MainActivity.this, aboutActivity.class));
            }
            return true;
        });

        binding.viewPager.setAdapter(new FragmentsAdapter(getSupportFragmentManager()));
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.bchat) {
                binding.viewPager.setCurrentItem(0);
                getSupportActionBar().setTitle("Chats");
                return true;
            }else if (id == R.id.bpeople) {
                binding.viewPager.setCurrentItem(1);
                getSupportActionBar().setTitle("People");
                return true;
            } else if (id == R.id.brequest) {
                binding.viewPager.setCurrentItem(2);
                getSupportActionBar().setTitle("Message Requests");
                return true;
            }
            return false;
        });
    }

//    private void updateRequestBadge() {
//        if (currentUser == null) return;
//
//        DatabaseReference requestRef = FirebaseDatabase.getInstance()
//                .getReference("ChatRequests")
//                .child(currentUser.getUid());
//
//        requestRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                int count = (int) snapshot.getChildrenCount();
//
//                BadgeDrawable badge = bottomNavigationView.getOrCreateBadge(R.id.brequest);
//                badge.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.red)); // optional styling
//
//                if (count > 0) {
//                    badge.setVisible(true);
//                    badge.setNumber(count);
//                } else {
//                    badge.clearNumber();
//                    badge.setVisible(false);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {}
//        });
//    }


    @Override
    protected void onResume() {
        super.onResume();
        isAppClosing = false; // Reset app-closing flag
        if (currentUser != null && userRef != null) {
            // Keep the user online when the app is running
            userRef.child("onlineStatus").setValue("online");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isAppClosing && currentUser != null && userRef != null) {
            // Set to timestamp when the app is about to go in the background
            userRef.child("onlineStatus").setValue(String.valueOf(System.currentTimeMillis()));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isAppClosing && currentUser != null && userRef != null) {
            // Set to timestamp when the app is destroyed (app is closed)
            userRef.child("onlineStatus").setValue(String.valueOf(System.currentTimeMillis()));
        }
    }


    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            isAppClosing = true; // Mark app as closing
            finishAffinity();
            finish();
        }
    }



}