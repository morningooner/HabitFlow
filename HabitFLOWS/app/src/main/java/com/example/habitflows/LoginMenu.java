package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

public class LoginMenu extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;


    //More refined checking
    /*private void setupAuthStateListener() {
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    Intent intent = new Intent(LoginMenu.this, MainMenu.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(LoginMenu.this, "Login Failed", Toast.LENGTH_SHORT).show();
                }
            }
        };
    }*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_menu);

        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        EditText emel = (EditText) findViewById(R.id.logInEmail); // This field is used for email/username in layout
        EditText pwd = (EditText) findViewById(R.id.logInPassword);
        Button logInBtn = (Button) findViewById(R.id.logInButton);
        Button signUpLinkBtn = (Button) findViewById(R.id.signUpLinkBtn);
        TextView forgotPwd = (TextView) findViewById(R.id.tvForgotPwd);

        //Less refined checking
        //Check if current user already logged in, kinda like SharedPreference
        if(mAuth.getCurrentUser() != null) {
            Intent intent = new Intent(LoginMenu.this, MainMenu.class);
            startActivity(intent);
            finish();
        }

        signUpLinkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginMenu.this, SignUpMenu.class);
                startActivity(intent);
            }
        });

        logInBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emel.getText().toString().trim();
                String password = pwd.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(LoginMenu.this, "Please enter email", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(LoginMenu.this, "Please enter password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Sign in with Firebase Auth
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(LoginMenu.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginMenu.this, "Login Successful", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginMenu.this, MainMenu.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Login failure error
                                    Toast.makeText(LoginMenu.this, "Login Failed: " + task.getException().getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
        forgotPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginMenu.this, ForgotPassword.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
