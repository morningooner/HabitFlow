package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
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

    // Shown once per login session; reset on logout
    private static boolean sWelcomeShown = false;
    private Button btnProfile, btnHabit, btnStatistics, btnPlayHabit, btnLeaderboard, btnDiscover, btnRankMenu, mainMenuLogoutBtn;
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
        mainMenuLogoutBtn = findViewById(R.id.mainMenuLogoutBtn);

        btnProfile = findViewById(R.id.Profile);
        btnHabit = findViewById(R.id.btnHabit);
        btnStatistics = findViewById(R.id.btnStatistics);
        btnPlayHabit = findViewById(R.id.btnPlayHabit);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        btnDiscover = findViewById(R.id.btnDiscover);
        btnRankMenu = findViewById(R.id.btnRankMenu);

        if (currentUser != null && currentUser.getEmail() != null) {
            mDB.collection("Users").document(currentUser.getEmail().toLowerCase().trim()).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    UserModel user = doc.toObject(UserModel.class);
                    if (user != null) {
                        mainMenuHomeTV4.setText(user.getUsername());
                    }
                }
            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize all click listeners including logout
        setupClickListeners(mainMenuLogoutBtn);

        // Run the "Solo Leveling" System Entrance Animation
        SystemEntranceAnim.applySystemEntranceAnimation(mainMenuHomeTV, systemContainer, mainMenuLogoutBtn);

        if (!sWelcomeShown) {
            sWelcomeShown = true;
            showWelcomeOverlay();
        }
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

    private void showWelcomeOverlay() {
        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();

        View welcomeView = LayoutInflater.from(this).inflate(R.layout.item_welcome_player, null);

        // Set the player name
        TextView tvWelcome = welcomeView.findViewById(R.id.tvWelcomePlayer);
        if (currentUser != null) {
            String name = currentUser.getDisplayName();
            tvWelcome.setText("WELCOME BACK, " + (name != null ? name.toUpperCase() : "PLAYER"));
        }

        // Wrap in a full-screen dim overlay
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0xDD05050A);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        contentParams.gravity = Gravity.CENTER;
        overlay.addView(welcomeView, contentParams);

        decorView.addView(overlay, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Fade out and remove after 3 seconds
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                overlay.animate()
                        .alpha(0f)
                        .setDuration(600)
                        .withEndAction(() -> decorView.removeView(overlay))
                        .start(),
                3000);
    }

    private void setupClickListeners(Button logoutBtn) {
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> {
                sWelcomeShown = false;
                mAuth.signOut();
                Intent intent = new Intent(MainMenu.this, LoginMenu.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
        
        btnHabit.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, Habit.class)));
        btnProfile.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, Profile.class)));
        btnStatistics.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, Statistics.class)));
        btnPlayHabit.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, playHabit.class)));
        btnDiscover.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, Discover.class)));
        btnLeaderboard.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, LeaderboardMenu.class)));
        btnRankMenu.setOnClickListener(v -> startActivity(new Intent(MainMenu.this, RankingMenu.class)));
    }
}
