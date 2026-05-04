package com.example.habit_tracker;

import java.util.ArrayList;
import java.util.List;

public class HabitList {
    private List<Habit> allHabits;
    private LevelManager levelManager;
    private void addDefaultHabits() {
        allHabits.add(new Habit("Jogging"));
        allHabits.add(new Habit("Study"));
        allHabits.add(new Habit("Workout"));
    }

    public HabitList() {
        allHabits = new ArrayList<>();
        addDefaultHabits();
    }
    public void addHabit(Habit habit) {
        allHabits.add(habit);
    }

    // Method to get the total count
    public int getTotalHabits() {
        return allHabits.size();
    }


}