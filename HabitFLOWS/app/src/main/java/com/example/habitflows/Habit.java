package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Habit extends AppCompatActivity {

    private FirebaseFirestore mDB;
    private FirebaseAuth mAuth;
    private EditText etHabitName, etDuration;
    private HabitAdapter adapter;
    private List<HabitModel> habitList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_habit);

        mAuth = FirebaseAuth.getInstance();
        mDB = FirebaseFirestore.getInstance();

        // Initialize UI
        etHabitName = findViewById(R.id.etHabitName);
        etDuration = findViewById(R.id.etDuration);
        ImageView btnBackProfile = findViewById(R.id.btnBackProfile);
        Button btnAddHabit = findViewById(R.id.btnAddHabit);
        RecyclerView rvHabits = findViewById(R.id.rvHabits);

        // Setup RecyclerView
        habitList = new ArrayList<>();
        adapter = new HabitAdapter(habitList, this::deleteHabit);
        rvHabits.setLayoutManager(new LinearLayoutManager(this));
        rvHabits.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Listen for habit changes in real-time
        listenForHabits();

        btnBackProfile.setOnClickListener(v -> {
            startActivity(new Intent(Habit.this, MainMenu.class));
            finish();
        });

        btnAddHabit.setOnClickListener(v -> {
            String name = etHabitName.getText().toString().trim();
            String dur = etDuration.getText().toString().trim();

            if (name.isEmpty() || dur.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            saveHabit(name, Integer.parseInt(dur));
        });
    }

    private void listenForHabits() {
        if (mAuth.getCurrentUser() == null) return;

        mDB.collection("Users")
                .document(mAuth.getCurrentUser().getEmail())
                .collection("Habits")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        habitList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            HabitModel model = doc.toObject(HabitModel.class);
                            if (model != null) {
                                habitList.add(model);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void saveHabit(String name, int duration) {
        HabitModel habit = new HabitModel(name, duration, "Days");
        mDB.collection("Users")
                .document(mAuth.getCurrentUser().getEmail())
                .collection("Habits")
                .document(name)
                .set(habit)
                .addOnSuccessListener(aVoid -> {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Habit Added!")
                            .setMessage("Habit saved successfully!")
                            .setPositiveButton("Awesome", null)
                            .show();
                    
                    etHabitName.setText("");
                    etDuration.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteHabit(HabitModel habit) {
        mDB.collection("Users")
                .document(mAuth.getCurrentUser().getEmail())
                .collection("Habits")
                .document(habit.getHabitName())
                .delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Habit deleted", Toast.LENGTH_SHORT).show());
    }
}
