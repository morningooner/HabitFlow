package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SignUpMenu extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_menu);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();

        TextView login = (TextView) findViewById(R.id.logInLinkBtn);
        EditText uname = (EditText) findViewById(R.id.signUpUsername);
        EditText emel = (EditText) findViewById(R.id.signUpEmail);
        EditText pwd = (EditText) findViewById(R.id.signUpPwd);
        Button signUp = (Button) findViewById(R.id.signUpBtn);

        login.setMovementMethod(LinkMovementMethod.getInstance());
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignUpMenu.this, LoginMenu.class);
                startActivity(intent);
                finish();
            }
        });

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = uname.getText().toString().trim();
                String email = emel.getText().toString().trim();
                String password = pwd.getText().toString().trim();

                if (username.isEmpty()) {
                    Toast.makeText(SignUpMenu.this, "Please enter username", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (email.isEmpty()) {
                    Toast.makeText(SignUpMenu.this, "Please enter email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.isEmpty()) {
                    Toast.makeText(SignUpMenu.this, "Please enter password", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 8) {
                    Toast.makeText(SignUpMenu.this, "Minimum password length must be 8 characters", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create user with Firebase Auth
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignUpMenu.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    if (user != null) {
                                        // 1. Update Firebase Auth Profile
                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(username)
                                                .build();

                                        user.updateProfile(profileUpdates);

                                        // 2. Create User Document in Firestore
                                        Map<String, Object> userData = new HashMap<>();
                                        userData.put("username", username);
                                        userData.put("email", email);
                                        userData.put("uid", user.getUid());
                                        userData.put("following", new ArrayList<String>());
                                        userData.put("overallProgress", 0);

                                        mDB.collection("Users").document(email)
                                                .set(userData)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(SignUpMenu.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                                    Intent intent = new Intent(SignUpMenu.this, LoginMenu.class);
                                                    startActivity(intent);
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(SignUpMenu.this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                } else {
                                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                                    Toast.makeText(SignUpMenu.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}
