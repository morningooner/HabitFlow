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
    private OnHabitEditListener editListener;

    public interface OnHabitDeleteListener {
        void onDelete(HabitModel habit);
    }

    public interface OnHabitEditListener {
        void onEdit(HabitModel habit);
    }

    public HabitAdapter(List<HabitModel> habitList, OnHabitEditListener editListener, OnHabitDeleteListener deleteListener) {
        this.habitList = habitList;
        this.editListener = editListener;
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

        // Calculate progress based on completedDays from the timer sessions
        int completed = habit.getCompletedDays();
        int total = habit.getDuration();
        
        int percentage = (total > 0) ? (int) (((float) completed / total) * 100) : 0;
        percentage = Math.min(percentage, 100); // Cap at 100%

        holder.habitProgressIndicator.setProgress(percentage);
        holder.tvHabitPercentage.setText(percentage + "%");

        // Hide Edit/Delete if no listeners provided (e.g., in Profile view)
        if (deleteListener == null) {
            holder.btnDeleteHabit.setVisibility(View.GONE);
        } else {
            holder.btnDeleteHabit.setVisibility(View.VISIBLE);
            holder.btnDeleteHabit.setOnClickListener(v -> deleteListener.onDelete(habit));
        }

        if (editListener == null) {
            holder.btnEditHabit.setVisibility(View.GONE);
        } else {
            holder.btnEditHabit.setVisibility(View.VISIBLE);
            holder.btnEditHabit.setOnClickListener(v -> editListener.onEdit(habit));
        }
    }

    @Override
    public int getItemCount() {
        return habitList.size();
    }

    static class HabitViewHolder extends RecyclerView.ViewHolder {
        TextView tvHabitName, tvHabitDuration, tvHabitPercentage;
        LinearProgressIndicator habitProgressIndicator;
        Button btnDeleteHabit, btnEditHabit;

        public HabitViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHabitName = itemView.findViewById(R.id.tvHabitName);
            tvHabitDuration = itemView.findViewById(R.id.tvHabitDuration);
            tvHabitPercentage = itemView.findViewById(R.id.tvHabitPercentage);
            habitProgressIndicator = itemView.findViewById(R.id.habitProgressIndicator);
            btnDeleteHabit = itemView.findViewById(R.id.btnDeleteHabit);
            btnEditHabit = itemView.findViewById(R.id.btnEditHabit);
        }
    }
}
