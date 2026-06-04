package com.example.habitflows;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Discover extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;
    private RecyclerView rvDiscover;
    private UserAdapter adapter;
    private List<UserModel> userList = new ArrayList<>();
    private List<String> followingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_discover);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();

        // Initialize UI
        ImageView btnBack = findViewById(R.id.btnBackDiscover);
        rvDiscover = findViewById(R.id.rvDiscover);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        setupRecyclerView();

        if (findViewById(R.id.main) != null) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        loadCurrentUserAndUsers();
    }

    private void setupRecyclerView() {
        if (rvDiscover == null) return;
        adapter = new UserAdapter(userList, followingList, this::handleFollowClick);
        rvDiscover.setLayoutManager(new LinearLayoutManager(this));
        rvDiscover.setAdapter(adapter);
    }

    private void loadCurrentUserAndUsers() {
        FirebaseUser currentUserAuth = mAuth.getCurrentUser();
        if (currentUserAuth == null || currentUserAuth.getEmail() == null) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String currentEmail = currentUserAuth.getEmail();

        // 1. Get current user's profile to see who they already follow
        mDB.collection("Users").document(currentEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    followingList.clear();
                    if (documentSnapshot.exists()) {
                        try {
                            UserModel user = documentSnapshot.toObject(UserModel.class);
                            if (user != null && user.getFollowing() != null) {
                                followingList.addAll(user.getFollowing());
                            }
                        } catch (Exception e) {
                            Log.e("Discover", "Following list parse error", e);
                            // Even if parsing fails, we continue to load other users
                        }
                    }
                    fetchAllUsers(currentEmail);
                })
                .addOnFailureListener(e -> {
                    Log.e("Discover", "Failed to load profile", e);
                    fetchAllUsers(currentEmail);
                });
    }

    private void fetchAllUsers(String currentEmail) {
        mDB.collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            UserModel user = doc.toObject(UserModel.class);
                            // Safety: Only add valid users who are NOT the current user
                            if (user != null && user.getEmail() != null && !user.getEmail().equalsIgnoreCase(currentEmail)) {
                                userList.add(user);
                            }
                        } catch (Exception e) {
                            Log.e("Discover", "User parse error for ID: " + doc.getId(), e);
                        }
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Discover", "Error fetching users", e);
                    Toast.makeText(this, "Failed to load community", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleFollowClick(UserModel targetUser, boolean isCurrentlyFollowing) {
        FirebaseUser currentUserAuth = mAuth.getCurrentUser();
        if (currentUserAuth == null || targetUser.getUid() == null) return;
        
        String currentEmail = currentUserAuth.getEmail();
        if (currentEmail == null) return;

        if (isCurrentlyFollowing) {
            mDB.collection("Users").document(currentEmail)
                    .update("following", FieldValue.arrayRemove(targetUser.getUid()))
                    .addOnSuccessListener(aVoid -> {
                        followingList.remove(targetUser.getUid());
                        if (adapter != null) adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Action failed", Toast.LENGTH_SHORT).show());
        } else {
            mDB.collection("Users").document(currentEmail)
                    .update("following", FieldValue.arrayUnion(targetUser.getUid()))
                    .addOnSuccessListener(aVoid -> {
                        followingList.add(targetUser.getUid());
                        if (adapter != null) adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Action failed", Toast.LENGTH_SHORT).show());
        }
    }
}