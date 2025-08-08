package echonest.sociogram.connectus.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import echonest.sociogram.connectus.ChatDetailActivity;
import echonest.sociogram.connectus.Models.ModelUser;
import com.example.connectus.R;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

public class AdapterUsers extends  RecyclerView.Adapter<AdapterUsers.MyHolder> {


    Context context;
    List<ModelUser>userList;
    //constructor


    public AdapterUsers(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;

    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(context).inflate(R.layout.row_peoplelist, parent,false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
// get data
        ModelUser user = userList.get(position);
        String hisUid = user.getUserId();
        String userImage = user.getProfilePhoto();
        String userName = user.getName();

        //set data
        holder.nameTv.setText(userName);
//        String email=userList.get(position).getEmail();



        try {
            Glide.with(context)
                    .load(userImage)
                    .placeholder(R.drawable.avatar)
                    .into(holder.profileIv);
        } catch (Exception e) {
            Glide.with(context)
                    .load(R.drawable.avatar)
                    .into(holder.profileIv);
        }


        String onlineStatus = user.getOnlineStatus();
        if (onlineStatus != null && onlineStatus.equals("online")) {
            holder.onlineStatusIv.setImageResource(R.drawable.circle_online);
        } else {
            holder.onlineStatusIv.setImageResource(R.drawable.circle_offline);
        }

        // handle item click
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, ChatDetailActivity.class);
            intent.putExtra("hisUid", hisUid);
            context.startActivity(intent);
        });
    }


    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView profileIv, onlineStatusIv;
        TextView nameTv ;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            onlineStatusIv = itemView.findViewById(R.id.onlineStatusIv);
            nameTv = itemView.findViewById(R.id.nameTv);

        }
    }

}
