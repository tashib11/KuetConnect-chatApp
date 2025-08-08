package echonest.sociogram.connectus;


import android.app.DownloadManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import com.example.connectus.R;

public class FullScreenVideoActivity extends AppCompatActivity {
    private PlayerView playerView;
    private ExoPlayer exoPlayer;
    private String videoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_video);

        // Transparent status bar
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        videoUrl = getIntent().getStringExtra("videoUrl");

        // Initialize UI components
        initializePlayer();
        setupControls();
    }

    private void initializePlayer() {
        playerView = findViewById(R.id.exoPlayerView);
        exoPlayer = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(exoPlayer);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl));
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void setupControls() {
        // Close button
        findViewById(R.id.closeButton).setOnClickListener(v -> finish());

        // Download button
        findViewById(R.id.downloadButton).setOnClickListener(v -> downloadVideo());
    }

    private void downloadVideo() {
        try {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(videoUrl));

            request.setTitle("Video Download");
            request.setDescription("Downloading video file");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    "EchoNest/video_" + System.currentTimeMillis() + ".mp4"
            );

            downloadManager.enqueue(request);
            Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}