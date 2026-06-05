package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardMenu extends AppCompatActivity {

    private FirebaseFirestore mDB;
    private FirebaseAuth mAuth;

    private TextView tvUserRankClass, tvXpLabel, tvXpProgressText;
    private ImageView ivMainRankBadge, ivRankStarDecor;
    private RecyclerView rvRankedUsers;
    private RankAdapter adapter;
    private List<UserModel> userList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leaderboard_menu);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();

        // Initialize UI
        ImageView btnRankingBack = findViewById(R.id.btnRankingBack);
        ivMainRankBadge = findViewById(R.id.ivMainRankBadge);
        ivRankStarDecor = findViewById(R.id.ivRankStarDecor);
        tvUserRankClass = findViewById(R.id.tvUserRankClass);
        tvXpLabel = findViewById(R.id.tvXpLabel);
        tvXpProgressText = findViewById(R.id.tvXpProgressText);
        rvRankedUsers = findViewById(R.id.rvRankedUsers);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnRankingBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        userList = new ArrayList<>();
        adapter = new RankAdapter(userList);
        rvRankedUsers.setLayoutManager(new LinearLayoutManager(this));
        rvRankedUsers.setAdapter(adapter);

        loadRankings();
        loadCurrentUserRank();
    }

    private void loadCurrentUserRank() {
        if (mAuth.getCurrentUser() == null) return;

        mDB.collection("Users").document(mAuth.getCurrentUser().getEmail())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        if (user != null) {
                            updateRankUI(user.getXp());
                        }
                    }
                });
    }

    private void updateRankUI(int xp) {
        String rankClass;
        String nextRank;
        int nextRankXp;
        int color;

        if (xp < 100) {
            rankClass = "F Class"; nextRank = "E Class"; nextRankXp = 100; color = 0xFFFF5252;
            ivRankStarDecor.setVisibility(View.GONE);
        } else if (xp < 300) {
            rankClass = "E Class"; nextRank = "D Class"; nextRankXp = 300; color = 0xFF4CAF50;
            ivRankStarDecor.setVisibility(View.VISIBLE);
        } else if (xp < 700) {
            rankClass = "D Class"; nextRank = "C Class"; nextRankXp = 700; color = 0xFF2196F3;
            ivRankStarDecor.setVisibility(View.VISIBLE);
        } else if (xp < 1500) {
            rankClass = "C Class"; nextRank = "B Class"; nextRankXp = 1500; color = 0xFF9C27B0;
            ivRankStarDecor.setVisibility(View.VISIBLE);
        } else if (xp < 3000) {
            rankClass = "B Class"; nextRank = "A Class"; nextRankXp = 3000; color = 0xFFFF9800;
            ivRankStarDecor.setVisibility(View.VISIBLE);
        } else if (xp < 6000) {
            rankClass = "A Class"; nextRank = "S Class"; nextRankXp = 6000; color = 0xFFE91E63;
            ivRankStarDecor.setVisibility(View.VISIBLE);
        } else {
            rankClass = "S Class"; nextRank = "MAX"; nextRankXp = xp; color = 0xFFFFD700;
            ivRankStarDecor.setVisibility(View.VISIBLE);
        }

        tvUserRankClass.setText(rankClass);
        ivMainRankBadge.setColorFilter(color);
        ivRankStarDecor.setColorFilter(color);

        if (!nextRank.equals("MAX")) {
            tvXpLabel.setText("Top 5 advance to " + nextRank);
            tvXpProgressText.setText((nextRankXp - xp) + " XP to Next Rank");
        } else {
            tvXpLabel.setText("Ultimate Rank Achieved");
            tvXpProgressText.setText("You are at the top!");
        }
    }

    private void loadRankings() {
        mDB.collection("Users")
                .orderBy("xp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading rankings", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        userList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            UserModel user = doc.toObject(UserModel.class);
                            if (user != null) {
                                userList.add(user);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
