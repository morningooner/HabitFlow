package com.example.habitflows;

import java.util.ArrayList;
import java.util.List;

public class UserModel {
    private String username = "";
    private String email = "";
    private String uid = "";
    private List<String> following = new ArrayList<>();
    private int overallProgress = 0;
    private int xp = 0;
    private String rank = "F";
    private String profession = "";
    private String profileImageBase64 = "";
    private int streak = 0;
    private String lastStreakUpdateDate = "";
    private String lastHabitResetDate = "";

    public UserModel() {
        // Required for Firestore
    }

    public UserModel(String username, String email, String uid) {
        this.username = username != null ? username : "";
        this.email = email != null ? email : "";
        this.uid = uid != null ? uid : "";
        this.following = new ArrayList<>();
        this.overallProgress = 0;
        this.xp = 0;
        this.rank = "F";
        this.profession = "";
        this.profileImageBase64 = "";
        this.streak = 0;
        this.lastStreakUpdateDate = "";
        this.lastHabitResetDate = "";
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username != null ? username : ""; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email != null ? email : ""; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid != null ? uid : ""; }

    public List<String> getFollowing() { 
        return following != null ? following : new ArrayList<>(); 
    }
    public void setFollowing(List<String> following) { 
        this.following = following != null ? following : new ArrayList<>(); 
    }

    public int getOverallProgress() { return overallProgress; }
    public void setOverallProgress(int overallProgress) { this.overallProgress = overallProgress; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank != null ? rank : "F"; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession != null ? profession : ""; }

    public String getProfileImageBase64() { return profileImageBase64; }
    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64 != null ? profileImageBase64 : ""; }

    public int getStreak() { return streak; }
    public void setStreak(int streak) { this.streak = streak; }

    public String getLastStreakUpdateDate() { return lastStreakUpdateDate; }
    public void setLastStreakUpdateDate(String lastStreakUpdateDate) { this.lastStreakUpdateDate = lastStreakUpdateDate; }

    public String getLastHabitResetDate() { return lastHabitResetDate; }
    public void setLastHabitResetDate(String lastHabitResetDate) { this.lastHabitResetDate = lastHabitResetDate; }
}
