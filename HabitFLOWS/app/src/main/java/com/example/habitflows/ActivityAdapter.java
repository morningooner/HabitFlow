package com.example.habitflows;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.format.DateUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityModel activity = activityList.get(position);
        if (activity == null) return;

        holder.tvUsername.setText(activity.getUsername());
        holder.tvHabitName.setText(activity.getHabitName());

        if (activity.getTimestamp() != null) {
            long time = activity.getTimestamp().toDate().getTime();
            String timeAgo = (String) DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
            holder.tvTime.setText(timeAgo);
        }

        // Set profile image from Base64
        String encodedImage = activity.getProfileImageBase64();
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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(activity.getUserEmail());
        });
    }

    @Override
    public int getItemCount() {
        return activityList.size();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvHabitName, tvTime;
        ShapeableImageView ivAvatar;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvActivityUser);
            tvHabitName = itemView.findViewById(R.id.tvActivityHabit);
            tvTime = itemView.findViewById(R.id.tvActivityTime);
            ivAvatar = itemView.findViewById(R.id.ivActivityAvatar);
        }
    }
}
