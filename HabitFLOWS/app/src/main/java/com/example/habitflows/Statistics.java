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
import com.google.firebase.firestore.ListenerRegistration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class Statistics extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;

    // Top bar
    private TextView tvUserHeaderName;

    // Identity row
    private TextView tvRpgName, tvCompletedHabitsCount, tvRankLetter;

    private ListenerRegistration habitsListener;

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
            if (tvRankLetter != null) tvRankLetter.setText(user.getRank());
        });
    }

    private void loadHabitStatistics() {
        if (mAuth.getCurrentUser() == null) return;
        String email = mAuth.getCurrentUser().getEmail();

        if (habitsListener != null) habitsListener.remove();

        habitsListener = mDB.collection("Users").document(email).collection("Habits")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    habitList.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        try {
                            HabitModel h = doc.toObject(HabitModel.class);
                            if (h != null) habitList.add(h);
                        } catch (Exception ex) {
                            Log.e("Statistics", "Error parsing habit", ex);
                        }
                    }
                    renderAllStats();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (habitsListener != null) habitsListener.remove();
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

        // Weekly: count distinct days this week (Mon–Sun) where at least one habit was completed
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        boolean[] weekDaysCompleted = new boolean[7];
        for (HabitModel h : habitList) {
            for (String dateStr : h.getCompletedDates()) {
                try {
                    LocalDate d = LocalDate.parse(dateStr);
                    long offset = ChronoUnit.DAYS.between(monday, d);
                    if (offset >= 0 && offset < 7) weekDaysCompleted[(int) offset] = true;
                } catch (Exception ignored) {}
            }
        }
        int weeklyDaysCount = 0;
        for (boolean b : weekDaysCompleted) if (b) weeklyDaysCount++;
        int weeklyPercent = (int) (weeklyDaysCount / 7.0 * 100);

        // --- Update Overall Ring ---
        if (cpOverall != null) cpOverall.setProgress(overallPercent, true);
        if (tvOverallPercent != null) tvOverallPercent.setText(overallPercent + "%");

        // --- Update Chips ---
        if (tvChipHabits != null) tvChipHabits.setText(String.valueOf(totalHabits));
        if (tvChipToday != null) tvChipToday.setText(dailyPercent + "%");
        if (tvChipWeekly != null) tvChipWeekly.setText(weeklyPercent + "%");

        // --- Weekly label (X / 7 days) ---
        if (tvWeeklyPercent != null) tvWeeklyPercent.setText(weeklyDaysCount + " / 7 days");

        // --- Update Active Habits Count ---
        if (tvCompletedHabitsCount != null) tvCompletedHabitsCount.setText("ACTIVE: " + totalHabits);

        // --- Daily Progress ---
        if (dailyProgressBar != null) dailyProgressBar.setProgress(dailyPercent, true);
        if (tvDailyPercent != null) tvDailyPercent.setText(dailyPercent + "%");
        if (tvDailyLabel != null) tvDailyLabel.setText(completedToday + " / " + totalHabits + " habits completed today");

        // --- Weekly Progress ---
        if (weeklyProgressBar != null) weeklyProgressBar.setProgress(weeklyPercent, true);

        // --- Weekly Bar Chart ---
        buildWeeklyChart(weekDaysCompleted);

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

    private void buildWeeklyChart(boolean[] weekDaysCompleted) {
        if (llWeeklyChart == null) return;
        llWeeklyChart.removeAllViews();

        String[] labels = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};
        int todayIndex = LocalDate.now().getDayOfWeek().getValue() - 1; // 0=Mon, 6=Sun

        float density = getResources().getDisplayMetrics().density;
        int maxBarPx = (int) (72 * density);

        for (int i = 0; i < 7; i++) {
            boolean completed = weekDaysCompleted[i];
            boolean isToday = (i == todayIndex);
            boolean isFuture = (i > todayIndex);

            LinearLayout column = new LinearLayout(this);
            LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            colParams.setMargins((int)(3 * density), 0, (int)(3 * density), 0);
            column.setLayoutParams(colParams);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);

            // Bar container
            FrameLayout barContainer = new FrameLayout(this);
            barContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, maxBarPx));

            // Background track
            View bgView = new View(this);
            bgView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            bgView.setBackgroundColor(Color.parseColor(isFuture ? "#111827" : "#1F2937"));
            barContainer.addView(bgView);

            // Fill — only when day was actually completed
            if (completed) {
                View fillView = new View(this);
                FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, maxBarPx);
                fillParams.gravity = Gravity.BOTTOM;
                fillView.setLayoutParams(fillParams);
                // Today = bright blue, past days = slightly muted blue
                fillView.setBackgroundColor(Color.parseColor(isToday ? "#3B82F6" : "#1D4ED8"));
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
            if (isToday) {
                dayLabel.setTextColor(Color.parseColor("#3B82F6"));
                dayLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            } else if (isFuture) {
                dayLabel.setTextColor(Color.parseColor("#374151"));
            } else {
                dayLabel.setTextColor(Color.parseColor("#6B7280"));
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
            tvDuration.setText(habit.getCompletedDays() + " / " + habit.getDuration() + " " + habit.getUnit());

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

            // Apply visuals directly here
            boolean completed = habit.isTodayCompleted();
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(completed ? Color.parseColor("#4ADE80") : Color.parseColor("#374151"));
            dot.setBackground(circle);
            tvStatus.setText(completed ? "DONE" : "PENDING");
            tvStatus.setTextColor(completed ? Color.parseColor("#4ADE80") : Color.parseColor("#4B5563"));

            // Read-only — checklist auto-updates via Firestore real-time listener

            llHabitChecklist.addView(row);
        }
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
