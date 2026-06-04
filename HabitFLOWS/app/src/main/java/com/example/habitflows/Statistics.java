package com.example.habitflows;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Statistics extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;

    // Header & Overall Stats
    private ImageView btnBackStats;
    private CircularProgressIndicator cpOverall;
    private TextView tvOverallPercent, tvUserHeaderName, tvLevelDisplay;

    // RPG Status Window
    private TextView tvRpgName, tvRpgLv, tvCompletedHabitsCount, tvRankLetter;

    // Habit Selection & Details
    private MaterialButton btnSelectHabit;
    private MaterialCardView cvHabitDetail;
    private TextView tvDetailName, tvDetailStart, tvDetailEnd, tvDetailPercentage;
    private LinearProgressIndicator detailProgressBar;
    private MaterialButton btnResetHabit;
    private LinearLayout llHabitChecklist;

    private List<HabitModel> habitList = new ArrayList<>();
    private String[] habitNames;
    private HabitModel selectedHabit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statistics);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();

        // Bind Views
        btnBackStats = findViewById(R.id.btnBackStats);
        cpOverall = findViewById(R.id.cpOverall);
        tvOverallPercent = findViewById(R.id.tvOverallPercent);
        tvUserHeaderName = findViewById(R.id.tvUserHeaderName);
        tvLevelDisplay = findViewById(R.id.tvLevelDisplay);

        tvRpgName = findViewById(R.id.tvRpgName);
        tvRpgLv = findViewById(R.id.tvRpgLv);
        tvCompletedHabitsCount = findViewById(R.id.tvCompletedHabitsCount);
        tvRankLetter = findViewById(R.id.tvRankLetter);

        btnSelectHabit = findViewById(R.id.btnSelectHabit);
        cvHabitDetail = findViewById(R.id.cvHabitDetail);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailStart = findViewById(R.id.tvDetailStart);
        tvDetailEnd = findViewById(R.id.tvDetailEnd);
        tvDetailPercentage = findViewById(R.id.tvDetailPercentage);
        detailProgressBar = findViewById(R.id.detailProgressBar);
        btnResetHabit = findViewById(R.id.btnResetHabit);
        llHabitChecklist = findViewById(R.id.llHabitChecklist);

        if (btnBackStats != null) {
            btnBackStats.setOnClickListener(v -> finish());
        }
        
        if (btnSelectHabit != null) {
            btnSelectHabit.setOnClickListener(v -> showHabitSelectionDialog());
        }
        
        if (btnResetHabit != null) {
            btnResetHabit.setOnClickListener(v -> {
                if (selectedHabit != null) resetHabitProgress(selectedHabit);
            });
        }

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        loadUserProfile();
        loadHabitStatistics();
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) return;
        String email = mAuth.getCurrentUser().getEmail();

        mDB.collection("Users").document(email).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("username");
                Long progress = doc.getLong("overallProgress");
                int progVal = (progress != null) ? progress.intValue() : 0;

                if (tvRpgName != null) tvRpgName.setText("NAME: " + (name != null ? name.toUpperCase() : "USER"));
                if (tvUserHeaderName != null) tvUserHeaderName.setText(name != null ? name.toUpperCase() : "USER");
                
                int level = (progVal / 20) + 1;
                if (tvRpgLv != null) tvRpgLv.setText("LV: " + level);
                if (tvLevelDisplay != null) tvLevelDisplay.setText("LVL : " + level);

                String rank = "E";
                if (progVal >= 90) rank = "S";
                else if (progVal >= 75) rank = "A";
                else if (progVal >= 50) rank = "B";
                else if (progVal >= 30) rank = "C";
                else if (progVal >= 15) rank = "D";
                if (tvRankLetter != null) tvRankLetter.setText(rank);
            }
        });
    }

    private void loadHabitStatistics() {
        if (mAuth.getCurrentUser() == null) return;
        String userEmail = mAuth.getCurrentUser().getEmail();

        mDB.collection("Users").document(userEmail).collection("Habits")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    habitList.clear();
                    int totalCompleted = 0;
                    int totalDuration = 0;
                    int activeHabits = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            HabitModel habit = doc.toObject(HabitModel.class);
                            if (habit != null) {
                                habitList.add(habit);
                                totalCompleted += habit.getCompletedDays();
                                totalDuration += habit.getDuration();
                                activeHabits++;
                            }
                        } catch (Exception e) {
                            Log.e("Statistics", "Error parsing habit: " + e.getMessage());
                        }
                    }

                    if (tvCompletedHabitsCount != null) tvCompletedHabitsCount.setText("ACTIVE: " + activeHabits);
                    updateOverallUIAndDatabase(totalCompleted, totalDuration);
                    populateHabitChecklist();

                    if (!habitList.isEmpty()) {
                        habitNames = new String[habitList.size()];
                        for (int i = 0; i < habitList.size(); i++) {
                            String name = habitList.get(i).getHabitName();
                            habitNames[i] = (name != null) ? name : "Unnamed Habit";
                        }
                        if (btnSelectHabit != null) btnSelectHabit.setEnabled(true);
                        
                        if (selectedHabit != null) {
                            for (HabitModel h : habitList) {
                                if (h.getHabitName() != null && h.getHabitName().equals(selectedHabit.getHabitName())) {
                                    displayHabitDetails(h);
                                    break;
                                }
                            }
                        }
                    } else {
                        if (btnSelectHabit != null) btnSelectHabit.setEnabled(false);
                        if (cvHabitDetail != null) cvHabitDetail.setVisibility(View.GONE);
                    }
                });
    }

    private void populateHabitChecklist() {
        if (llHabitChecklist == null) return;
        llHabitChecklist.removeAllViews();
        for (HabitModel habit : habitList) {
            CheckBox checkBox = new CheckBox(this);
            String label = habit.getHabitName() != null ? habit.getHabitName() : "Habit";
            checkBox.setText(label + (habit.isTodayCompleted() ? " ✔" : ""));
            checkBox.setTextColor(getResources().getColor(R.color.whiteText, getTheme()));
            checkBox.setChecked(habit.isTodayCompleted());
            checkBox.setOnCheckedChangeListener((v, isChecked) -> updateHabitChecklistStatus(habit, isChecked, checkBox));
            llHabitChecklist.addView(checkBox);
        }
    }

    private void updateHabitChecklistStatus(HabitModel habit, boolean isChecked, CheckBox checkBox) {
        if (mAuth.getCurrentUser() == null || habit.getHabitName() == null) return;
        String userEmail = mAuth.getCurrentUser().getEmail();
        mDB.collection("Users").document(userEmail).collection("Habits")
                .document(habit.getHabitName())
                .update("todayCompleted", isChecked)
                .addOnSuccessListener(aVoid -> {
                    habit.setTodayCompleted(isChecked);
                    String label = habit.getHabitName() != null ? habit.getHabitName() : "Habit";
                    checkBox.setText(label + (isChecked ? " ✔" : ""));
                    loadHabitStatistics();
                });
    }

    private void updateOverallUIAndDatabase(int totalCompleted, int totalDuration) {
        int avgProgress = (totalDuration > 0) ? (int) (((float) totalCompleted / totalDuration) * 100) : 0;
        if (cpOverall != null) cpOverall.setProgress(avgProgress, true);
        if (tvOverallPercent != null) tvOverallPercent.setText(avgProgress + "%");

        if (mAuth.getCurrentUser() != null) {
            mDB.collection("Users").document(mAuth.getCurrentUser().getEmail())
                    .update("overallProgress", avgProgress);
        }
    }

    private void showHabitSelectionDialog() {
        if (habitNames == null || habitNames.length == 0) {
            Toast.makeText(this, "No habits found", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Select Habit to Analyze")
                .setItems(habitNames, (d, which) -> {
                    if (which >= 0 && which < habitList.size()) {
                        displayHabitDetails(habitList.get(which));
                    }
                })
                .show();
    }

    private void displayHabitDetails(HabitModel habit) {
        if (habit == null) return;
        selectedHabit = habit;
        
        if (cvHabitDetail != null) cvHabitDetail.setVisibility(View.VISIBLE);
        if (tvDetailName != null) tvDetailName.setText(habit.getHabitName());
        
        String startDateStr = habit.getStartDate();
        if (tvDetailStart != null) tvDetailStart.setText(startDateStr != null ? startDateStr : "N/A");

        int progress = (habit.getDuration() > 0) ? (int) (((float) habit.getCompletedDays() / habit.getDuration()) * 100) : 0;
        if (detailProgressBar != null) detailProgressBar.setProgress(progress, true);
        if (tvDetailPercentage != null) tvDetailPercentage.setText(progress + "% ACHIEVED");

        if (tvDetailEnd != null) {
            if (startDateStr != null && !startDateStr.isEmpty()) {
                try {
                    // Handle ISO format date string
                    String cleanDate = startDateStr.split("T")[0]; // Take only the date part if time is present
                    LocalDate date = LocalDate.parse(cleanDate);
                    tvDetailEnd.setText(date.plusDays(habit.getDuration()).toString());
                } catch (Exception e) {
                    tvDetailEnd.setText("N/A");
                }
            } else {
                tvDetailEnd.setText("N/A");
            }
        }
    }

    private void resetHabitProgress(HabitModel habit) {
        if (mAuth.getCurrentUser() == null || habit == null || habit.getHabitName() == null) return;
        new AlertDialog.Builder(this)
                .setMessage("Reset '" + habit.getHabitName() + "' progress?")
                .setPositiveButton("Reset", (d, w) -> {
                    mDB.collection("Users").document(mAuth.getCurrentUser().getEmail())
                            .collection("Habits").document(habit.getHabitName())
                            .update("completedDays", 0, "startDate", LocalDate.now().toString(), "todayCompleted", false)
                            .addOnSuccessListener(aVoid -> loadHabitStatistics());
                }).setNegativeButton("Cancel", null).show();
    }
}