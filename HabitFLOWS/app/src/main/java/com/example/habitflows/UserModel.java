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
    private String profileImageBase64 = "";

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
        this.profileImageBase64 = "";
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

    public String getProfileImageBase64() { return profileImageBase64; }
    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64 != null ? profileImageBase64 : ""; }
}
