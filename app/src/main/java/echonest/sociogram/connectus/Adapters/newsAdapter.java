package echonest.sociogram.connectus.Adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connectus.R;
import echonest.sociogram.connectus.newsDetailActivity;
import com.kwabenaberko.newsapilib.models.Article;
import com.squareup.picasso.Picasso;

import java.util.List;

public class newsAdapter extends  RecyclerView.Adapter<newsAdapter.NewsViewHolder> {
    List<Article> articleList;
public newsAdapter(List<Article> articleList){
    this.articleList= articleList;
}
    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.news_recycler_row,parent,false);
    return  new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
    Article article = articleList.get(position);
    holder.titleTextView.setText(article.getTitle());
    holder.sourceTextView.setText(article.getSource().getName());
        Picasso.get().load(article.getUrlToImage()).error(R.drawable.no_image_icon).into(holder.imageView);

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), newsDetailActivity.class);
            intent.putExtra("url",article.getUrl());
            view.getContext().startActivity(intent);
        });

    }

    public void updateData(List<Article> data){
    articleList.clear();
    articleList.addAll(data);
    }
    @Override
    public int getItemCount() {
        return articleList.size();
    }

    class NewsViewHolder extends RecyclerView.ViewHolder{
        TextView titleTextView, sourceTextView;
        ImageView imageView;
        public NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView= itemView.findViewById(R.id.articleTitle);
            sourceTextView= itemView.findViewById(R.id.articleSource);
            imageView= itemView.findViewById(R.id.articleImageView);
        }
    }
}
