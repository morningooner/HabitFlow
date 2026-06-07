package com.example.habitflows;

import com.google.firebase.Timestamp;

public class ActivityModel {
    private String userId;
    private String username;
    private String habitName;
    private Timestamp timestamp;
    private String userEmail;
    private String profileImageBase64;

    public ActivityModel() {
    }

    public ActivityModel(String userId, String username, String habitName, Timestamp timestamp, String userEmail, String profileImageBase64) {
        this.userId = userId;
        this.username = username;
        this.habitName = habitName;
        this.timestamp = timestamp;
        this.userEmail = userEmail;
        this.profileImageBase64 = profileImageBase64;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getHabitName() { return habitName; }
    public void setHabitName(String habitName) { this.habitName = habitName; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getProfileImageBase64() { return profileImageBase64; }
    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64; }
}
