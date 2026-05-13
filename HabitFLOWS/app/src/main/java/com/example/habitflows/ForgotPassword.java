package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        //Firebase instance
        mAuth = mAuth.getInstance();

        Button forgetPwdBtn = findViewById(R.id.forgetPwdBtn);
        Button forgetPwdBtnLogin = findViewById(R.id.forgetPwdBtnLogin);
        EditText emailAddress = findViewById(R.id.forgetPwdEmail);

        forgetPwdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailAddressReset = emailAddress.getText().toString().trim();

                if (emailAddressReset.isEmpty()){
                    Toast.makeText(ForgotPassword.this, "Please enter email", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAuth.sendPasswordResetEmail(emailAddressReset).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPassword.this, "Reset link sent.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ForgotPassword.this, "Error detected.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });


        forgetPwdBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ForgotPassword.this, LoginMenu.class);
                startActivity(intent);
                finish();
            }
        });
    }
}