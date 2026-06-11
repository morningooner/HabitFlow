package com.example.habitflows;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.time.LocalDate;
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

        //Views for ANIMATION
        LinearLayout notificationContainer = findViewById(R.id.notificationContainer);

        // Initialize UI
        ImageView btnBackProfile = findViewById(R.id.btnBackProfile);
        FloatingActionButton fabAddHabit = findViewById(R.id.fabAddHabit);
        RecyclerView rvHabits = findViewById(R.id.rvHabits);

        // Setup RecyclerView
        habitList = new ArrayList<>();
        adapter = new HabitAdapter(habitList, this::showEditDialog, this::deleteHabit, this::updateHabitStatus);
        rvHabits.setLayoutManager(new LinearLayoutManager(this));
        rvHabits.setAdapter(adapter);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Check for daily reset before listening
        checkAndResetHabits();

        btnBackProfile.setOnClickListener(v -> {
            startActivity(new Intent(Habit.this, MainMenu.class));
            finish();
        });

        // Show creation dialog when FAB is clicked
        fabAddHabit.setOnClickListener(v -> showCreateHabitDialog());

        // Run the "Solo Leveling" System Entrance Animation
        SystemEntranceAnim.applySystemEntranceAnimation(btnBackProfile, notificationContainer, fabAddHabit);
    }

    private void checkAndResetHabits() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        String email = user.getEmail().toLowerCase().trim();
        String today = LocalDate.now().toString();

        mDB.collection("Users").document(email).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                UserModel userModel = doc.toObject(UserModel.class);
                if (userModel != null && !today.equals(userModel.getLastHabitResetDate())) {
                    mDB.collection("Users").document(email).collection("Habits").get().addOnSuccessListener(query -> {
                        WriteBatch batch = mDB.batch();
                        for (DocumentSnapshot habitDoc : query.getDocuments()) {
                            batch.update(habitDoc.getReference(), "todayCompleted", false);
                        }
                        batch.update(mDB.collection("Users").document(email), "lastHabitResetDate", today);
                        batch.commit().addOnSuccessListener(aVoid -> listenForHabits());
                    }).addOnFailureListener(e -> listenForHabits());
                } else {
                    listenForHabits();
                }
            } else {
                listenForHabits();
            }
        }).addOnFailureListener(e -> listenForHabits());
    }

    private void listenForHabits() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        mDB.collection("Users")
                .document(user.getEmail())
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

    private void updateHabitStatus(HabitModel habit, boolean isCompleted) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        String email = user.getEmail();

        mDB.collection("Users").document(email)
                .collection("Habits").document(habit.getHabitName())
                .update("todayCompleted", isCompleted)
                .addOnSuccessListener(aVoid -> {
                    if (isCompleted) {
                        checkAllHabitsCompleted();
                    }
                });
    }

    private void checkAllHabitsCompleted() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        String email = user.getEmail();
        String today = LocalDate.now().toString();

        mDB.collection("Users").document(email).get().addOnSuccessListener(userDoc -> {
            UserModel userModel = userDoc.toObject(UserModel.class);
            if (userModel != null && !today.equals(userModel.getLastStreakUpdateDate())) {
                
                mDB.collection("Users").document(email).collection("Habits").get().addOnSuccessListener(query -> {
                    boolean allDone = true;
                    for (DocumentSnapshot doc : query) {
                        HabitModel habit = doc.toObject(HabitModel.class);
                        if (habit != null && !habit.isTodayCompleted()) {
                            allDone = false;
                            break;
                        }
                    }

                    if (allDone && !query.isEmpty()) {
                        int newStreak = userModel.getStreak() + 1;
                        mDB.collection("Users").document(email)
                                .update("streak", newStreak, "lastStreakUpdateDate", today)
                                .addOnSuccessListener(aVoid -> Toast.makeText(this, "CONGRATULATIONS! Daily streak updated to " + newStreak + "!", Toast.LENGTH_LONG).show());
                    }
                });
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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;

        HabitModel habit = new HabitModel(name, duration, "Minutes");
        mDB.collection("Users")
                .document(user.getEmail())
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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        String userEmail = user.getEmail();
        
        if (!oldHabit.getHabitName().equals(newName)) {
            mDB.collection("Users").document(userEmail).collection("Habits").document(oldHabit.getHabitName()).delete();
        }

        HabitModel updatedHabit = new HabitModel(newName, newDuration, oldHabit.getUnit());
        updatedHabit.setStartDate(oldHabit.getStartDate());
        updatedHabit.setCompletedDays(oldHabit.getCompletedDays());
        updatedHabit.setTodayCompleted(oldHabit.isTodayCompleted());

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
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user == null || user.getEmail() == null) return;

                    mDB.collection("Users")
                            .document(user.getEmail())
                            .collection("Habits")
                            .document(habit.getHabitName())
                            .delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Habit deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
