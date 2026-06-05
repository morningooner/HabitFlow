package com.example.habitflows;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class RankAdapter extends RecyclerView.Adapter<RankAdapter.RankViewHolder> {

    private List<UserModel> userList;

    public RankAdapter(List<UserModel> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public RankViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rank_user, parent, false);
        return new RankViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RankViewHolder holder, int position) {
        UserModel user = userList.get(position);
        
        holder.tvRankPosition.setText(String.valueOf(position + 1));
        holder.tvUserName.setText(user.getUsername());
        holder.tvUserXP.setText(user.getXp() + " XP");

        if (user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageBase64())
                    .placeholder(R.drawable.profilepic)
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(R.drawable.profilepic);
        }

        // Optional: Show medal/badge icons for top 3
        if (position == 0) {
            holder.tvRankPosition.setTextColor(0xFFFFD700); // Gold
        } else if (position == 1) {
            holder.tvRankPosition.setTextColor(0xFFC0C0C0); // Silver
        } else if (position == 2) {
            holder.tvRankPosition.setTextColor(0xFFCD7F32); // Bronze
        } else {
            holder.tvRankPosition.setTextColor(0xFFFFFFFF);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class RankViewHolder extends RecyclerView.ViewHolder {
        TextView tvRankPosition, tvUserName, tvUserXP;
        ShapeableImageView ivUserAvatar;
        ImageView ivSmallBadge;

        public RankViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRankPosition = itemView.findViewById(R.id.tvRankPosition);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserXP = itemView.findViewById(R.id.tvUserXP);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            ivSmallBadge = itemView.findViewById(R.id.ivSmallBadge);
        }
    }
}
