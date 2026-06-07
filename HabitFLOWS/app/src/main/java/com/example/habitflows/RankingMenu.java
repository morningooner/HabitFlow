package com.example.habitflows;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RankingMenu extends AppCompatActivity {

    private FirebaseFirestore mDB;
    private FirebaseAuth mAuth;
    
    private TextView tvUsername, tvUserRankClass, tvRankInitial, tvXpProgressText, tvXpLabel;
    private LinearProgressIndicator xpProgressBar;
    private ImageView ivRankStarDecor, btnRankingBack, ivMainRankBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ranking_menu);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();

        // Initialize UI
        tvUsername = findViewById(R.id.tvUsername);
        tvUserRankClass = findViewById(R.id.tvUserRankClass);
        tvRankInitial = findViewById(R.id.tvRankInitial);
        tvXpProgressText = findViewById(R.id.tvXpProgressText);
        tvXpLabel = findViewById(R.id.tvXpLabel);
        xpProgressBar = findViewById(R.id.xpProgressBar);
        ivRankStarDecor = findViewById(R.id.ivRankStarDecor);
        ivMainRankBadge = findViewById(R.id.ivMainRankBadge);
        btnRankingBack = findViewById(R.id.btnRankingBack);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnRankingBack.setOnClickListener(v -> finish());

        loadUserStats();
    }

    private void loadUserStats() {
        if (mAuth.getCurrentUser() == null) return;
        
        mDB.collection("Users").document(mAuth.getCurrentUser().getEmail())
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        UserModel user = doc.toObject(UserModel.class);
                        if (user != null) {
                            tvUsername.setText(user.getUsername().toUpperCase());
                            updateRankDisplay(user);
                            checkEligibilityToRankUp(user);
                        }
                    }
                });
    }

    private void updateRankDisplay(UserModel user) {
        int xp = user.getXp();
        String currentRank = user.getRank();
        
        int nextXp;
        int currentLevelMinXp;
        String nextRankName;

        // Visual rules for different Classes
        if (xp < 100) { currentLevelMinXp = 0; nextXp = 100; nextRankName = "E"; }
        else if (xp < 300) { currentLevelMinXp = 100; nextXp = 300; nextRankName = "D"; }
        else if (xp < 700) { currentLevelMinXp = 300; nextXp = 700; nextRankName = "C"; }
        else if (xp < 1500) { currentLevelMinXp = 700; nextXp = 1500; nextRankName = "B"; }
        else if (xp < 3000) { currentLevelMinXp = 1500; nextXp = 3000; nextRankName = "A"; }
        else if (xp < 6000) { currentLevelMinXp = 3000; nextXp = 6000; nextRankName = "S"; }
        else { currentLevelMinXp = 0; nextXp = xp; nextRankName = "MAX"; }

        tvRankInitial.setText(currentRank);
        tvUserRankClass.setText(currentRank + "-CLASS");
        
        // Show star for E class and above
        ivRankStarDecor.setVisibility(!currentRank.equals("F") ? View.VISIBLE : View.GONE);
        
        if (!nextRankName.equals("MAX")) {
            int diff = nextXp - xp;
            tvXpProgressText.setText(diff + " XP");
            tvXpLabel.setText("XP TO NEXT RANK (" + nextXp + " TOTAL):");
            int progress = ((xp - currentLevelMinXp) * 100) / (nextXp - currentLevelMinXp);
            xpProgressBar.setProgress(progress);
        } else {
            tvXpProgressText.setText("MAX LEVEL");
            tvXpLabel.setText("ULTIMATE RANK ACHIEVED");
            xpProgressBar.setProgress(100);
        }
    }

    private void checkEligibilityToRankUp(UserModel user) {
        int xp = user.getXp();
        String currentRank = user.getRank();
        String targetRank = "";

        if (xp >= 6000) targetRank = "S";
        else if (xp >= 3000) targetRank = "A";
        else if (xp >= 1500) targetRank = "B";
        else if (xp >= 700) targetRank = "C";
        else if (xp >= 300) targetRank = "D";
        else if (xp >= 100) targetRank = "E";
        else targetRank = "F";

        // If the user's XP qualifies them for a higher rank than what they currently have
        if (!targetRank.equals(currentRank) && isRankHigher(targetRank, currentRank)) {
            showRankUpNotification(user, currentRank, targetRank);
        }
    }

    private boolean isRankHigher(String target, String current) {
        String sequence = "FEDCBAS";
        return sequence.indexOf(target) > sequence.indexOf(current);
    }

    private void showRankUpNotification(UserModel user, String oldRank, String newRank) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_rank_up, null);
        TextView tvOld = dialogView.findViewById(R.id.tvOldRank);
        TextView tvNew = dialogView.findViewById(R.id.tvNewRank);
        ShapeableImageView ivAvatar = dialogView.findViewById(R.id.ivUserAvatar);
        
        tvOld.setText(oldRank + "-Class");
        tvNew.setText(newRank + "-Class");

        // Load avatar if exists
        String base64 = user.getProfileImageBase64();
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                ivAvatar.setImageBitmap(bitmap);
            } catch (Exception ignored) {}
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this, R.style.Theme_HabitFLOWS)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialogView.findViewById(R.id.btnAccept).setOnClickListener(v -> {
            mDB.collection("Users").document(user.getEmail())
                    .update("rank", newRank)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(RankingMenu.this, "System Evolved to " + newRank + " Class!", Toast.LENGTH_LONG).show();
                        dialog.dismiss();
                    });
        });

        dialog.show();
    }
}
