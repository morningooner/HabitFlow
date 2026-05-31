package com.example.habitflows;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;

public class HabitAdapter extends RecyclerView.Adapter<HabitAdapter.HabitViewHolder> {

    private List<HabitModel> habitList;
    private OnHabitDeleteListener deleteListener;

    public interface OnHabitDeleteListener {
        void onDelete(HabitModel habit);
    }

    public HabitAdapter(List<HabitModel> habitList, OnHabitDeleteListener deleteListener) {
        this.habitList = habitList;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public HabitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_habit, parent, false);
        return new HabitViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HabitViewHolder holder, int position) {
        HabitModel habit = habitList.get(position);
        holder.tvHabitName.setText(habit.getHabitName());
        holder.tvHabitDuration.setText(habit.getDuration() + " Days Goal");

        // FIXED: Calculate progress based on completedDays from the timer sessions
        int completed = habit.getCompletedDays();
        int total = habit.getDuration();
        
        int percentage = (total > 0) ? (int) (((float) completed / total) * 100) : 0;
        percentage = Math.min(percentage, 100); // Cap at 100%

        holder.habitProgressIndicator.setProgress(percentage);
        holder.tvHabitPercentage.setText(percentage + "%");

        holder.btnDeleteHabit.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDelete(habit);
            }
        });
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView tvHabitName, tvHabitDuration, tvHabitPercentage;
        LinearProgressIndicator habitProgressIndicator;
        Button btnDeleteHabit;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHabitName = itemView.findViewById(R.id.tvHabitName);
            tvHabitDuration = itemView.findViewById(R.id.tvHabitDuration);
            tvHabitPercentage = itemView.findViewById(R.id.tvHabitPercentage);
            habitProgressIndicator = itemView.findViewById(R.id.habitProgressIndicator);
            btnDeleteHabit = itemView.findViewById(R.id.btnDeleteHabit);
        }
    }
}