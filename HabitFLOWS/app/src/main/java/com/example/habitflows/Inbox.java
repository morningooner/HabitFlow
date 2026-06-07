package com.example.habitflows;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Inbox extends AppCompatActivity {

    private RecyclerView rvRequests;
    private TextView tvNoRequests;
    private RequestAdapter adapter;
    private List<RequestModel> requestList = new ArrayList<>();
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;
    private String currentEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();
        
        if (mAuth.getCurrentUser() != null) {
            currentEmail = mAuth.getCurrentUser().getEmail().toLowerCase().trim();
        }

        ImageView btnBack = findViewById(R.id.btnBackInbox);
        rvRequests = findViewById(R.id.rvRequests);
        tvNoRequests = findViewById(R.id.tvNoRequests);

        btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        loadRequests();
    }

    private void setupRecyclerView() {
        adapter = new RequestAdapter(requestList, new RequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(RequestModel request) {
                handleAccept(request);
            }

            @Override
            public void onDecline(RequestModel request) {
                handleDecline(request);
            }
        });
        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        rvRequests.setAdapter(adapter);
    }

    private void loadRequests() {
        if (currentEmail == null) return;

        mDB.collection("Users").document(currentEmail).collection("Requests")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    requestList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        RequestModel request = doc.toObject(RequestModel.class);
                        if (request != null) {
                            requestList.add(request);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    tvNoRequests.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    private void handleAccept(RequestModel request) {
        // 1. Add current user's UID to requester's 'following' list? 
        // Or requester's UID to current user's 'followers'?
        // Let's stick to: Requester (A) wants to follow Me (B).
        // On Accept: Add Me (B) to A's following list.
        
        mDB.collection("Users").document(request.getFromEmail())
                .update("following", FieldValue.arrayUnion(mAuth.getCurrentUser().getUid()))
                .addOnSuccessListener(aVoid -> {
                    // 2. Remove the request
                    deleteRequest(request);
                    Toast.makeText(this, "Accepted follow request", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to accept", Toast.LENGTH_SHORT).show());
    }

    private void handleDecline(RequestModel request) {
        deleteRequest(request);
        Toast.makeText(this, "Declined request", Toast.LENGTH_SHORT).show();
    }

    private void deleteRequest(RequestModel request) {
        mDB.collection("Users").document(currentEmail).collection("Requests")
                .document(request.getFromEmail())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    requestList.remove(request);
                    adapter.notifyDataSetChanged();
                    tvNoRequests.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }
}
