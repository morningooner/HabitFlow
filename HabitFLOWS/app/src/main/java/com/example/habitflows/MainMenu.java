package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainMenu extends AppCompatActivity {
    private FirebaseAuth mAuth;
    Button btnProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu);

        mAuth = FirebaseAuth.getInstance();

        TextView mainMenuHomeTV4 = findViewById(R.id.mainMenuHomeTV4);
        Button mainMenuLogoutBtn = findViewById(R.id.mainMenuLogoutBtn);
        btnProfile = findViewById(R.id.Profile);

        mainMenuLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(MainMenu.this, LoginMenu.class);
                startActivity(intent);
                finish();
            }
        });

        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainMenu.this, Profile.class);
                startActivity(intent);
            }
        });

        mainMenuHomeTV4.setText(mAuth.getCurrentUser().getDisplayName());

    };
}