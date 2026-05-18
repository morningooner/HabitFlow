package com.example.habitflows;

import java.time.LocalDate;
// This is a POJO (Plain Old Java Object) for Firestore
public class HabitModel {
    private String habitName;
    private int duration;
    private String unit;
    private String startDate;

    // 1. Required empty constructor for Firestore
    public HabitModel() {}

    // 2. Constructor for your use
    public HabitModel(String habitName, int duration, String unit) {
        this.habitName = habitName;
        this.duration = duration;
        this.unit = unit;
        startDate = LocalDate.now().toString();
    }

    // 3. Getters and Setters (Firestore needs these to read/write data)
    public String getHabitName() { return habitName; }
    public void setHabitName(String habitName) { this.habitName = habitName; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

}