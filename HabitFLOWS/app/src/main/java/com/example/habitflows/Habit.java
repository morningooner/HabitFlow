package com.example.habitflows;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Habit extends AppCompatActivity {

    private FirebaseFirestore mDB;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_habit);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();

        ImageView btnBackProfile = findViewById(R.id.btnBackProfile);
        Button btnAddHabit = findViewById(R.id.btnAddHabit);
        EditText etHabitName = findViewById(R.id.etHabitName);
        EditText etDuration = findViewById(R.id.etDuration);


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnBackProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Habit.this, MainMenu.class);
                startActivity(intent);
            }
        });

        btnAddHabit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String habitName = etHabitName.getText().toString().trim();
                String durationText = etDuration.getText().toString().trim();

                if(habitName.isEmpty() || durationText.isEmpty()){
                    Toast.makeText(Habit.this, "Please fill all fields",
                            Toast.LENGTH_SHORT).show();

                    return;
                }

                int duration = Integer.parseInt(durationText);

                String userId = mAuth.getCurrentUser().getUid();

                HabitModel habitData = new HabitModel(habitName, duration, "Days");

                mDB.collection("Users")
                        .document(userId)
                        .collection("Habits")
                        .add(habitData)
                        .addOnSuccessListener(documentReference -> {

                            Toast.makeText(Habit.this, "Habit saved!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {

                            Toast.makeText(Habit.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                        });
            }
        });

    }
}