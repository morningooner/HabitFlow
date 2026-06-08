package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnticipateOvershootInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainMenu extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private Button btnProfile, btnHabit, btnStatistics, btnPlayHabit, btnLeaderboard, btnDiscover, btnRankMenu;
    private TextView mainMenuHomeTV4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);

        mAuth = FirebaseAuth.getInstance();

        // Initialize Views
        TextView mainMenuHomeTV = findViewById(R.id.mainMenuHomeTV);
        LinearLayout systemContainer = findViewById(R.id.systemContainer);
        mainMenuHomeTV4 = findViewById(R.id.mainMenuHomeTV4);
        Button mainMenuLogoutBtn = findViewById(R.id.mainMenuLogoutBtn);

        btnProfile = findViewById(R.id.Profile);
        btnHabit = findViewById(R.id.btnHabit);
        btnStatistics = findViewById(R.id.btnStatistics);
        btnPlayHabit = findViewById(R.id.btnPlayHabit);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);
        btnDiscover = findViewById(R.id.btnDiscover);
        btnRankMenu = findViewById(R.id.btnRankMenu);

        if (mAuth.getCurrentUser() != null) {
            String name = mAuth.getCurrentUser().getDisplayName();
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
