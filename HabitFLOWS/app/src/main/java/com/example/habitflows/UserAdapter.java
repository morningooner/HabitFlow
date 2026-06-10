package com.example.habitflows;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
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
    private OnUserClickListener userClickListener;

    public interface OnFollowClickListener {
        void onFollowClick(UserModel user, boolean isFollowing);
    }

    public interface OnUserClickListener {
        void onUserClick(UserModel user);
    }

    public UserAdapter(List<UserModel> userList, List<String> followingList, 
                       OnFollowClickListener followListener, OnUserClickListener userClickListener) {
        this.userList = userList;
        this.followingList = followingList;
        this.followListener = followListener;
        this.userClickListener = userClickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user_compact, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);
        if (user == null) return;

        // Set username
        String rawUsername = user.getUsername();
        String cleanUsername = (rawUsername != null && rawUsername.contains("@")) 
                ? rawUsername.split("@")[0] : (rawUsername != null ? rawUsername : "Unknown User");
        holder.tvUsername.setText(cleanUsername);

        // Set profile image from Base64
        String encodedImage = user.getProfileImageBase64();
        if (encodedImage != null && !encodedImage.isEmpty()) {
            try {
                byte[] decodedByte = Base64.decode(encodedImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
                holder.ivAvatar.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.ivAvatar.setImageResource(R.drawable.profilepic);
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.profilepic);
        }

        // Follow button logic
        boolean isFollowing = user.getUid() != null && followingList != null && followingList.contains(user.getUid());
        holder.btnFollow.setText(isFollowing ? "Following" : "Follow");
        holder.btnFollow.setAlpha(isFollowing ? 0.6f : 1.0f);

        holder.btnFollow.setOnClickListener(v -> {
            if (followListener != null) followListener.onFollowClick(user, isFollowing);
        });

        // Navigate to profile on item click
        holder.itemView.setOnClickListener(v -> {
            if (userClickListener != null) userClickListener.onUserClick(user);
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