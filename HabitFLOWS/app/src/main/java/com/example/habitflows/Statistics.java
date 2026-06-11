package com.example.habitflows;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Statistics extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;

    // Top bar
    private TextView tvLevelDisplay, tvUserHeaderName;

    // Identity row
    private TextView tvRpgName, tvRpgLv, tvCompletedHabitsCount, tvRankLetter;

    // Overall
    private CircularProgressIndicator cpOverall;
    private TextView tvOverallPercent;

    // Chips
    private TextView tvChipHabits, tvChipToday, tvChipWeekly;

    // Daily
    private LinearProgressIndicator dailyProgressBar;
    private TextView tvDailyPercent, tvDailyLabel;

    // Weekly
    private LinearProgressIndicator weeklyProgressBar;
    private TextView tvWeeklyPercent;
    private LinearLayout llWeeklyChart;

    // Dynamic sections
    private LinearLayout llHabitsBreakdown, llHabitChecklist;

    private List<HabitModel> habitList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistics);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();

        ConstraintLayout topBar = findViewById(R.id.topBar);
        CardView statusWindow = findViewById(R.id.statusWindow);
        LinearLayout bottomNav = findViewById(R.id.bottomIcons);

        // Bind views
        tvUserHeaderName = findViewById(R.id.tvUserHeaderName);
        tvRpgName = findViewById(R.id.tvRpgName);
        tvCompletedHabitsCount = findViewById(R.id.tvCompletedHabitsCount);
        tvRankLetter = findViewById(R.id.tvRankLetter);

        cpOverall = findViewById(R.id.cpOverall);
        tvOverallPercent = findViewById(R.id.tvOverallPercent);

        tvChipHabits = findViewById(R.id.tvChipHabits);
        tvChipToday = findViewById(R.id.tvChipToday);
        tvChipWeekly = findViewById(R.id.tvChipWeekly);

        dailyProgressBar = findViewById(R.id.dailyProgressBar);
        tvDailyPercent = findViewById(R.id.tvDailyPercent);
        tvDailyLabel = findViewById(R.id.tvDailyLabel);

        weeklyProgressBar = findViewById(R.id.weeklyProgressBar);
        tvWeeklyPercent = findViewById(R.id.tvWeeklyPercent);
        llWeeklyChart = findViewById(R.id.llWeeklyChart);

        llHabitsBreakdown = findViewById(R.id.llHabitsBreakdown);
        llHabitChecklist = findViewById(R.id.llHabitChecklist);

        ImageView btnBack = findViewById(R.id.btnBackStats);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadUserProfile();
        loadHabitStatistics();

        SystemEntranceAnim.applySystemEntranceAnimation(topBar, statusWindow, bottomNav);
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) return;
        String email = mAuth.getCurrentUser().getEmail();

        mDB.collection("Users").document(email).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            UserModel user = doc.toObject(UserModel.class);
            if (user == null) return;

            String name = user.getUsername() != null ? user.getUsername().toUpperCase() : "USER";
            int progVal = user.getOverallProgress();
            int level = (progVal / 20) + 1;

            if (tvRpgName != null) tvRpgName.setText("NAME: " + name);
            if (tvUserHeaderName != null) tvUserHeaderName.setText(name);
            if (tvRpgLv != null) tvRpgLv.setText("LV: " + level);
            if (tvLevelDisplay != null) tvLevelDisplay.setText("LVL : " + level);
            if (tvRankLetter != null) tvRankLetter.setText(user.getRank());
        });
    }

    private void loadHabitStatistics() {
        if (mAuth.getCurrentUser() == null) return;
        String email = mAuth.getCurrentUser().getEmail();

        mDB.collection("Users").document(email).collection("Habits")
                .get()
                .addOnSuccessListener(query -> {
                    habitList.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        try {
                            HabitModel h = doc.toObject(HabitModel.class);
                            if (h != null) habitList.add(h);
                        } catch (Exception e) {
                            Log.e("Statistics", "Error parsing habit", e);
                        }
                    }
                    renderAllStats();
                });
    }

    private void renderAllStats() {
        int totalHabits = habitList.size();
        int totalCompleted = 0;
        int totalDuration = 0;
        int completedToday = 0;

        for (HabitModel h : habitList) {
            totalCompleted += h.getCompletedDays();
            totalDuration += h.getDuration();
            if (h.isTodayCompleted()) completedToday++;
        }

        // Overall %
        int overallPercent = (totalDuration > 0) ? (int) ((totalCompleted * 100f) / totalDuration) : 0;

        // Daily %
        int dailyPercent = (totalHabits > 0) ? (completedToday * 100 / totalHabits) : 0;

        // Weekly avg (estimated: completedDays / daysSinceStart * 7, capped at 7, averaged)
        float weeklyDaysSum = 0;
        int weeklyCount = 0;
        for (HabitModel h : habitList) {
            if (h.getStartDate() == null) continue;
            try {
                LocalDate start = LocalDate.parse(h.getStartDate().split("T")[0]);
                long daysSince = Math.max(1, ChronoUnit.DAYS.between(start, LocalDate.now()) + 1);
                float daysPerWeek = Math.min((h.getCompletedDays() * 7f) / daysSince, 7f);
                weeklyDaysSum += daysPerWeek;
                weeklyCount++;
            } catch (Exception ignored) {}
        }
        float avgDaysPerWeek = (weeklyCount > 0) ? weeklyDaysSum / weeklyCount : 0;
        int weeklyPercent = (int) Math.min(avgDaysPerWeek / 7f * 100, 100);

        // --- Update Overall Ring ---
        if (cpOverall != null) cpOverall.setProgress(overallPercent, true);
        if (tvOverallPercent != null) tvOverallPercent.setText(overallPercent + "%");

        // --- Update Chips ---
        if (tvChipHabits != null) tvChipHabits.setText(String.valueOf(totalHabits));
        if (tvChipToday != null) tvChipToday.setText(dailyPercent + "%");
        if (tvChipWeekly != null) tvChipWeekly.setText(weeklyPercent + "%");

        // --- Update Active Habits Count ---
        if (tvCompletedHabitsCount != null) tvCompletedHabitsCount.setText("ACTIVE: " + totalHabits);

        // --- Daily Progress ---
        if (dailyProgressBar != null) dailyProgressBar.setProgress(dailyPercent, true);
        if (tvDailyPercent != null) tvDailyPercent.setText(dailyPercent + "%");
        if (tvDailyLabel != null) tvDailyLabel.setText(completedToday + " / " + totalHabits + " habits completed today");

        // --- Weekly Progress ---
        if (weeklyProgressBar != null) weeklyProgressBar.setProgress(weeklyPercent, true);
        if (tvWeeklyPercent != null) tvWeeklyPercent.setText(weeklyPercent + "%");

        // --- Weekly Bar Chart ---
        buildWeeklyChart(dailyPercent, weeklyPercent);

        // --- Habits Breakdown ---
        buildHabitsBreakdown();

        // --- Daily Checklist ---
        buildChecklist();

        // Persist overall progress to Firestore
        if (mAuth.getCurrentUser() != null) {
            mDB.collection("Users").document(mAuth.getCurrentUser().getEmail())
                    .update("overallProgress", overallPercent);
        }
    }

    private void buildWeeklyChart(int dailyPercent, int weeklyPercent) {
        if (llWeeklyChart == null) return;
        llWeeklyChart.removeAllViews();

        String[] labels = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        int todayIndex = LocalDate.now().getDayOfWeek().getValue() - 1; // 0=Mon, 6=Sun

        float density = getResources().getDisplayMetrics().density;
        int maxBarPx = (int) (72 * density);

        for (int i = 0; i < 7; i++) {
            LinearLayout column = new LinearLayout(this);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            colParams.setMargins((int)(3 * density), 0, (int)(3 * density), 0);
            column.setLayoutParams(colParams);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);

            // Bar container
            FrameLayout barContainer = new FrameLayout(this);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, maxBarPx);
            barContainer.setLayoutParams(containerParams);

            // Background (track)
            View bgView = new View(this);
            bgView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            bgView.setBackgroundColor(Color.parseColor("#1F2937"));
            barContainer.addView(bgView);

            // Only fill today's bar with actual data — no estimates for other days
            if (i == todayIndex && dailyPercent > 0) {
                View fillView = new View(this);
                int fillHeight = Math.max((int)(maxBarPx * (dailyPercent / 100f)), (int)(4 * density));
                FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, fillHeight);
                fillParams.gravity = Gravity.BOTTOM;
                fillView.setLayoutParams(fillParams);
                fillView.setBackgroundColor(Color.parseColor("#3B82F6"));
                barContainer.addView(fillView);
            }

            column.addView(barContainer);

            // Day label
            TextView dayLabel = new TextView(this);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.topMargin = (int)(5 * density);
            dayLabel.setLayoutParams(labelParams);
            dayLabel.setText(labels[i]);
            dayLabel.setTextSize(9);
            if (i == todayIndex) {
                dayLabel.setTextColor(Color.parseColor("#3B82F6"));
                dayLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                dayLabel.setTextColor(Color.parseColor("#4B5563"));
            }
            column.addView(dayLabel);

            llWeeklyChart.addView(column);
        }
    }

    private void buildHabitsBreakdown() {
        if (llHabitsBreakdown == null) return;
        llHabitsBreakdown.removeAllViews();

        if (habitList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No habits tracked yet.");
            empty.setTextColor(Color.parseColor("#6B7280"));
            empty.setTextSize(13);
            llHabitsBreakdown.addView(empty);
            return;
        }

        for (HabitModel habit : habitList) {
            View card = LayoutInflater.from(this).inflate(R.layout.item_stat_habit, llHabitsBreakdown, false);

            TextView tvName = card.findViewById(R.id.tvStatHabitName);
            TextView tvPercent = card.findViewById(R.id.tvStatPercent);
            LinearProgressIndicator bar = card.findViewById(R.id.statProgressBar);
            TextView tvDuration = card.findViewById(R.id.tvStatDuration);
            View btnReset = card.findViewById(R.id.btnStatReset);

            int progress = (habit.getDuration() > 0)
                    ? (int) ((habit.getCompletedDays() * 100f) / habit.getDuration())
                    : 0;

            tvName.setText(habit.getHabitName());
            tvPercent.setText(progress + "%");
            bar.setProgress(progress, true);
            tvDuration.setText(habit.getDuration() + " Minutes");

            btnReset.setOnClickListener(v -> confirmResetHabit(habit));

            llHabitsBreakdown.addView(card);
        }
    }

    private void buildChecklist() {
        if (llHabitChecklist == null) return;
        llHabitChecklist.removeAllViews();

        if (habitList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No habits to check off.");
            empty.setTextColor(Color.parseColor("#6B7280"));
            empty.setTextSize(13);
            llHabitChecklist.addView(empty);
            return;
        }

        for (HabitModel habit : habitList) {
            View row = LayoutInflater.from(this).inflate(R.layout.item_stat_checklist, llHabitChecklist, false);

            View dot = row.findViewById(R.id.viewCheckDot);
            TextView tvName = row.findViewById(R.id.tvCheckHabitName);
            TextView tvStatus = row.findViewById(R.id.tvCheckStatus);

            tvName.setText(habit.getHabitName());
            updateChecklistRowVisuals(dot, tvStatus, habit.isTodayCompleted());

            row.setOnClickListener(v -> {
                boolean newState = !habit.isTodayCompleted();
                toggleHabitCompletion(habit, newState, dot, tvStatus);
            });

            llHabitChecklist.addView(row);
        }
    }

    private void updateChecklistRowVisuals(View dot, TextView tvStatus, boolean completed) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(completed ? Color.parseColor("#4ADE80") : Color.parseColor("#374151"));
        dot.setBackground(circle);

        tvStatus.setText(completed ? "DONE" : "PENDING");
        tvStatus.setTextColor(completed ? Color.parseColor("#4ADE80") : Color.parseColor("#4B5563"));
    }

    private void toggleHabitCompletion(HabitModel habit, boolean newState, View dot, TextView tvStatus) {
        if (mAuth.getCurrentUser() == null || habit.getHabitName() == null) return;
        String email = mAuth.getCurrentUser().getEmail();

        mDB.collection("Users").document(email).collection("Habits")
                .document(habit.getHabitName())
                .update("todayCompleted", newState)
                .addOnSuccessListener(aVoid -> {
                    habit.setTodayCompleted(newState);
                    updateChecklistRowVisuals(dot, tvStatus, newState);
                    // Refresh stats without rebuilding everything
                    refreshProgressStats();
                });
    }

    private void refreshProgressStats() {
        int totalHabits = habitList.size();
        int totalCompleted = 0, totalDuration = 0, completedToday = 0;
        for (HabitModel h : habitList) {
            totalCompleted += h.getCompletedDays();
            totalDuration += h.getDuration();
            if (h.isTodayCompleted()) completedToday++;
        }

        int overallPercent = (totalDuration > 0) ? (int) ((totalCompleted * 100f) / totalDuration) : 0;
        int dailyPercent = (totalHabits > 0) ? (completedToday * 100 / totalHabits) : 0;

        if (cpOverall != null) cpOverall.setProgress(overallPercent, true);
        if (tvOverallPercent != null) tvOverallPercent.setText(overallPercent + "%");
        if (tvChipToday != null) tvChipToday.setText(dailyPercent + "%");
        if (dailyProgressBar != null) dailyProgressBar.setProgress(dailyPercent, true);
        if (tvDailyPercent != null) tvDailyPercent.setText(dailyPercent + "%");
        if (tvDailyLabel != null) tvDailyLabel.setText(completedToday + " / " + totalHabits + " habits completed today");
    }

    private void confirmResetHabit(HabitModel habit) {
        if (mAuth.getCurrentUser() == null || habit.getHabitName() == null) return;
        new AlertDialog.Builder(this)
                .setMessage("Reset '" + habit.getHabitName() + "' progress?")
                .setPositiveButton("Reset", (d, w) ->
                        mDB.collection("Users").document(mAuth.getCurrentUser().getEmail())
                                .collection("Habits").document(habit.getHabitName())
                                .update("completedDays", 0, "startDate", LocalDate.now().toString(), "todayCompleted", false)
                                .addOnSuccessListener(aVoid -> loadHabitStatistics()))
                .setNegativeButton("Cancel", null)
                .show();
    }
}
