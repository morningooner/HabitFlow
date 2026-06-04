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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Statistics extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;

    private ImageView btnBackStats;
    private CircularProgressIndicator cpOverall;
    private TextView tvOverallPercent;
    private MaterialButton btnSelectHabit;

    private MaterialCardView cvHabitDetail;
    private TextView tvDetailName, tvDetailDuration, tvDetailStart, tvDetailEnd, tvDetailPercentage;
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

        // Initialize UI components
        btnBackStats = findViewById(R.id.btnBackStats);
        cpOverall = findViewById(R.id.cpOverall);
        tvOverallPercent = findViewById(R.id.tvOverallPercent);
        btnSelectHabit = findViewById(R.id.btnSelectHabit);

        cvHabitDetail = findViewById(R.id.cvHabitDetail);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailDuration = findViewById(R.id.tvDetailDuration);
        tvDetailStart = findViewById(R.id.tvDetailStart);
        tvDetailEnd = findViewById(R.id.tvDetailEnd);
        tvDetailPercentage = findViewById(R.id.tvDetailPercentage);
        detailProgressBar = findViewById(R.id.detailProgressBar);
        btnResetHabit = findViewById(R.id.btnResetHabit);
        llHabitChecklist = findViewById(R.id.llHabitChecklist);

        btnBackStats.setOnClickListener(v -> finish());
        btnSelectHabit.setOnClickListener(v -> showHabitSelectionDialog());
        
        btnResetHabit.setOnClickListener(v -> {
            if (selectedHabit != null) {
                resetHabitProgress(selectedHabit);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadHabitStatistics();
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

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        HabitModel habit = doc.toObject(HabitModel.class);
                        if (habit != null) {
                            habitList.add(habit);
                            totalCompleted += habit.getCompletedDays();
                            totalDuration += habit.getDuration();
                        }
                    }

                    // Recalculate and Save Overall Progress to Database
                    updateOverallUIAndDatabase(totalCompleted, totalDuration);
                    populateHabitChecklist();

                    if (!habitList.isEmpty()) {
                        habitNames = new String[habitList.size()];
                        for (int i = 0; i < habitList.size(); i++) {
                            habitNames[i] = habitList.get(i).getHabitName();
                        }
                        btnSelectHabit.setEnabled(true);
                        
                        // If a habit is selected, refresh its details from the new data
                        if (selectedHabit != null) {
                            for (HabitModel h : habitList) {
                                if (h.getHabitName().equals(selectedHabit.getHabitName())) {
                                    displayHabitDetails(h);
                                    break;
                                }
                            }
                        }
                    } else {
                        btnSelectHabit.setEnabled(false);
                        cvHabitDetail.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load statistics", Toast.LENGTH_SHORT).show());
    }

    private void populateHabitChecklist() {
        llHabitChecklist.removeAllViews();
        if (habitList.isEmpty()) return;

        for (HabitModel habit : habitList) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(habit.getHabitName() + (habit.isTodayCompleted() ? " - Complete" : " - Not Complete"));
            checkBox.setTextColor(getResources().getColor(R.color.whiteText, getTheme()));
            checkBox.setChecked(habit.isTodayCompleted());
            checkBox.setTextSize(16);
            checkBox.setPadding(10, 10, 10, 10);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateHabitChecklistStatus(habit, isChecked, checkBox);
            });

            llHabitChecklist.addView(checkBox);
        }
    }

    private void updateHabitChecklistStatus(HabitModel habit, boolean isChecked, CheckBox checkBox) {
        if (mAuth.getCurrentUser() == null) return;
        String userEmail = mAuth.getCurrentUser().getEmail();

        mDB.collection("Users").document(userEmail).collection("Habits")
                .document(habit.getHabitName())
                .update("todayCompleted", isChecked)
                .addOnSuccessListener(aVoid -> {
                    habit.setTodayCompleted(isChecked);
                    checkBox.setText(habit.getHabitName() + (isChecked ? " - Complete" : " - Not Complete"));
                    Toast.makeText(this, "Status updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    checkBox.setChecked(!isChecked); // Revert UI
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateOverallUIAndDatabase(int totalCompleted, int totalDuration) {
        int avgProgress = (totalDuration > 0) ? (int) (((float) totalCompleted / totalDuration) * 100) : 0;
        
        // Update Circle Progress on UI
        cpOverall.setProgress(avgProgress, true);
        tvOverallPercent.setText(avgProgress + "%");

        // Save updated average to the user document
        if (mAuth.getCurrentUser() != null) {
            String userEmail = mAuth.getCurrentUser().getEmail();
            mDB.collection("Users").document(userEmail)
                    .update("overallProgress", avgProgress)
                    .addOnFailureListener(e -> Log.e("STATS", "Failed to sync overall progress", e));
        }
    }

    private void showHabitSelectionDialog() {
        if (habitNames == null || habitNames.length == 0) return;
        new AlertDialog.Builder(this)
                .setTitle("Select a Habit")
                .setItems(habitNames, (dialog, which) -> displayHabitDetails(habitList.get(which)))
                .show();
    }

    private void displayHabitDetails(HabitModel habit) {
        selectedHabit = habit;
        cvHabitDetail.setVisibility(View.VISIBLE);
        tvDetailName.setText(habit.getHabitName());
        tvDetailDuration.setText(habit.getDuration() + " Days");
        tvDetailStart.setText(habit.getStartDate());

        int habitProgress = (habit.getDuration() > 0)
                ? (int) (((float) habit.getCompletedDays() / habit.getDuration()) * 100)
                : 0;

        detailProgressBar.setProgress(habitProgress, true);
        tvDetailPercentage.setText(habitProgress + "%");

        try {
            LocalDate startDate = LocalDate.parse(habit.getStartDate());
            LocalDate endDate = startDate.plusDays(habit.getDuration());
            tvDetailEnd.setText(endDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } catch (Exception e) {
            tvDetailEnd.setText("N/A");
        }
    }

    private void resetHabitProgress(HabitModel habit) {
        new AlertDialog.Builder(this)
                .setTitle("Reset Progress")
                .setMessage("Are you sure you want to reset '" + habit.getHabitName() + "'? This will restart the goal from today.")
                .setPositiveButton("Reset", (dialog, which) -> {
                    String userEmail = mAuth.getCurrentUser().getEmail();
                    String today = LocalDate.now().toString();
                    
                    mDB.collection("Users").document(userEmail).collection("Habits")
                            .document(habit.getHabitName())
                            .update("completedDays", 0, "startDate", today, "todayCompleted", false)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Habit restarted!", Toast.LENGTH_SHORT).show();
                                // Reload logic triggers recalculation of overallProgress and saves it
                                loadHabitStatistics();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Reset failed", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}