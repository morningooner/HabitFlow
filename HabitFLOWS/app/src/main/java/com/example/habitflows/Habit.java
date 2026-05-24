package com.example.habitflows;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Habit extends AppCompatActivity {

    private FirebaseFirestore mDB;
    private FirebaseAuth mAuth;

    private TextView tvHabitName, tvHabitDuration, tvHabitPercentage;
    private LinearProgressIndicator habitProgressIndicator;
    private EditText etHabitName, etDuration;
    private Button btnDeleteHabit;
    private String currentHabitName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_habit);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();

        // Display views
        tvHabitName = findViewById(R.id.tvHabitName);
        tvHabitDuration = findViewById(R.id.tvHabitDuration);
        tvHabitPercentage = findViewById(R.id.tvHabitPercentage);
        habitProgressIndicator = findViewById(R.id.habitProgressIndicator);
        btnDeleteHabit = findViewById(R.id.btnDeleteHabit);

        // Input views
        etHabitName = findViewById(R.id.etHabitName);
        etDuration = findViewById(R.id.etDuration);
        
        ImageView btnBackProfile = findViewById(R.id.btnBackProfile);
        Button btnAddHabit = findViewById(R.id.btnAddHabit);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Load existing habit data
        loadHabitData();

        btnBackProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Habit.this, MainMenu.class);
            startActivity(intent);
            finish();
        });

        btnAddHabit.setOnClickListener(v -> {
            String habitName = etHabitName.getText().toString().trim();
            String durationText = etDuration.getText().toString().trim();

            if (habitName.isEmpty() || durationText.isEmpty()) {
                Toast.makeText(Habit.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            int duration = Integer.parseInt(durationText);
            HabitModel habitData = new HabitModel(habitName, duration, "Days");

            mDB.collection("Users")
                    .document(mAuth.getCurrentUser().getEmail())
                    .collection("Habits")
                    .document(habitName)
                    .set(habitData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(Habit.this, "Habit saved!", Toast.LENGTH_SHORT).show();
                        etHabitName.setText("");
                        etDuration.setText("");
                        loadHabitData(); // Refresh the display
                    })
                    .addOnFailureListener(e -> Toast.makeText(Habit.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        btnDeleteHabit.setOnClickListener(v -> {
            if (!currentHabitName.isEmpty()) {
                mDB.collection("Users")
                        .document(mAuth.getCurrentUser().getEmail())
                        .collection("Habits")
                        .document(currentHabitName)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(Habit.this, "Habit deleted", Toast.LENGTH_SHORT).show();
                            loadHabitData(); // Refresh UI
                        })
                        .addOnFailureListener(e -> Toast.makeText(Habit.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadHabitData() {
        if (mAuth.getCurrentUser() == null) return;

        mDB.collection("Users")
                .document(mAuth.getCurrentUser().getEmail())
                .collection("Habits")
                .limit(1) // Just getting the first habit for display
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            HabitModel habit = document.toObject(HabitModel.class);
                            currentHabitName = habit.getHabitName();
                            updateUI(habit);
                        }
                        btnDeleteHabit.setVisibility(View.VISIBLE);
                    } else {
                        // Reset UI if no habit yet
                        currentHabitName = "";
                        tvHabitName.setText("No habit active");
                        tvHabitDuration.setText("Start a new journey below");
                        habitProgressIndicator.setProgress(0);
                        tvHabitPercentage.setText("0%");
                        btnDeleteHabit.setVisibility(View.GONE);
                    }
                });
    }

    private void updateUI(HabitModel habit) {
        tvHabitName.setText(habit.getHabitName());
        tvHabitDuration.setText(habit.getDuration() + " " + habit.getUnit() + " Goal");

        // Calculate progress
        if (habit.getStartDate() != null) {
            LocalDate start = LocalDate.parse(habit.getStartDate());
            LocalDate today = LocalDate.now();
            long daysPassed = ChronoUnit.DAYS.between(start, today);
            
            // Ensure progress is between 0 and duration
            int progress = (int) Math.min(Math.max(daysPassed, 0), habit.getDuration());
            float percentageValue = (progress / (float) habit.getDuration()) * 100;
            int percentage = (int) percentageValue;

            habitProgressIndicator.setProgress(percentage);
            tvHabitPercentage.setText(percentage + "%");
        }
    }
}
