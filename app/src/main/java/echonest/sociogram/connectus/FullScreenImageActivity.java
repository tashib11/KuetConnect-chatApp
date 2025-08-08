package echonest.sociogram.connectus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;

import com.bumptech.glide.request.target.Target;

import android.Manifest;
import com.example.connectus.R;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.connectus.R;
import com.google.android.material.button.MaterialButton;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class FullScreenImageActivity extends AppCompatActivity {

    private long downloadId;
    private BroadcastReceiver downloadReceiver;
    private static final int STORAGE_PERMISSION_CODE = 101; // Add this constant

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        // Window configuration
        Window window = getWindow();
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        window.setNavigationBarColor(ContextCompat.getColor(this, R.color.black));

        initViews();
        handleImageLoading();
    }

    private void initViews() {
        ImageView fullScreenImageView = findViewById(R.id.fullScreenImageView);
        MaterialButton downloadButton = findViewById(R.id.downloadButton);
        ImageView backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> supportFinishAfterTransition());

        downloadButton.setOnClickListener(v -> {
            String imageUrl = getIntent().getStringExtra("image_url");
            if (TextUtils.isEmpty(imageUrl)) {
                showToast("Invalid image URL");
                return;
            }
            handleDownload(imageUrl);
        });
    }

    private void handleImageLoading() {
        String imageUrl = getIntent().getStringExtra("image_url");
        ImageView imageView = findViewById(R.id.fullScreenImageView);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.baseline_image_24)
                .error(R.drawable.baseline_image_24)
                .transform(new RoundedCornersTransformation(16, 0)) // 16dp radius
                .fitCenter()
                .override(Target.SIZE_ORIGINAL)
                .into(imageView);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) { // Use constant here
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                handleDownload(getIntent().getStringExtra("image_url"));
            } else {
                showToast("Permission denied. Cannot download image.");
            }
        }
    }

    private void handleDownload(String imageUrl) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startDownload(imageUrl);
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                startDownload(imageUrl);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            }
        }
    }

    private void startDownload(String imageUrl) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(imageUrl))
                    .setTitle("Image Download")
                    .setDescription("Downloading image...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_PICTURES,
                            "EchoNest/" + System.currentTimeMillis() + ".jpg"
                    );

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                downloadId = dm.enqueue(request);
                showToast("Download started");
            }
        } catch (Exception e) {
            showToast("Download failed: " + e.getLocalizedMessage());
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    showToast("Image saved to Pictures/EchoNest");
                }
            }
        };
        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (downloadReceiver != null) {
            unregisterReceiver(downloadReceiver);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}