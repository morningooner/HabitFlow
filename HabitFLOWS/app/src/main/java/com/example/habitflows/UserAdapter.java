package com.example.habitflows;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<UserModel> userList;
    private List<String> followingList;
    private OnFollowClickListener followListener;

    public interface OnFollowClickListener {
        void onFollowClick(UserModel user, boolean isFollowing);
    }

    public UserAdapter(List<UserModel> userList, List<String> followingList, OnFollowClickListener followListener) {
        this.userList = userList;
        this.followingList = followingList;
        this.followListener = followListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);
        if (user == null) return;

        // Clean username: Remove @gmail.com or any email domain if present
        String rawUsername = user.getUsername();
        String cleanUsername = "Unknown User";
        
        if (rawUsername != null && !rawUsername.isEmpty()) {
            if (rawUsername.contains("@")) {
                cleanUsername = rawUsername.split("@")[0];
            } else {
                cleanUsername = rawUsername;
            }
        }
        
        holder.tvUsername.setText(cleanUsername);

        // Null safety for following check
        boolean isFollowing = user.getUid() != null && followingList != null && followingList.contains(user.getUid());
        
        if (isFollowing) {
            holder.btnFollow.setText("Following");
            holder.btnFollow.setAlpha(0.6f);
        } else {
            holder.btnFollow.setText("Follow");
            holder.btnFollow.setAlpha(1.0f);
        }

        holder.btnFollow.setOnClickListener(v -> {
            if (followListener != null && user.getUid() != null) {
                followListener.onFollowClick(user, isFollowing);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        MaterialButton btnFollow;
        ShapeableImageView ivAvatar;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvDiscoveryUsername);
            btnFollow = itemView.findViewById(R.id.btnFollow);
            ivAvatar = itemView.findViewById(R.id.ivUserAvatar);
        }
    }
}