package com.example.habitflows;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
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
    private TextView tvDetailName, tvDetailDuration, tvDetailStart, tvDetailEnd;

    private List<HabitModel> habitList = new ArrayList<>();
    private String[] habitNames;

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

        // Handle Back Button
        btnBackStats.setOnClickListener(v -> finish());

        // Handle Habit Selection
        btnSelectHabit.setOnClickListener(v -> showHabitSelectionDialog());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadHabitStatistics();
    }

    private void loadHabitStatistics() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        mDB.collection("Users").document(userId).collection("Habits")
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
                            
                            // Log progress for debugging
                            float progress = (habit.getDuration() > 0) ? 
                                    ((float) habit.getCompletedDays() / habit.getDuration()) * 100 : 0;
                            Log.d("STATS", habit.getHabitName() + " : " + progress + "%");
                        }
                    }

                    if (!habitList.isEmpty()) {
                        // Calculate overall progress for the circle
                        int avgProgress = (totalDuration > 0) ? 
                                (int) (((float) totalCompleted / totalDuration) * 100) : 0;
                        
                        cpOverall.setProgress(avgProgress, true);
                        tvOverallPercent.setText(avgProgress + "%");

                        // Prepare names for the selection dialog
                        habitNames = new String[habitList.size()];
                        for (int i = 0; i < habitList.size(); i++) {
                            habitNames[i] = habitList.get(i).getHabitName();
                        }
                        btnSelectHabit.setEnabled(true);
                        btnSelectHabit.setText("Choose Habit to View");
                    } else {
                        cpOverall.setProgress(0, true);
                        tvOverallPercent.setText("0%");
                        btnSelectHabit.setEnabled(false);
                        btnSelectHabit.setText("No Habits Found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("STATS", "Error loading habits", e);
                    Toast.makeText(this, "Failed to load habits", Toast.LENGTH_SHORT).show();
                });
    }

    private void showHabitSelectionDialog() {
        if (habitNames == null || habitNames.length == 0) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Habit");
        builder.setItems(habitNames, (dialog, which) -> {
            displayHabitDetails(habitList.get(which));
        });
        builder.show();
    }

    private void displayHabitDetails(HabitModel habit) {
        cvHabitDetail.setVisibility(View.VISIBLE);
        tvDetailName.setText(habit.getHabitName());
        tvDetailDuration.setText(habit.getDuration() + " Days");
        
        // Display Start Date
        String startStr = habit.getStartDate();
        tvDetailStart.setText(startStr);

        // Calculate and Display End Date using Duration
        try {
            LocalDate startDate = LocalDate.parse(startStr);
            LocalDate endDate = startDate.plusDays(habit.getDuration());
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            tvDetailEnd.setText(endDate.format(formatter));
        } catch (Exception e) {
            Log.e("STATS", "Error parsing date", e);
            tvDetailEnd.setText("N/A");
        }
    }
}
