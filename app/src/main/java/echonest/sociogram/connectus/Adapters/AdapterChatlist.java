package echonest.sociogram.connectus.Adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import echonest.sociogram.connectus.ChatDetailActivity;
import echonest.sociogram.connectus.Models.ModelUser;
import com.example.connectus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class AdapterChatlist extends RecyclerView.Adapter<AdapterChatlist.MyHolder> {

    Context context;
    List<ModelUser> userList;
    private HashMap<String, String> lastMessageMap;
    private HashMap<String, Long> lastMessageTimestampMap; // New map for timestamps

    public AdapterChatlist(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
        this.lastMessageMap = new HashMap<>();
        this.lastMessageTimestampMap = new HashMap<>();
    }
    // Method to set timestamps for the last messages
    public void setLastMessageTimestampMap(HashMap<String, Long> lastMessageTimestampMap) {
        this.lastMessageTimestampMap = lastMessageTimestampMap;
        sortChatList(); // Sort the chat list whenever new timestamps are set
        notifyDataSetChanged();
    }

    // Sort userList based on timestamps
    private void sortChatList() {
        Collections.sort(userList, (user1, user2) -> {
            Long timestamp1 = lastMessageTimestampMap.getOrDefault(user1.getUserId(), 0L);
            Long timestamp2 = lastMessageTimestampMap.getOrDefault(user2.getUserId(), 0L);
            return Long.compare(timestamp2, timestamp1); // Sort in descending order
        });
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_chatlist, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        ModelUser user = userList.get(position);
        String hisUid = user.getUserId();
        String userImage = user.getProfilePhoto();
        String userName = user.getName();
        String lastMessage = lastMessageMap.get(hisUid);

        holder.nameTv.setText(userName);
        holder.lastMessageTv.setText(lastMessage != null ? lastMessage : "");

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

        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(context, ChatDetailActivity.class);
            intent.putExtra("hisUid", hisUid);
            context.startActivity(intent);
        });
        // Long-click listener for delete chat
        holder.itemView.setOnLongClickListener(view -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Chat")
                    .setMessage("Are you sure you want to delete this chat?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        deleteChat(hisUid, position); // Perform chat deletion
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    // Method to delete chat from Firebase and update the RecyclerView
    private void deleteChat(String userId, int position) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Reference to the chatlist in Firebase
        DatabaseReference chatListRef = FirebaseDatabase.getInstance()
                .getReference("Chatlist")
                .child(currentUserId)
                .child(userId);

        // Remove the person only from the Firebase Chatlist
        chatListRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Find and remove the specific user from userList by matching userId
                    for (int i = 0; i < userList.size(); i++) {
                        if (userList.get(i).getUserId().equals(userId)) {
                            userList.remove(i);
                            notifyItemRemoved(i);
                            Toast.makeText(context, "Chat deleted", Toast.LENGTH_SHORT).show();
                            break; // Stop after removing the specific user
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to delete chat", Toast.LENGTH_SHORT).show());
    }



    public void setLastMessageMap(HashMap<String, String> lastMessageMap) {
        this.lastMessageMap = lastMessageMap;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView profileIv, onlineStatusIv;
        TextView nameTv, lastMessageTv;

        MyHolder(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            onlineStatusIv = itemView.findViewById(R.id.onlineStatusIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            lastMessageTv = itemView.findViewById(R.id.lastMessageTv);
        }
    }
}

