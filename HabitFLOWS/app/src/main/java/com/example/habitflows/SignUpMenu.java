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

public class SignUpMenu extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_menu);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        //userAuth = FirebaseAuth.getInstance().getCurrentUser();

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
                                    // Sign up success, update UI with the signed-in user's information
                                    FirebaseUser user = mAuth.getCurrentUser();

                                    // If user don't exist yet
                                    if (user != null) {
                                        // Store username in Firebase Profile
                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(username)
                                                .build();

                                        user.updateProfile(profileUpdates).addOnCompleteListener(updateTask -> {
                                            Toast.makeText(SignUpMenu.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(SignUpMenu.this, LoginMenu.class);
                                            startActivity(intent);
                                            finish();
                                        });
                                    }
                                } else {
                                    // Show Firebase error
                                    String errorMessage = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                                    Toast.makeText(SignUpMenu.this, "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}
