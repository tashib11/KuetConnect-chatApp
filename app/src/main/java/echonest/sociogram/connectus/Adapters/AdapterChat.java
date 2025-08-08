package echonest.sociogram.connectus.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import echonest.sociogram.connectus.FullScreenImageActivity;
import echonest.sociogram.connectus.FullScreenVideoActivity;
import echonest.sociogram.connectus.Models.ModelChat;
import com.example.connectus.R;

public class AdapterChat extends RecyclerView.Adapter<AdapterChat.MyHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;

    private final Context context;
    private String imageUrl;
    private List<ModelChat> chatList;
    private FirebaseUser fUser;

    public AdapterChat(Context context, List<ModelChat> chatList, String imageUrl) {
        this.context = context;
        this.chatList = chatList;
        this.imageUrl = imageUrl;
        this.fUser = FirebaseAuth.getInstance().getCurrentUser();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // inflate layout based on viewType
        int layout = (viewType == MSG_TYPE_LEFT) ? R.layout.sample_receiver : R.layout.sample_sender;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        if (chatList == null || position < 0 || position >= chatList.size()) return;

        ModelChat currentMessage = chatList.get(position);

        // Format timestamp
        holder.timeTv.setText(formatTimestamp(currentMessage.getTimestamp()));

        // Reset visibility for recycled views
        resetViewVisibility(holder);

        // Handle different message types
        String type = currentMessage.getType();
        if ("text".equals(type)) {
            handleTextMessage(holder, currentMessage);
        } else if ("image".equals(type)) {
            handleImageMessage(holder, currentMessage);
        } else if ("video".equals(type)) {
            handleVideoMessage(holder, currentMessage);
        } else {
            // fallback
            holder.messageTv.setVisibility(View.VISIBLE);
            holder.messageTv.setText(currentMessage.getMessage());
        }

        // Set profile image
        setProfileImage(holder);

        // Show message status only for the last message
        if (position == chatList.size() - 1) {
            if (fUser != null && currentMessage.getSender().equals(fUser.getUid())) {
                holder.isSeenTv.setVisibility(View.VISIBLE);
                holder.isSeenTv.setText(currentMessage.getMessageStatus());
            } else {
                holder.isSeenTv.setVisibility(View.GONE);
            }
        } else {
            holder.isSeenTv.setVisibility(View.GONE);
        }

        // Add long-click listener to message layout
        holder.messageLayout.setOnLongClickListener(v -> {
            showDeleteDialog(position);
            return true;
        });

        // For image/video also allow long press on their views
        if ("image".equals(type)) {
            holder.messageIv.setOnLongClickListener(v -> {
                showDeleteDialog(position);
                return true;
            });
        }
        if ("video".equals(type)) {
            holder.messageVideoThumbnail.setOnLongClickListener(v -> {
                showDeleteDialog(position);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return (chatList == null) ? 0 : chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        // ensure fUser available
        if (fUser == null) fUser = FirebaseAuth.getInstance().getCurrentUser();
        ModelChat msg = chatList.get(position);
        return (fUser != null && msg.getSender().equals(fUser.getUid())) ? MSG_TYPE_RIGHT : MSG_TYPE_LEFT;
    }

    // Method to update the profile image dynamically
    public void updateProfileImage(String newImageUrl) {
        this.imageUrl = newImageUrl;
        notifyDataSetChanged(); // Refresh the RecyclerView
    }

    private void resetViewVisibility(MyHolder holder) {
        holder.messageTv.setVisibility(View.GONE);
        holder.messageIv.setVisibility(View.GONE);
        holder.messageVideoThumbnail.setVisibility(View.GONE);
        holder.playButtonOverlay.setVisibility(View.GONE);
        holder.progressBar.setVisibility(View.GONE);
        holder.videoProgressBar.setVisibility(View.GONE);
        holder.progressPercentage.setVisibility(View.GONE);
        holder.videoProgressPercentage.setVisibility(View.GONE);
    }

    private void handleTextMessage(MyHolder holder, ModelChat currentMessage) {
        holder.messageTv.setVisibility(View.VISIBLE);
        String msg = currentMessage.getMessage();
        if ("[Encrypted Message]".equals(msg) || "[Decryption Failed]".equals(msg)) {
            holder.messageTv.setText("🔒 " + msg);
            holder.messageTv.setTypeface(null, Typeface.ITALIC);
            holder.messageTv.setTextColor(ContextCompat.getColor(context, R.color.gray));
        } else {
            holder.messageTv.setText(msg);
            holder.messageTv.setTypeface(null, Typeface.NORMAL);
            holder.messageTv.setTextColor(ContextCompat.getColor(context, R.color.black));
        }
    }

    private void handleImageMessage(MyHolder holder, ModelChat currentMessage) {
        holder.messageIv.setVisibility(View.VISIBLE);

        if (currentMessage.getLocalImageUri() != null && currentMessage.isUploading()) {
            // show local image with progress
            try {
                int blurRadius = 25 - (currentMessage.getUploadProgress() * 25 / 100);
                blurRadius = Math.max(1, blurRadius);
                Glide.with(context)
                        .load(currentMessage.getLocalImageUri())
                        .into(holder.messageIv); // kept simple to avoid transform import issues
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.progressBar.setProgress(currentMessage.getUploadProgress());
                holder.progressPercentage.setVisibility(View.VISIBLE);
                holder.progressPercentage.setText(currentMessage.getUploadProgress() + "%");
                holder.messageIv.setAlpha(0.3f + (0.7f * currentMessage.getUploadProgress() / 100));
            } catch (Exception e) {
                Log.e("AdapterChat", "Error loading local image", e);
            }
        } else if (currentMessage.getMessage() != null) {
            // Display uploaded image URL
            Glide.with(context)
                    .load(currentMessage.getMessage())
                    .placeholder(R.drawable.baseline_image_24)
                    .into(holder.messageIv);

            holder.messageIv.setAlpha(1.0f);
            holder.progressBar.setVisibility(View.GONE);
            holder.progressPercentage.setVisibility(View.GONE);
        }

        holder.messageIv.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullScreenImageActivity.class);
            String imageUrl = currentMessage.getMessage() != null ? currentMessage.getMessage() : currentMessage.getLocalImageUri();
            intent.putExtra("image_url", imageUrl);
            context.startActivity(intent);
        });
    }

    private void handleVideoMessage(MyHolder holder, ModelChat currentMessage) {
        holder.messageVideoThumbnail.setVisibility(View.VISIBLE);
        holder.playButtonOverlay.setVisibility(View.GONE);

        if (currentMessage.isUploading()) {
            try {
                Glide.with(context)
                        .asBitmap()
                        .load(currentMessage.getLocalImageUri() != null ? Uri.parse(currentMessage.getLocalImageUri()) : R.drawable.baseline_image_24)
                        .into(holder.messageVideoThumbnail);

                holder.videoProgressBar.setVisibility(View.VISIBLE);
                holder.videoProgressPercentage.setVisibility(View.VISIBLE);
                holder.videoProgressBar.setProgress(currentMessage.getUploadProgress());
                holder.videoProgressPercentage.setText(currentMessage.getUploadProgress() + "%");
            } catch (Exception e) {
                Log.e("AdapterChat", "Error loading video thumbnail", e);
            }
        } else {
            Glide.with(context)
                    .asBitmap()
                    .load(currentMessage.getMessage())
                    .into(holder.messageVideoThumbnail);

            holder.videoProgressBar.setVisibility(View.GONE);
            holder.videoProgressPercentage.setVisibility(View.GONE);
            holder.playButtonOverlay.setVisibility(View.VISIBLE);
        }

        holder.messageVideoThumbnail.setOnClickListener(v -> {
            if (!currentMessage.isUploading()) {
                Intent intent = new Intent(context, FullScreenVideoActivity.class);
                intent.putExtra("videoUrl", currentMessage.getMessage());
                context.startActivity(intent);
            }
        });
        holder.playButtonOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullScreenVideoActivity.class);
            intent.putExtra("videoUrl", currentMessage.getMessage());
            context.startActivity(intent);
        });
    }

    private void setProfileImage(MyHolder holder) {
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.avatar)
                .into(holder.profileIv);
    }

    private String formatTimestamp(String timestamp) {
        try {
            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
            cal.setTimeInMillis(Long.parseLong(timestamp));
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm aa", Locale.ENGLISH);
            return sdf.format(cal.getTime());
        } catch (NumberFormatException e) {
            Log.e("Timestamp Error", "Invalid timestamp format", e);
            return "";
        }
    }

    private void showDeleteDialog(int position) {
        if (position < 0 || position >= getItemCount()) return;
        ModelChat message = chatList.get(position);

        new MaterialAlertDialogBuilder(context)
                .setTitle("Delete Message")
                .setMessage("Delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMessage(position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // delete by position (safe)
    private void deleteMessage(int position) {
        if (position < 0 || position >= chatList.size()) return;
        ModelChat message = chatList.get(position);
        String currentUserUid = FirebaseAuth.getInstance().getUid();

        if (message == null || currentUserUid == null || !message.getSender().equals(currentUserUid)) {
            Toast.makeText(context, "Can't delete others' messages", Toast.LENGTH_SHORT).show();
            return;
        }

        String messageId = message.getMessageId();
        if (messageId == null) {
            Toast.makeText(context, "Invalid message id", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference messageRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(messageId);

        if ("image".equals(message.getType()) || "video".equals(message.getType())) {
            try {
                StorageReference storageRef = FirebaseStorage.getInstance()
                        .getReferenceFromUrl(message.getMessage());
                storageRef.delete().addOnSuccessListener(aVoid -> {
                    // now remove from DB and local list
                    removeMessageFromDbAndList(messageRef, messageId);
                }).addOnFailureListener(e -> {
                    Toast.makeText(context, "Media delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                // if URL invalid, still try to delete DB entry
                removeMessageFromDbAndList(messageRef, messageId);
            }
        } else {
            removeMessageFromDbAndList(messageRef, messageId);
        }
    }

    /**
     * Remove by messageId to locate correct index, then notify RecyclerView properly.
     */
    private void removeMessageFromDbAndList(DatabaseReference messageRef, String messageId) {
        messageRef.removeValue()
                .addOnSuccessListener(unused -> {
                    // find index by messageId (do NOT rely on a passed 'position' alone)
                    int index = -1;
                    for (int i = 0; i < chatList.size(); i++) {
                        ModelChat m = chatList.get(i);
                        if (m != null && messageId.equals(m.getMessageId())) {
                            index = i;
                            break;
                        }
                    }

                    if (index != -1) {
                        chatList.remove(index);
                        notifyItemRemoved(index);
                        // IMPORTANT: inform RecyclerView that later items shifted
                        notifyItemRangeChanged(index, chatList.size() - index);
                    } else {
                        // If not found locally, refresh entire list to be safe
                        notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView profileIv, messageIv, messageVideoThumbnail, playButtonOverlay;
        TextView messageTv, timeTv, isSeenTv, progressPercentage, videoProgressPercentage;
        LinearLayout messageLayout;
        ProgressBar progressBar, videoProgressBar;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            messageIv = itemView.findViewById(R.id.messageIvImage);
            messageTv = itemView.findViewById(R.id.messageTv);
            timeTv = itemView.findViewById(R.id.timeTv);
            isSeenTv = itemView.findViewById(R.id.isSeenTv);
            messageLayout = itemView.findViewById(R.id.messageLayout);
            messageVideoThumbnail = itemView.findViewById(R.id.messageVideoThumbnail);
            playButtonOverlay = itemView.findViewById(R.id.playButtonOverlay);
            progressBar = itemView.findViewById(R.id.progressBar);
            videoProgressBar = itemView.findViewById(R.id.videoProgressBar);
            progressPercentage = itemView.findViewById(R.id.progressPercentage);
            videoProgressPercentage = itemView.findViewById(R.id.videoProgressPercentage);
        }
    }
}
