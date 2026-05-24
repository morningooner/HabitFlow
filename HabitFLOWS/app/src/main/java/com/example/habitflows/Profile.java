package com.example.habitflows;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;


import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class Profile extends AppCompatActivity {

    private ImageView ivProfile;
    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;
    private FirebaseStorage mStorage;
    private FirebaseUser currentUser;

    // Register the photo picker activity result launcher
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    // Update UI locally first
                    ivProfile.setImageURI(uri);
                    // Upload to Firebase Storage
                    uploadImageToFirebase(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();
        mStorage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        ivProfile = findViewById(R.id.ivProfile);
        ImageView ivBack = findViewById(R.id.ivBack);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Load existing profile picture from Firestore URL
        loadProfilePicture();

        ivBack.setOnClickListener(v -> {
            Intent intent = new Intent(Profile.this, MainMenu.class);
            startActivity(intent);
            finish();
        });

        ivProfile.setOnClickListener(v -> {
            // Launch photo picker
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
    }

    private void uploadImageToFirebase(Uri uri) {
        if (currentUser == null || currentUser.getEmail() == null) return;

        try {
            // 1. Open the image stream
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // 2. COMPRESS (Firestore has a 1MB limit, so we must shrink it)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            // We use 25% quality to ensure it stays small enough for the free database
            bitmap.compress(Bitmap.CompressFormat.JPEG, 25, baos);
            byte[] imageBytes = baos.toByteArray();

            // 3. Convert image bytes to a String
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            // 4. Save directly to Firestore (Free Tier)
            Map<String, Object> data = new HashMap<>();
            data.put("profileImageBase64", encodedImage);

            mDB.collection("Users").document(currentUser.getEmail())
                    .set(data, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(Profile.this, "Profile picture saved!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(Profile.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (Exception e) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
        }
    }

    /*private void saveImageUrlToFirestore(String url) {
        if (currentUser == null || currentUser.getEmail() == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("profileImageUrl", url);

        // Save URL to the user's document in Firestore
        mDB.collection("Users")
                .document(currentUser.getEmail())
                .set(data, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Profile.this, "Profile picture saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Profile.this, "Failed to save URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }*/

    private void loadProfilePicture() {
        if (currentUser == null || currentUser.getEmail() == null) return;

        mDB.collection("Users").document(currentUser.getEmail())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get the String we saved
                        String encodedImage = documentSnapshot.getString("profileImageBase64"); //Saved in b 64
                        if (encodedImage != null && !encodedImage.isEmpty()) {
                            // Convert the String back into an Image
                            byte[] decodedByte = Base64.decode(encodedImage, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
                            ivProfile.setImageBitmap(bitmap);
                        }
                    }
                });
    }
}
