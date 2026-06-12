package com.example.habitflows;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
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
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

        LinearLayout playContainer = findViewById(R.id.playContainer);

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
        updatePlayButtonState(); // Initial check

        SystemEntranceAnim.applySystemEntranceAnimation(btnBackPlay, playContainer, tvHabitPlayName);
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
        updatePlayButtonState();

        if (timerRunning) {
            endTime = prefs.getLong("endTime", 0);
            timeLeftInMillis = endTime - System.currentTimeMillis();

            if (timeLeftInMillis < 0) {
                timeLeftInMillis = 0;
                timerRunning = false;
                updateCountDownText();
                updateProgressBar();
                
                // If it finished while we were away, record progress
                if (!savedHabit.equals("Focus Session") && !savedHabit.equals("Choose Habit") && !savedHabit.equals("QUEST TIMER")) {
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
                    updatePlayButtonState();
                }).show();
    }

    private void updatePlayButtonState() {
        String currentHabit = tvHabitPlayName.getText().toString();
        boolean isHabitSelected = !currentHabit.equals("Focus Session") && !currentHabit.equals("Choose Habit") && !currentHabit.equals("QUEST TIMER");
        btnPlayPause.setEnabled(isHabitSelected);
        btnPlayPause.setAlpha(isHabitSelected ? 1.0f : 0.5f);
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
        if (habitName.equals("Focus Session") || habitName.equals("Choose Habit") || habitName.equals("QUEST TIMER")) return;

        if (mAuth.getCurrentUser() == null) return;

        String userEmail = mAuth.getCurrentUser().getEmail();

        // Read user document to get profession for XP buff calculation
        mDB.collection("Users").document(userEmail).get().addOnSuccessListener(userDoc -> {
            UserModel user = userDoc.toObject(UserModel.class);
            if (user == null) return;

            // Read the habit to check if XP was already granted today
            mDB.collection("Users").document(userEmail)
                    .collection("Habits").document(habitName)
                    .get()
                    .addOnSuccessListener(doc -> {
                        HabitModel habit = doc.toObject(HabitModel.class);
                        boolean alreadyDoneToday = habit != null && habit.isTodayCompleted();

                        if (!alreadyDoneToday) {
                            // First completion today — increment days, mark done, record date
                            String today = LocalDate.now().toString();
                            mDB.collection("Users").document(userEmail)
                                    .collection("Habits").document(habitName)
                                    .update("completedDays", FieldValue.increment(1),
                                            "todayCompleted", true,
                                            "completedDates", FieldValue.arrayUnion(today))
                                    .addOnSuccessListener(aVoid -> checkAllHabitsCompleted());

                            // Calculate XP with 20% buff based on profession and day
                            int xpToGrant = calculateXpWithBuff(user.getProfession());
                            boolean hasBuff = xpToGrant > 10;

                            mDB.collection("Users").document(userEmail)
                                    .update("xp", FieldValue.increment(xpToGrant))
                                    .addOnSuccessListener(aVoid -> {
                                        String message = "QUEST COMPLETE! +" + xpToGrant + " XP earned.";
                                        if (hasBuff) {
                                            message += " (20% Profession Buff Applied!)";
                                        }
                                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            // Already completed today — session recorded but no extra XP
                            Toast.makeText(this, "Session complete! Habit already done today — no extra XP.", Toast.LENGTH_SHORT).show();
                            checkAllHabitsCompleted();
                        }
                    })
                    .addOnFailureListener(e -> Log.e("PlayHabit", "Error reading habit", e));
        });
    }

    private int calculateXpWithBuff(String profession) {
        int baseXp = 10;
        DayOfWeek day = LocalDate.now().getDayOfWeek();
        boolean isBuffDay = false;

        // Monday: Mage, Tuesday: Assassin, Wednesday: Fighter, Thursday: Marksman, Friday: Tank
        switch (day) {
            case MONDAY:
                isBuffDay = "Mage".equalsIgnoreCase(profession);
                break;
            case TUESDAY:
                isBuffDay = "Assassin".equalsIgnoreCase(profession);
                break;
            case WEDNESDAY:
                isBuffDay = "Fighter".equalsIgnoreCase(profession);
                break;
            case THURSDAY:
                isBuffDay = "Marksman".equalsIgnoreCase(profession);
                break;
            case FRIDAY:
                isBuffDay = "Tank".equalsIgnoreCase(profession);
                break;
        }

        if (isBuffDay) {
            return 12; // 20% buff on 10 XP
        }
        return baseXp;
    }

    private void checkAllHabitsCompleted() {
        if (mAuth.getCurrentUser() == null) return;
        String email = mAuth.getCurrentUser().getEmail();
        String today = LocalDate.now().toString();

        mDB.collection("Users").document(email).get().addOnSuccessListener(userDoc -> {
            UserModel user = userDoc.toObject(UserModel.class);
            if (user != null && !today.equals(user.getLastStreakUpdateDate())) {
                
                mDB.collection("Users").document(email).collection("Habits").get().addOnSuccessListener(query -> {
                    boolean allDone = true;
                    if (query.isEmpty()) return;

                    for (DocumentSnapshot doc : query) {
                        HabitModel habit = doc.toObject(HabitModel.class);
                        if (habit != null && !habit.isTodayCompleted()) {
                            allDone = false;
                            break;
                        }
                    }

                    if (allDone) {
                        int newStreak = user.getStreak() + 1;
                        mDB.collection("Users").document(email)
                                .update("streak", newStreak, "lastStreakUpdateDate", today)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "SYSTEM ALERT: All Quests Cleared! Streak: " + newStreak, Toast.LENGTH_LONG).show();
                                });
                    }
                });
            }
        });
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
