package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Habit extends AppCompatActivity {

    private FirebaseFirestore mDB;
    private FirebaseAuth mAuth;
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
        ImageView btnBackProfile = findViewById(R.id.btnBackProfile);
        FloatingActionButton fabAddHabit = findViewById(R.id.fabAddHabit);
        RecyclerView rvHabits = findViewById(R.id.rvHabits);

        // Setup RecyclerView
        habitList = new ArrayList<>();
        adapter = new HabitAdapter(habitList, this::showEditDialog, this::deleteHabit);
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

        // Show creation dialog when FAB is clicked
        fabAddHabit.setOnClickListener(v -> showCreateHabitDialog());
    }

    //Load habit everytime onCreate and everytime changed
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

    private void showCreateHabitDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_habit, null);
        EditText etName = dialogView.findViewById(R.id.etEditHabitName);
        EditText etDuration = dialogView.findViewById(R.id.etEditDuration);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Create New Habit")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String durStr = etDuration.getText().toString().trim();

                    if (name.isEmpty() || durStr.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveHabit(name, Integer.parseInt(durStr));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveHabit(String name, int duration) {
        HabitModel habit = new HabitModel(name, duration, "Days");
        mDB.collection("Users")
                .document(mAuth.getCurrentUser().getEmail())
                .collection("Habits")
                .document(name)
                .set(habit)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Habit added successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showEditDialog(HabitModel habit) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_habit, null);
        EditText editName = dialogView.findViewById(R.id.etEditHabitName);
        EditText editDuration = dialogView.findViewById(R.id.etEditDuration);

        editName.setText(habit.getHabitName());
        editDuration.setText(String.valueOf(habit.getDuration()));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Edit Habit")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = editName.getText().toString().trim();
                    String newDurStr = editDuration.getText().toString().trim();

                    if (newName.isEmpty() || newDurStr.isEmpty()) {
                        Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateHabitInFirebase(habit, newName, Integer.parseInt(newDurStr));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateHabitInFirebase(HabitModel oldHabit, String newName, int newDuration) {
        String userEmail = mAuth.getCurrentUser().getEmail();
        
        if (!oldHabit.getHabitName().equals(newName)) {
            mDB.collection("Users").document(userEmail).collection("Habits").document(oldHabit.getHabitName()).delete();
        }

        HabitModel updatedHabit = new HabitModel(newName, newDuration, oldHabit.getUnit());
        updatedHabit.setStartDate(oldHabit.getStartDate());
        updatedHabit.setCompletedDays(oldHabit.getCompletedDays());

        mDB.collection("Users")
                .document(userEmail)
                .collection("Habits")
                .document(newName)
                .set(updatedHabit)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Habit updated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteHabit(HabitModel habit) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Habit")
                .setMessage("Are you sure you want to delete '" + habit.getHabitName() + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    mDB.collection("Users")
                            .document(mAuth.getCurrentUser().getEmail())
                            .collection("Habits")
                            .document(habit.getHabitName())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Habit deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
