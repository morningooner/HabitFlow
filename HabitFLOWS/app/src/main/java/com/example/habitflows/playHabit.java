package com.example.habitflows;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class playHabit extends AppCompatActivity {

    private ImageView btnBackPlay;
    private TextView tvHabitPlayName, tvTimer;
    private MaterialButton btnSelectHabitPlay, btnResetTimer, btnPlayPause;
    private CircularProgressIndicator progressTimer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;

    private List<HabitModel> habitList = new ArrayList<>();
    private String[] habitNames;

    // Timer logic variables
    private long mStartTimeInMillis = 1500000; // Default 25 minutes
    private CountDownTimer countDownTimer;
    private boolean timerRunning;
    private long timeLeftInMillis;
    private long endTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_play_habit);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();

        btnBackPlay = findViewById(R.id.btnBackPlay);
        tvHabitPlayName = findViewById(R.id.tvHabitPlayName);
        tvTimer = findViewById(R.id.tvTimer);
        btnSelectHabitPlay = findViewById(R.id.btnSelectHabitPlay);
        btnResetTimer = findViewById(R.id.btnResetTimer);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        progressTimer = findViewById(R.id.progressTimer);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBackPlay.setOnClickListener(v -> finish());
        btnSelectHabitPlay.setOnClickListener(v -> showHabitSelectionDialog());
        btnPlayPause.setOnClickListener(v -> {
            if (timerRunning) pauseTimer();
            else startTimer();
        });
        btnResetTimer.setOnClickListener(v -> resetTimer());

        loadHabitsFromFirestore();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = getSharedPreferences("timer_prefs", MODE_PRIVATE);
        
        mStartTimeInMillis = prefs.getLong("startTimeInMillis", 1500000);
        timeLeftInMillis = prefs.getLong("millisLeft", mStartTimeInMillis);
        timerRunning = prefs.getBoolean("timerRunning", false);
        String savedHabit = prefs.getString("selectedHabitName", "Focus Session");
        tvHabitPlayName.setText(savedHabit);

        updateCountDownText();
        updateProgressBar();

        if (timerRunning) {
            endTime = prefs.getLong("endTime", 0);
            timeLeftInMillis = endTime - System.currentTimeMillis();

            if (timeLeftInMillis < 0) {
                timeLeftInMillis = 0;
                timerRunning = false;
                updateCountDownText();
                updateProgressBar();
                
                // If it finished while we were away, record progress
                if (!savedHabit.equals("Focus Session") && !savedHabit.equals("Choose Habit")) {
                    saveHabitProgress(savedHabit);
                }
                clearTimerPrefs();
            } else {
                startTimer();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences prefs = getSharedPreferences("timer_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putLong("startTimeInMillis", mStartTimeInMillis);
        editor.putLong("millisLeft", timeLeftInMillis);
        editor.putBoolean("timerRunning", timerRunning);
        editor.putLong("endTime", endTime);
        editor.putString("selectedHabitName", tvHabitPlayName.getText().toString());
        editor.apply();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private void clearTimerPrefs() {
        SharedPreferences prefs = getSharedPreferences("timer_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("timerRunning", false).apply();
    }

    private void loadHabitsFromFirestore() {
        if (mAuth.getCurrentUser() == null) return;
        mDB.collection("Users").document(mAuth.getCurrentUser().getEmail()).collection("Habits")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    habitList.clear();
                    habitList.addAll(queryDocumentSnapshots.toObjects(HabitModel.class));
                    if (!habitList.isEmpty()) {
                        habitNames = new String[habitList.size()];
                        for (int i = 0; i < habitList.size(); i++) {
                            habitNames[i] = habitList.get(i).getHabitName();
                        }
                    }
                });
    }

    private void showHabitSelectionDialog() {
        if (habitNames == null || habitNames.length == 0) {
            Toast.makeText(this, "No habits found. Add one in the Habit page!", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Select a Habit")
                .setItems(habitNames, (dialog, which) -> {
                    HabitModel habit = habitList.get(which);
                    tvHabitPlayName.setText(habit.getHabitName());
                    mStartTimeInMillis = (long) habit.getDuration() * 60000;
                    resetTimer();
                }).show();
    }

    private void startTimer() {
        endTime = System.currentTimeMillis() + timeLeftInMillis;
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountDownText();
                updateProgressBar();
            }

            @Override
            public void onFinish() {
                timerRunning = false;
                updateProgressBar();
                btnPlayPause.setIconResource(android.R.drawable.ic_media_play);
                saveHabitProgress(tvHabitPlayName.getText().toString());
                clearTimerPrefs();
            }
        }.start();

        timerRunning = true;
        btnPlayPause.setIconResource(android.R.drawable.ic_media_pause);
    }

    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        timerRunning = false;
        btnPlayPause.setIconResource(android.R.drawable.ic_media_play);
    }

    private void resetTimer() {
        pauseTimer();
        timeLeftInMillis = mStartTimeInMillis;
        updateCountDownText();
        updateProgressBar();
    }

    private void saveHabitProgress(String habitName) {
        if (habitName.equals("Focus Session") || habitName.equals("Choose Habit")) return;

        if (mAuth.getCurrentUser() == null) return;

        mDB.collection("Users").document(mAuth.getCurrentUser().getEmail())
                .collection("Habits").document(habitName)
                .update("completedDays", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Log.d("PlayHabit", "Progress recorded for: " + habitName);
                    Toast.makeText(this, "Session complete! Progress saved for " + habitName, Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> Log.e("PlayHabit", "Error updating progress", e));
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
    }

    private void updateProgressBar() {
        if (mStartTimeInMillis > 0) {
            int progress = (int) ((float) timeLeftInMillis / mStartTimeInMillis * 1000);
            progressTimer.setProgress(progress);
        }
    }
}