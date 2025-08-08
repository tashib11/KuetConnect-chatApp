package echonest.sociogram.connectus.Adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connectus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import echonest.sociogram.connectus.FullScreenImageActivity;
import echonest.sociogram.connectus.FullScreenVideoActivity;
import echonest.sociogram.connectus.Models.ModelChat;

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
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == MSG_TYPE_LEFT) ? R.layout.sample_receiver : R.layout.sample_sender;
        View view = LayoutInflater.from(context).inflate(layout, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        ModelChat currentMessage = chatList.get(position);

        // Format timestamp
        holder.timeTv.setText(formatTimestamp(currentMessage.getTimestamp()));

        // Reset visibility for recycled views
        resetViewVisibility(holder);

        // Handle different message types
        switch (currentMessage.getType()) {
            case "text":
                handleTextMessage(holder, currentMessage);
                break;

            case "image":
                handleImageMessage(holder, currentMessage);
                break;

            case "video":
                handleVideoMessage(holder, currentMessage);
                break;
        }

        // Set profile image
        setProfileImage(holder);

        // Show message status only for the last message
        if (position == chatList.size() - 1) {
            if (currentMessage.getSender().equals(fUser.getUid())) {
                holder.isSeenTv.setVisibility(View.VISIBLE);
                holder.isSeenTv.setText(currentMessage.getMessageStatus());
            } else {
                holder.isSeenTv.setVisibility(View.GONE);
            }
        } else {
            holder.isSeenTv.setVisibility(View.GONE);
        }

        // Add long-click listener to message layout for text messages
        holder.messageLayout.setOnLongClickListener(v -> {
            showDeleteDialog(position); // Show delete dialog
            return true; // Indicate the event was handled
        });

        // Add long-click listener for image messages
        if ("image".equals(currentMessage.getType())) {
            holder.messageIv.setOnLongClickListener(v -> {
                showDeleteDialog(position); // Show delete dialog
                return true; // Indicate the event was handled
            });
        }

        // Add long-click listener for video messages
        if ("video".equals(currentMessage.getType())) {
            holder.messageVideoThumbnail.setOnLongClickListener(v -> {
                showDeleteDialog(position); // Show delete dialog
                return true; // Indicate the event was handled
            });
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        return chatList.get(position).getSender().equals(fUser.getUid()) ? MSG_TYPE_RIGHT : MSG_TYPE_LEFT;
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
    }

    private void handleTextMessage(MyHolder holder, ModelChat currentMessage) {
        holder.messageTv.setVisibility(View.VISIBLE);
        if ("[Encrypted Message]".equals(currentMessage.getMessage()) || "[Decryption Failed]".equals(currentMessage.getMessage())) {
            holder.messageTv.setText("🔒 " + currentMessage.getMessage()); // Add lock icon
            holder.messageTv.setTypeface(null, Typeface.ITALIC);
            holder.messageTv.setTextColor(ContextCompat.getColor(context, R.color.gray));
        } else {
            holder.messageTv.setText(currentMessage.getMessage());
            holder.messageTv.setTypeface(null, Typeface.NORMAL);
            holder.messageTv.setTextColor(ContextCompat.getColor(context, R.color.black));
        }
    }

    private void handleImageMessage(MyHolder holder, ModelChat currentMessage) {
        holder.messageIv.setVisibility(View.VISIBLE);

        if (currentMessage.getLocalImageUri() != null) {
            if (currentMessage.isUploading()) {
                // Calculate blur radius dynamically (min 1, max 25)
                int blurRadius = 25 - (currentMessage.getUploadProgress() * 25 / 100);
                blurRadius = Math.max(1, blurRadius); // Avoid 0 or negative blur radius

                // Load the image with dynamic blur and opacity
                Glide.with(context)
                        .load(currentMessage.getLocalImageUri())
                        .transform(new jp.wasabeef.glide.transformations.BlurTransformation(blurRadius)) // Dynamic blur
                        .into(holder.messageIv);

                // Show progress bar and update percentage
                holder.progressBar.setVisibility(View.VISIBLE);
                holder.progressBar.setProgress(currentMessage.getUploadProgress());
                holder.progressPercentage.setVisibility(View.VISIBLE);
                holder.progressPercentage.setText(currentMessage.getUploadProgress() + "%");

                // Gradually increase alpha (opacity) of the image
                holder.messageIv.setAlpha(0.3f + (0.7f * currentMessage.getUploadProgress() / 100));
            } else {
                // Upload completed - show clean image
                Glide.with(context)
                        .load(currentMessage.getMessage()) // Load uploaded URL
                        .placeholder(R.drawable.baseline_image_24)
                        .into(holder.messageIv);

                holder.messageIv.setAlpha(1.0f); // Fully opaque
                holder.progressBar.setVisibility(View.GONE);
                holder.progressPercentage.setVisibility(View.GONE);
            }
        } else if (currentMessage.getMessage() != null) {
            // Display uploaded image
            Glide.with(context)
                    .load(currentMessage.getMessage())
                    .placeholder(R.drawable.baseline_image_24)
                    .into(holder.messageIv);

            holder.messageIv.setAlpha(1.0f); // Fully opaque
            holder.progressBar.setVisibility(View.GONE);
            holder.progressPercentage.setVisibility(View.GONE);
        }

        // Add click listener for full-screen view
        holder.messageIv.setOnClickListener(v -> {
            Intent intent = new Intent(context, FullScreenImageActivity.class);
            String imageUrl = currentMessage.getMessage() != null ? currentMessage.getMessage() : currentMessage.getLocalImageUri();
            intent.putExtra("image_url", imageUrl);
            context.startActivity(intent);
        });
        Log.d("ChatAdapter", "Image URL: " + currentMessage.getMessage());
    }

    private void handleVideoMessage(MyHolder holder, ModelChat currentMessage) {
        holder.messageVideoThumbnail.setVisibility(View.VISIBLE);
        holder.playButtonOverlay.setVisibility(View.GONE); // Hide play button during upload

        if (currentMessage.isUploading()) {
            // Show video thumbnail and update progress bar and percentage
            Glide.with(context)
                    .asBitmap()
                    .load(currentMessage.getLocalImageUri() != null ? Uri.parse(currentMessage.getLocalImageUri()) : R.drawable.baseline_image_24)
                    .into(holder.messageVideoThumbnail);

            holder.videoProgressBar.setVisibility(View.VISIBLE);
            holder.videoProgressPercentage.setVisibility(View.VISIBLE);

            // Update progress
            holder.videoProgressBar.setProgress(currentMessage.getUploadProgress());
            holder.videoProgressPercentage.setText(currentMessage.getUploadProgress() + "%");
        } else {
            // Video upload completed
            Glide.with(context)
                    .asBitmap()
                    .load(currentMessage.getMessage()) // Video thumbnail URL

                    .into(holder.messageVideoThumbnail);

            holder.videoProgressBar.setVisibility(View.GONE);
            holder.videoProgressPercentage.setVisibility(View.GONE);
            holder.playButtonOverlay.setVisibility(View.VISIBLE); // Show play button once upload completes
        }

        // Click listeners for play functionality
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
        ModelChat message = chatList.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Message")
                .setMessage("Delete this message?")
                .setPositiveButton("Delete", (d, w) -> deleteMessage(message))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMessage(ModelChat message) {
        String currentUserUid = FirebaseAuth.getInstance().getUid();
        if (message == null || !message.getSender().equals(currentUserUid)) {
            Toast.makeText(context, "Can't delete others' messages", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference messageRef = FirebaseDatabase.getInstance()
                .getReference("Chats")
                .child(message.getMessageId());

        if (message.getType().equals("image") || message.getType().equals("video")) {
            // Delete media from Storage
            StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReferenceFromUrl(message.getMessage());
            storageRef.delete().addOnSuccessListener(unused -> {
                deleteFromDatabase(messageRef);
            }).addOnFailureListener(e -> {
                Toast.makeText(context, "Media delete failed", Toast.LENGTH_SHORT).show();
            });
        } else {
            deleteFromDatabase(messageRef);
        }
    }

    private void deleteFromDatabase(DatabaseReference messageRef) {
        messageRef.removeValue().addOnSuccessListener(unused -> {
            // Remove from local list
            int position = chatList.indexOf(messageRef);
            if (position != -1) {
                chatList.remove(position);
                notifyItemRemoved(position);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show();
        });
    }

//    private void updateDeletedMessage(DatabaseReference messageRef) {
//        HashMap<String, Object> hashMap = new HashMap<>();
//        hashMap.put("message", "This message has been deleted.");
//        hashMap.put("type", "text"); // Update type to indicate the message is deleted
//        messageRef.updateChildren(hashMap).addOnSuccessListener(unused ->
//                Toast.makeText(context, "Message deleted successfully.", Toast.LENGTH_SHORT).show()
//        ).addOnFailureListener(e ->
//                Toast.makeText(context, "Failed to update the message. Please try again.", Toast.LENGTH_SHORT).show()
//        );
//    }


    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView profileIv, messageIv, messageVideoThumbnail, playButtonOverlay;
        TextView messageTv, timeTv, isSeenTv,  progressPercentage, videoProgressPercentage;
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
    }}