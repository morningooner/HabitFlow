package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Discover extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;
    private RecyclerView rvDiscover;
    
    private UserAdapter userAdapter;
    private List<UserModel> userList = new ArrayList<>();
    
    private ActivityAdapter activityAdapter;
    private List<ActivityModel> activityList = new ArrayList<>();
    
    private List<String> followingList = new ArrayList<>();
    private String currentUserUid;
    private MaterialButtonToggleGroup toggleGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_discover);

            mAuth = FirebaseAuth.getInstance();
            mDB = FirebaseFirestore.getInstance();

            ConstraintLayout topBar = findViewById(R.id.topBar);
            CardView discoverWindow = findViewById(R.id.contentWindow);
            LinearLayout bottomIcons = findViewById(R.id.bottomIcons);

            ImageView btnBack = findViewById(R.id.btnBackDiscover);
            rvDiscover = findViewById(R.id.rvDiscover);
            toggleGroup = findViewById(R.id.toggleGroupDiscover);

            if (btnBack != null) btnBack.setOnClickListener(v -> finish());

            setupRecyclerView();
            setupToggle();

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
            loadCurrentUserAndData();

            // Run the "Solo Leveling" System Entrance Animation
            SystemEntranceAnim.applySystemEntranceAnimation(topBar, discoverWindow, bottomIcons);

        } catch (Exception e) {
            Log.e("Discover", "Crash in onCreate", e);
            Toast.makeText(this, "Error opening Discover", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        if (rvDiscover == null) return;
        rvDiscover.setLayoutManager(new LinearLayoutManager(this));
        
        // Initialize Adapters
        userAdapter = new UserAdapter(userList, followingList, this::handleFollowClick, user -> {
            Intent intent = new Intent(Discover.this, Profile.class);
            intent.putExtra("target_email", user.getEmail());
            startActivity(intent);
        });
        
        activityAdapter = new ActivityAdapter(activityList, email -> {
            Intent intent = new Intent(Discover.this, Profile.class);
            intent.putExtra("target_email", email);
            startActivity(intent);
        });
        
        // Default to Follow People view
        rvDiscover.setAdapter(userAdapter);
    }

    private void setupToggle() {
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnFollowPeople) {
                    rvDiscover.setAdapter(userAdapter);
                    fetchAllUsers(mAuth.getCurrentUser().getEmail().toLowerCase().trim());
                } else if (checkedId == R.id.btnActivityFeed) {
                    rvDiscover.setAdapter(activityAdapter);
                    loadActivityFeed();
                }
            }
        });
    }

    private void loadCurrentUserAndData() {
        FirebaseUser currentUserAuth = mAuth.getCurrentUser();
        if (currentUserAuth == null || currentUserAuth.getEmail() == null) return;
        
        String currentEmail = currentUserAuth.getEmail().toLowerCase().trim();
        currentUserUid = currentUserAuth.getUid();

        mDB.collection("Users").document(currentEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    followingList.clear();
                    if (documentSnapshot.exists()) {
                        UserModel user = documentSnapshot.toObject(UserModel.class);
                        if (user != null && user.getFollowing() != null) {
                            followingList.addAll(user.getFollowing());
                        }
                    } else {
                        // Create user document if it doesn't exist
                        Map<String, Object> newUser = new HashMap<>();
                        newUser.put("email", currentEmail);
                        newUser.put("uid", currentUserUid);
                        String username = currentUserAuth.getDisplayName();
                        if (username == null || username.isEmpty()) {
                            username = currentEmail.split("@")[0];
                        }
                        newUser.put("username", username);
                        newUser.put("following", new ArrayList<String>());
                        newUser.put("overallProgress", 0);
                        mDB.collection("Users").document(currentEmail).set(newUser, SetOptions.merge());
                    }
                    
                    // Initial load based on current selection
                    if (toggleGroup.getCheckedButtonId() == R.id.btnFollowPeople) {
                        fetchAllUsers(currentEmail);
                    } else {
                        loadActivityFeed();
                    }
                })
                .addOnFailureListener(e -> fetchAllUsers(currentEmail));
    }

    private void loadActivityFeed() {
        List<String> queryIds = new ArrayList<>(followingList);
        if (currentUserUid != null) {
            queryIds.add(currentUserUid); // Include own activities
        }

        if (queryIds.isEmpty()) {
            activityList.clear();
            activityAdapter.notifyDataSetChanged();
            return;
        }

        // Firestore whereIn supports up to 30 items.
        // If more, we should ideally use multiple queries, but for now we limit to first 30.
        List<String> limitedIds = queryIds.size() > 30 ? queryIds.subList(0, 30) : queryIds;

        mDB.collection("Activities")
                .whereIn("userId", limitedIds)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    activityList.clear();
                    activityList.addAll(queryDocumentSnapshots.toObjects(ActivityModel.class));
                    activityAdapter.notifyDataSetChanged();
                    if (activityList.isEmpty()) {
                        Toast.makeText(this, "No recent activity found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Discover", "Error loading activity feed", e);
                    // This error is often caused by missing composite index in Firestore.
                    // Check Logcat for the link to create it.
                    Toast.makeText(this, "Error loading feed. Check index.", Toast.LENGTH_LONG).show();
                });
    }

    private void fetchAllUsers(String currentEmail) {
        mDB.collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, UserModel> consolidatedUsers = new LinkedHashMap<>();
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            UserModel user = doc.toObject(UserModel.class);
                            if (user != null) {
                                String docId = doc.getId();
                                String normalizedEmail = docId.toLowerCase().trim();
                                user.setEmail(docId);
                                
                                if (normalizedEmail.equals(currentEmail)) continue;

                                if (consolidatedUsers.containsKey(normalizedEmail)) {
                                    UserModel existing = consolidatedUsers.get(normalizedEmail);
                                    boolean existingHasImage = existing.getProfileImageBase64() != null && !existing.getProfileImageBase64().isEmpty();
                                    boolean currentHasImage = user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty();
                                    
                                    if (!existingHasImage && currentHasImage) {
                                        consolidatedUsers.put(normalizedEmail, user);
                                    } else if (existingHasImage && currentHasImage) {
                                        if (docId.equals(normalizedEmail)) {
                                            consolidatedUsers.put(normalizedEmail, user);
                                        }
                                    }
                                } else {
                                    consolidatedUsers.put(normalizedEmail, user);
                                }
                            }
                        } catch (Exception e) {
                            Log.e("Discover", "Error parsing user: " + doc.getId());
                        }
                    }
                    
                    userList.clear();
                    userList.addAll(consolidatedUsers.values());
                    
                    if (userAdapter != null) userAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Discover", "Failed to fetch users", e);
                    Toast.makeText(this, "Failed to load users", Toast.LENGTH_SHORT).show();
                });
    }

    private void handleFollowClick(UserModel targetUser, boolean isCurrentlyFollowing) {
        FirebaseUser currentUserAuth = mAuth.getCurrentUser();
        if (currentUserAuth == null || currentUserAuth.getEmail() == null || targetUser.getUid() == null) return;
        
        String currentEmail = currentUserAuth.getEmail().toLowerCase().trim();

        if (isCurrentlyFollowing) {
            mDB.collection("Users").document(currentEmail)
                    .update("following", FieldValue.arrayRemove(targetUser.getUid()))
                    .addOnSuccessListener(aVoid -> {
                        followingList.remove(targetUser.getUid());
                        userAdapter.notifyDataSetChanged();
                        if (toggleGroup.getCheckedButtonId() == R.id.btnActivityFeed) {
                            loadActivityFeed();
                        }
                    });
        } else {
            mDB.collection("Users").document(currentEmail)
                    .update("following", FieldValue.arrayUnion(targetUser.getUid()))
                    .addOnSuccessListener(aVoid -> {
                        followingList.add(targetUser.getUid());
                        userAdapter.notifyDataSetChanged();
                        if (toggleGroup.getCheckedButtonId() == R.id.btnActivityFeed) {
                            loadActivityFeed();
                        }
                    });
        }
    }
}
