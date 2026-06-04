package com.example.habitflows;

import java.util.ArrayList;
import java.util.List;

public class UserModel {
    private String username = "";
    private String email = "";
    private String uid = "";
    private List<String> following = new ArrayList<>();

    public UserModel() {
        // Required for Firestore
    }

    public UserModel(String username, String email, String uid) {
        this.username = username != null ? username : "";
        this.email = email != null ? email : "";
        this.uid = uid != null ? uid : "";
        this.following = new ArrayList<>();
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
}