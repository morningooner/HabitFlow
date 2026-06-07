package com.example.habitflows;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Profile extends AppCompatActivity {

    private ImageView ivProfile;
    private TextView tvName, tvDescription, tvFollowingCount, tvFollowerCount, tvPostCount, tvLevelDisplay, tvUserHeaderName;
    private MaterialButton btnFollow;
    private LinearLayout llHabitsList;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;
    private FirebaseUser currentUser;
    private String targetEmail;
    private boolean isOwnProfile;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    ivProfile.setImageURI(uri);
                    uploadImageToFirestore(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Get target email and normalize it
        String rawTarget = getIntent().getStringExtra("target_email");
        if (rawTarget != null) {
            targetEmail = rawTarget.toLowerCase().trim();
        }

        // Normalize current user email
        String currentEmail = (currentUser != null && currentUser.getEmail() != null) 
                ? currentUser.getEmail().toLowerCase().trim() : null;

        isOwnProfile = (targetEmail == null || (currentEmail != null && targetEmail.equals(currentEmail)));
        if (targetEmail == null) targetEmail = currentEmail;

        initViews();
        setupListeners();
        loadProfileData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initViews() {
        ivProfile = findViewById(R.id.ivProfile);
        tvName = findViewById(R.id.tvName);
        tvDescription = findViewById(R.id.tvDescription);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        tvFollowerCount = findViewById(R.id.tvFollowerCount);
        tvPostCount = findViewById(R.id.tvPostCount);
        tvLevelDisplay = findViewById(R.id.tvLevelDisplay);
        tvUserHeaderName = findViewById(R.id.tvUserHeaderName);
        btnFollow = findViewById(R.id.btnFollow);
        llHabitsList = findViewById(R.id.llHabitsList);
        ImageView ivBack = findViewById(R.id.ivBack);

        if (isOwnProfile) {
            btnFollow.setText("EDIT BIO");
            tvUserHeaderName.setText("MY PROFILE");
        } else {
            tvUserHeaderName.setText("PLAYER PROFILE");
        }

        ivBack.setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        if (isOwnProfile) {
            ivProfile.setOnClickListener(v -> pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build()));
            btnFollow.setOnClickListener(v -> Toast.makeText(this, "Edit Bio coming soon", Toast.LENGTH_SHORT).show());
        } else {
            btnFollow.setOnClickListener(v -> handleFollowUnfollow());
        }
    }

    private void loadProfileData() {
        if (targetEmail == null) return;

        mDB.collection("Users").document(targetEmail).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                UserModel user = doc.toObject(UserModel.class);
                if (user != null) {
                    tvName.setText(user.getUsername());
                    tvLevelDisplay.setText("LVL : " + (user.getOverallProgress() / 10 + 1));
                    
                    String encodedImage = user.getProfileImageBase64();
                    if (encodedImage != null && !encodedImage.isEmpty()) {
                        try {
                            byte[] decodedByte = Base64.decode(encodedImage, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
                            ivProfile.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            ivProfile.setImageResource(R.drawable.profilepic);
                        }
                    }

                    List<String> following = user.getFollowing();
                    tvFollowingCount.setText(String.valueOf(following != null ? following.size() : 0));
                    
                    if (!isOwnProfile) {
                        checkIfFollowing();
                    }
                }
            }
        });

        // Load Habits
        mDB.collection("Users").document(targetEmail).collection("Habits").get().addOnSuccessListener(query -> {
            llHabitsList.removeAllViews();
            tvPostCount.setText(String.valueOf(query.size()));
            for (DocumentSnapshot doc : query.getDocuments()) {
                HabitModel habit = doc.toObject(HabitModel.class);
                if (habit != null) {
                    addHabitToLayout(habit);
                }
            }
        });
    }

    private void addHabitToLayout(HabitModel habit) {
        View habitView = LayoutInflater.from(this).inflate(R.layout.item_habit, llHabitsList, false);
        
        TextView tvHabitName = habitView.findViewById(R.id.tvHabitName);
        TextView tvDuration = habitView.findViewById(R.id.tvHabitDuration);
        TextView tvPercentage = habitView.findViewById(R.id.tvHabitPercentage);
        View btnEdit = habitView.findViewById(R.id.btnEditHabit);
        View btnDelete = habitView.findViewById(R.id.btnDeleteHabit);

        if (!isOwnProfile) {
            btnEdit.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }

        tvHabitName.setText(habit.getHabitName());
        tvDuration.setText(habit.getCompletedDays() + " / " + habit.getDuration() + " " + habit.getUnit());
        
        int progress = (int) ((habit.getCompletedDays() / (float) habit.getDuration()) * 100);
        tvPercentage.setText(progress + "%");
        
        com.google.android.material.progressindicator.LinearProgressIndicator progressIndicator = 
                habitView.findViewById(R.id.habitProgressIndicator);
        progressIndicator.setProgress(progress);

        llHabitsList.addView(habitView);
    }

    private void checkIfFollowing() {
        if (currentUser == null || currentUser.getEmail() == null) return;
        String currentEmail = currentUser.getEmail().toLowerCase().trim();
        
        mDB.collection("Users").document(currentEmail).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                List<String> myFollowing = (List<String>) doc.get("following");
                mDB.collection("Users").document(targetEmail).get().addOnSuccessListener(targetDoc -> {
                    String targetUid = targetDoc.getString("uid");
                    if (myFollowing != null && myFollowing.contains(targetUid)) {
                        btnFollow.setText("UNFOLLOW");
                    } else {
                        btnFollow.setText("FOLLOW");
                    }
                });
            }
        });
    }

    private void handleFollowUnfollow() {
        if (currentUser == null || currentUser.getEmail() == null) return;
        String currentEmail = currentUser.getEmail().toLowerCase().trim();

        mDB.collection("Users").document(targetEmail).get().addOnSuccessListener(targetDoc -> {
            String targetUid = targetDoc.getString("uid");
            if (targetUid == null) return;

            boolean isFollowing = btnFollow.getText().toString().equalsIgnoreCase("UNFOLLOW");

            if (isFollowing) {
                mDB.collection("Users").document(currentEmail)
                        .update("following", FieldValue.arrayRemove(targetUid))
                        .addOnSuccessListener(aVoid -> btnFollow.setText("FOLLOW"));
            } else {
                mDB.collection("Users").document(currentEmail)
                        .update("following", FieldValue.arrayUnion(targetUid))
                        .addOnSuccessListener(aVoid -> btnFollow.setText("UNFOLLOW"));
            }
        });
    }

    private void uploadImageToFirestore(android.net.Uri uri) {
        if (currentUser == null || currentUser.getEmail() == null) return;
        String currentEmail = currentUser.getEmail().toLowerCase().trim();
        
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos);
            String encodedImage = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            Map<String, Object> data = new HashMap<>();
            data.put("profileImageBase64", encodedImage);
            mDB.collection("Users").document(currentEmail).set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile picture updated!", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
        }
    }
}