package com.example.habitflows;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder> {

    private List<ActivityModel> activityList;
    private OnActivityClickListener listener;

    public interface OnActivityClickListener {
        void onUserClick(String email);
    }

    public ActivityAdapter(List<ActivityModel> activityList, OnActivityClickListener listener) {
        this.activityList = activityList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityModel activity = activityList.get(position);
        if (activity == null) return;

        holder.tvUsername.setText(activity.getUsername());
        holder.tvHabitName.setText(activity.getHabitName().toUpperCase());
        holder.tvDuration.setText(activity.getHabitDuration());

        int progress = activity.getProgress();
        holder.progressIndicator.setProgress(progress);
        holder.tvPercentage.setText(progress + "%");

        if (activity.getTimestamp() != null) {
            long time = activity.getTimestamp().toDate().getTime();
            String timeAgo = (String) DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.tvTime.setText(timeAgo);
        }

        // Profile avatar
        String encodedAvatar = activity.getProfileImageBase64();
        if (encodedAvatar != null && !encodedAvatar.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(encodedAvatar, Base64.DEFAULT);
                holder.ivAvatar.setImageBitmap(BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
            } catch (Exception e) {
                holder.ivAvatar.setImageResource(R.drawable.profilepic);
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.profilepic);
        }

        // Caption
        String caption = activity.getCaption();
        if (caption != null && !caption.isEmpty()) {
            holder.tvCaption.setText(caption);
            holder.tvCaption.setVisibility(View.VISIBLE);
        } else {
            holder.tvCaption.setVisibility(View.GONE);
        }

        // Post image
        String encodedPostImage = activity.getPostImageBase64();
        if (encodedPostImage != null && !encodedPostImage.isEmpty()) {
            try {
                byte[] decoded = Base64.decode(encodedPostImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                holder.ivPostImage.setImageBitmap(bitmap);
                holder.ivPostImage.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                holder.ivPostImage.setVisibility(View.GONE);
            }
        } else {
            holder.ivPostImage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(activity.getUserEmail());
        });
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvHabitName, tvTime, tvDuration, tvPercentage, tvCaption;
        ShapeableImageView ivAvatar;
        ImageView ivPostImage;
        LinearProgressIndicator progressIndicator;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvPostUser);
            tvHabitName = itemView.findViewById(R.id.tvPostHabitName);
            tvTime = itemView.findViewById(R.id.tvPostTime);
            ivAvatar = itemView.findViewById(R.id.ivPostAvatar);
            tvDuration = itemView.findViewById(R.id.tvPostDuration);
            tvPercentage = itemView.findViewById(R.id.tvPostPercentage);
            progressIndicator = itemView.findViewById(R.id.postProgressIndicator);
            tvCaption = itemView.findViewById(R.id.tvPostCaption);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
        }
    }
}
