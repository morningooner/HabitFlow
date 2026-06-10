package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;

public class MainMenu extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;
    private Button btnProfile, btnHabit, btnStatistics, btnPlayHabit, btnLeaderboard, btnDiscover, btnRankMenu;
    private TextView mainMenuHomeTV4, tvStreakCount;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize Views
        TextView mainMenuHomeTV = findViewById(R.id.mainMenuHomeTV);
        LinearLayout systemContainer = findViewById(R.id.systemContainer);
        mainMenuHomeTV4 = findViewById(R.id.mainMenuHomeTV4);
        tvStreakCount = findViewById(R.id.tvStreakCount);
        Button mainMenuLogoutBtn = findViewById(R.id.mainMenuLogoutBtn);

        btnProfile = findViewById(R.id.Profile);
        btnHabit = findViewById(R.id.btnHabit);
        btnStatistics = findViewById(R.id.btnStatistics);
        btnPlayHabit = findViewById(R.id.btnPlayHabit);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        btnDiscover = findViewById(R.id.btnDiscover);
        btnRankMenu = findViewById(R.id.btnRankMenu);

        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            mainMenuHomeTV4.setText(name != null ? name : "User");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupClickListeners(mainMenuLogoutBtn);

        // Run the "Solo Leveling" System Entrance Animation
        SystemEntranceAnim.applySystemEntranceAnimation(mainMenuHomeTV, systemContainer, mainMenuLogoutBtn);
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUserDataAndCheckStreak();
    }

    private void loadUserDataAndCheckStreak() {
        if (currentUser == null || currentUser.getEmail() == null) return;
        String email = currentUser.getEmail().toLowerCase().trim();
        String today = LocalDate.now().toString();

        mDB.collection("Users").document(email).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                UserModel user = documentSnapshot.toObject(UserModel.class);
                if (user != null) {
                    tvStreakCount.setText(String.valueOf(user.getStreak()));
                    
                    // Daily Reset Check: If last reset was not today, reset todayCompleted for all habits
                    if (!today.equals(user.getLastHabitResetDate())) {
                        resetDailyHabits(user, today);
                    } else {
                        checkHabitCompletion(user);
                    }
                }
            }
        });
    }

    private void resetDailyHabits(UserModel user, String today) {
        String email = user.getEmail().toLowerCase().trim();
        mDB.collection("Users").document(email).collection("Habits").get().addOnSuccessListener(query -> {
            for (DocumentSnapshot doc : query) {
                mDB.collection("Users").document(email).collection("Habits").document(doc.getId())
                        .update("todayCompleted", false);
            }
            // Update last reset date in Firestore
            mDB.collection("Users").document(email).update("lastHabitResetDate", today);
        });
    }

    private void checkHabitCompletion(UserModel user) {
        if (currentUser == null || currentUser.getEmail() == null) return;
        String email = currentUser.getEmail().toLowerCase().trim();
        String today = LocalDate.now().toString();

        // If already updated streak today, no need to check
        if (today.equals(user.getLastStreakUpdateDate())) {
            return;
        }

        mDB.collection("Users").document(email).collection("Habits").get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) return;

            boolean allCompleted = true;
            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                HabitModel habit = doc.toObject(HabitModel.class);
                if (habit != null && !habit.isTodayCompleted()) {
                    allCompleted = false;
                    break;
                }
            }

            if (allCompleted) {
                updateStreak(user);
            }
        });
    }

    private void updateStreak(UserModel user) {
        if (currentUser == null || currentUser.getEmail() == null) return;
        String email = currentUser.getEmail().toLowerCase().trim();
        String today = LocalDate.now().toString();

        int newStreak = user.getStreak() + 1;
        user.setStreak(newStreak);
        user.setLastStreakUpdateDate(today);

        mDB.collection("Users").document(email).set(user).addOnSuccessListener(aVoid -> {
            tvStreakCount.setText(String.valueOf(newStreak));
            Toast.makeText(MainMenu.this, "SYSTEM NOTIFICATION: Daily Quests Completed! Streak +1", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupClickListeners(Button logoutBtn) {
        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainMenu.this, LoginMenu.class));
            finish();
        });
        btnHabit.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, Habit.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, Profile.class)));
        btnStatistics.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, Statistics.class)));
        btnPlayHabit.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, playHabit.class)));
        btnDiscover.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, Discover.class)));
        btnLeaderboard.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, LeaderboardMenu.class)));
        btnRankMenu.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, RankingMenu.class)));
    }
}
