package com.example.habitflows;

import android.content.Intent;
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
    private UserAdapter adapter;
    private List<UserModel> userList = new ArrayList<>();
    private List<String> followingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_discover);

            mAuth = FirebaseAuth.getInstance();
            mDB = FirebaseFirestore.getInstance();

            ImageView btnBack = findViewById(R.id.btnBackDiscover);
            rvDiscover = findViewById(R.id.rvDiscover);

            if (btnBack != null) btnBack.setOnClickListener(v -> finish());

            setupRecyclerView();

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            loadCurrentUserAndUsers();
        } catch (Exception e) {
            Log.e("Discover", "Crash in onCreate", e);
            Toast.makeText(this, "Error opening Discover", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        if (rvDiscover == null) return;
        adapter = new UserAdapter(userList, followingList, this::handleFollowClick, user -> {
            Intent intent = new Intent(Discover.this, Profile.class);
            // Pass the exact document ID found in the database to ensure Profile loads the correct data
            intent.putExtra("target_email", user.getEmail());
            startActivity(intent);
        });
        rvDiscover.setLayoutManager(new LinearLayoutManager(this));
        rvDiscover.setAdapter(adapter);
    }

    private void loadCurrentUserAndUsers() {
        FirebaseUser currentUserAuth = mAuth.getCurrentUser();
        if (currentUserAuth == null || currentUserAuth.getEmail() == null) return;
        
        String currentEmail = currentUserAuth.getEmail().toLowerCase().trim();

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
                        Map<String, Object> newUser = new HashMap<>();
                        newUser.put("email", currentEmail);
                        newUser.put("uid", currentUserAuth.getUid());
                        String username = currentUserAuth.getDisplayName();
                        if (username == null || username.isEmpty()) {
                            username = currentEmail.split("@")[0];
                        }
                        newUser.put("username", username);
                        newUser.put("following", new ArrayList<String>());
                        newUser.put("overallProgress", 0);
                        mDB.collection("Users").document(currentEmail).set(newUser, SetOptions.merge());
                    }
                    fetchAllUsers(currentEmail);
                })
                .addOnFailureListener(e -> fetchAllUsers(currentEmail));
    }

    private void fetchAllUsers(String currentEmail) {
        mDB.collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Use a Map to consolidate users by normalized email
                    Map<String, UserModel> consolidatedUsers = new LinkedHashMap<>();
                    
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            UserModel user = doc.toObject(UserModel.class);
                            if (user != null) {
                                String docId = doc.getId();
                                String normalizedEmail = docId.toLowerCase().trim();
                                
                                // Sync the model's email field with the actual database document ID
                                user.setEmail(docId);
                                
                                // Skip current user
                                if (normalizedEmail.equals(currentEmail)) continue;

                                if (consolidatedUsers.containsKey(normalizedEmail)) {
                                    UserModel existing = consolidatedUsers.get(normalizedEmail);
                                    
                                    boolean existingHasImage = existing.getProfileImageBase64() != null && !existing.getProfileImageBase64().isEmpty();
                                    boolean currentHasImage = user.getProfileImageBase64() != null && !user.getProfileImageBase64().isEmpty();
                                    
                                    // Preference logic to pick the document with the most complete info
                                    if (!existingHasImage && currentHasImage) {
                                        consolidatedUsers.put(normalizedEmail, user);
                                    } else if (existingHasImage && currentHasImage) {
                                        // If both have images, prefer the lowercase doc ID as it's the standard
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
                    
                    if (adapter != null) adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("Discover", "Failed to fetch users", e);
                    Toast.makeText(this, "Could not load users", Toast.LENGTH_SHORT).show();
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
                        if (adapter != null) adapter.notifyDataSetChanged();
                    });
        } else {
            mDB.collection("Users").document(currentEmail)
                    .update("following", FieldValue.arrayUnion(targetUser.getUid()))
                    .addOnSuccessListener(aVoid -> {
                        followingList.add(targetUser.getUid());
                        if (adapter != null) adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("following", followingList);
                        followingList.add(targetUser.getUid());
                        mDB.collection("Users").document(currentEmail).set(data, SetOptions.merge());
                    });
        }
    }
}