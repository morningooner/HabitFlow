package com.example.habitflows;

import java.time.LocalDate;

public class HabitModel {
    private String habitName;
    private int duration;
    private String unit;
    private String startDate;
    private int completedDays;
    private boolean isTodayCompleted; // Field for checklist status

    public HabitModel() {}

    public HabitModel(String habitName, int duration, String unit) {
        this.habitName = habitName;
        this.duration = duration;
        this.unit = unit;
        this.startDate = LocalDate.now().toString();
        this.completedDays = 0;
        this.isTodayCompleted = false;
    }

    public String getHabitName() { return habitName; }
    public void setHabitName(String habitName) { this.habitName = habitName; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public int getCompletedDays() { return completedDays; }
    public void setCompletedDays(int completedDays) { this.completedDays = completedDays; }

    public boolean isTodayCompleted() { return isTodayCompleted; }
    public void setTodayCompleted(boolean todayCompleted) { isTodayCompleted = todayCompleted; }
}