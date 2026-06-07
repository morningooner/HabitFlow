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

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private List<RequestModel> requestList;
    private OnRequestActionListener listener;

    public interface OnRequestActionListener {
        void onAccept(RequestModel request);
        void onDecline(RequestModel request);
    }

    public RequestAdapter(List<RequestModel> requestList, OnRequestActionListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        RequestModel request = requestList.get(position);
        if (request == null) return;

        holder.tvUsername.setText(request.getFromUsername());

        String encodedImage = request.getFromProfileImage();
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

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(request);
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) listener.onDecline(request);
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername;
        ShapeableImageView ivAvatar;
        MaterialButton btnAccept, btnDecline;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvRequestUsername);
            ivAvatar = itemView.findViewById(R.id.ivRequestAvatar);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}
