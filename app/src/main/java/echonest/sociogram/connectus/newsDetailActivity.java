package echonest.sociogram.connectus;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.connectus.R;
import com.example.connectus.databinding.ActivityNewsDetailBinding;

public class newsDetailActivity extends AppCompatActivity {
    ActivityNewsDetailBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        ((Window) window).setStatusBarColor(this.getResources().getColor(R.color.black));

        binding = ActivityNewsDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String url=getIntent().getStringExtra("url");
        WebSettings webSettings=binding.webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        binding.webView.setWebViewClient(new WebViewClient());
        binding.webView.loadUrl(url);

    }

    @Override
    public void onBackPressed() {
        if(binding.webView.canGoBack()){
            binding.webView.goBack();
        }else
        super.onBackPressed();
    }
}