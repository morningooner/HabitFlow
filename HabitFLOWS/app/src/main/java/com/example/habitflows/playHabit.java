package com.example.habitflows;

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
import com.google.firebase.firestore.DocumentSnapshot;
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
    private long timeLeftInMillis = mStartTimeInMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_play_habit);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();

        // Initialize UI components
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

        // Click Listeners
        btnBackPlay.setOnClickListener(v -> finish());

        btnSelectHabitPlay.setOnClickListener(v -> showHabitSelectionDialog());

        btnPlayPause.setOnClickListener(v -> {
            if (timerRunning) {
                pauseTimer();
            } else {
                startTimer();
            }
        });

        btnResetTimer.setOnClickListener(v -> resetTimer());

        updateCountDownText();
        loadHabitsFromFirestore();
    }

    private void loadHabitsFromFirestore() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        mDB.collection("Users").document(userId).collection("Habits")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    habitList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        HabitModel habit = doc.toObject(HabitModel.class);
                        if (habit != null) {
                            habitList.add(habit);
                        }
                    }

                    if (!habitList.isEmpty()) {
                        habitNames = new String[habitList.size()];
                        for (int i = 0; i < habitList.size(); i++) {
                            habitNames[i] = habitList.get(i).getHabitName();
                        }
                        btnSelectHabitPlay.setEnabled(true);
                    } else {
                        btnSelectHabitPlay.setEnabled(false);
                        btnSelectHabitPlay.setText("No Habits Found");
                    }
                })
                .addOnFailureListener(e -> Log.e("PlayHabit", "Error loading habits", e));
    }

    private void showHabitSelectionDialog() {
        if (habitNames == null || habitNames.length == 0) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("What habit are you doing?");
        builder.setItems(habitNames, (dialog, which) -> {
            HabitModel selectedHabit = habitList.get(which);
            tvHabitPlayName.setText(selectedHabit.getHabitName());
            
            // Set timer based on habit duration (converting minutes to milliseconds)
            if (selectedHabit.getDuration() > 0) {
                mStartTimeInMillis = (long) selectedHabit.getDuration() * 60000;
                resetTimer();
                Toast.makeText(this, "Timer set for " + selectedHabit.getDuration() + " minutes", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void startTimer() {
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
                btnPlayPause.setIconResource(android.R.drawable.ic_media_play);
                Toast.makeText(playHabit.this, "Habit session complete! Well done.", Toast.LENGTH_SHORT).show();
            }
        }.start();

        timerRunning = true;
        btnPlayPause.setIconResource(android.R.drawable.ic_media_pause);
    }

    private void pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerRunning = false;
        btnPlayPause.setIconResource(android.R.drawable.ic_media_play);
    }

    private void resetTimer() {
        pauseTimer();
        timeLeftInMillis = mStartTimeInMillis;
        updateCountDownText();
        progressTimer.setProgress(1000);
    }

    private void updateCountDownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        tvTimer.setText(timeLeftFormatted);
    }

    private void updateProgressBar() {
        if (mStartTimeInMillis > 0) {
            int progress = (int) ((float) timeLeftInMillis / mStartTimeInMillis * 1000);
            progressTimer.setProgress(progress);
        }
    }
}
