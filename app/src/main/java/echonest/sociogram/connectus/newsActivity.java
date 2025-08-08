package echonest.sociogram.connectus;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import echonest.sociogram.connectus.Adapters.newsAdapter;
import com.example.connectus.R;
import com.example.connectus.databinding.ActivityNewsBinding;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.kwabenaberko.newsapilib.NewsApiClient;
import com.kwabenaberko.newsapilib.models.Article;
import com.kwabenaberko.newsapilib.models.request.TopHeadlinesRequest;
import com.kwabenaberko.newsapilib.models.response.ArticleResponse;

import java.util.ArrayList;
import java.util.List;

public class newsActivity extends AppCompatActivity implements View.OnClickListener {
    ActivityNewsBinding binding;
RecyclerView recyclerView;
List<Article> articleList= new ArrayList<>();
newsAdapter adapter;
LinearProgressIndicator progressIndicator;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        ((Window) window).setStatusBarColor(this.getResources().getColor(R.color.black));

        binding= ActivityNewsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        recyclerView=findViewById(R.id.newsRecyclerView);
        recyclerView = binding.newsRecyclerView;
        progressIndicator = binding.progressBar;

        binding.btn1news.setOnClickListener(this);
        binding.btn2news.setOnClickListener(this);
        binding.btn3news.setOnClickListener(this);
        binding.btn4news.setOnClickListener(this);
        binding.btn5news.setOnClickListener(this);
        binding.btn6news.setOnClickListener(this);
        binding.btn7news.setOnClickListener(this);

        binding.searchViewNews.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                getNews("GENERAL",query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        setupRecylerView();

        getNews("GENERAL",null);
    }

    void changeInProgress(boolean show){
        if(show){
            progressIndicator.setVisibility(View.VISIBLE);
        }else{
            progressIndicator.setVisibility(View.INVISIBLE);
        }
    }
    void setupRecylerView(){
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new newsAdapter(articleList);
        recyclerView.setAdapter(adapter);
    }


    void getNews(String category, String  query){
        changeInProgress(true);
        NewsApiClient newsApiClient= new NewsApiClient("96493867975343b7a1921f87271ca662");
        newsApiClient.getTopHeadlines(
                new TopHeadlinesRequest.Builder()
                        .language("en").category(category).q(query)
                        .build(),
                new NewsApiClient.ArticlesResponseCallback() {
                    @Override
                    public void onSuccess(ArticleResponse response) {
//                        Log.i("got response", response.toString());

//                        response.getArticles().forEach((article -> {
//                            Log.i("Article",article.getTitle());
//                        }));
                        runOnUiThread(()->{
                            changeInProgress(false);
                            articleList = response.getArticles();
                            adapter.updateData(articleList);
                            adapter.notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.i("got Failure", throwable.getMessage());

                    }
                }
        );
    }

    @Override
    public void onClick(View view) {
        Button btn= (Button) view;
        String category= btn.getText().toString();
        getNews(category,null);

    }
}