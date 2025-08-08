package echonest.sociogram.connectus.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.connectus.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

import echonest.sociogram.connectus.Models.ModelChatlist;
import echonest.sociogram.connectus.Models.ModelUser;

public class AdapterRequestList extends RecyclerView.Adapter<AdapterRequestList.MyHolder> {

    private Context context;
    private List<ModelUser> requestList;

    public AdapterRequestList(Context context, List<ModelUser> requestList) {
        this.context = context;
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_request, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        ModelUser user = requestList.get(position);

        holder.nameTv.setText(user.getName());
        Glide.with(context)
                .load(user.getProfilePhoto())
                .placeholder(R.drawable.avatar)
                .into(holder.profileIv);

        // Accept request
        holder.acceptBtn.setOnClickListener(v -> acceptRequest(user.getUserId()));
        holder.cancelBtn.setOnClickListener(v -> cancelRequest(user.getUserId()));

    }

    private void acceptRequest(String senderUid) {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Add each other to Chatlist
        DatabaseReference chatlistRef = FirebaseDatabase.getInstance().getReference("Chatlist");
        chatlistRef.child(myUid).child(senderUid).setValue(new ModelChatlist(senderUid));
        chatlistRef.child(senderUid).child(myUid).setValue(new ModelChatlist(myUid));

        // Remove request
        FirebaseDatabase.getInstance()
                .getReference("ChatRequests")
                .child(myUid)
                .child(senderUid)
                .removeValue();

        Toast.makeText(context, "Request accepted", Toast.LENGTH_SHORT).show();

        // Remove from requestList using UID match
        removeUserFromList(senderUid);
    }

    private void cancelRequest(String senderUid) {
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseDatabase.getInstance()
                .getReference("ChatRequests")
                .child(myUid)
                .child(senderUid)
                .removeValue();

        Toast.makeText(context, "Request canceled", Toast.LENGTH_SHORT).show();

        removeUserFromList(senderUid);
    }

    private void removeUserFromList(String uid) {
        for (int i = 0; i < requestList.size(); i++) {
            if (requestList.get(i).getUserId().equals(uid)) {
                requestList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }




    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class MyHolder extends RecyclerView.ViewHolder {
        ImageView profileIv;
        TextView nameTv;
        Button acceptBtn, cancelBtn;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            acceptBtn = itemView.findViewById(R.id.acceptBtn);
            cancelBtn = itemView.findViewById(R.id.cancelBtn);
        }
    }
}
