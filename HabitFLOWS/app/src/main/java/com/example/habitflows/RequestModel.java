package com.example.habitflows;

public class RequestModel {
    private String fromEmail;
    private String fromUsername;
    private String fromUid;
    private String fromProfileImage;

    public RequestModel() {}

    public RequestModel(String fromEmail, String fromUsername, String fromUid, String fromProfileImage) {
        this.fromEmail = fromEmail;
        this.fromUsername = fromUsername;
        this.fromUid = fromUid;
        this.fromProfileImage = fromProfileImage;
    }

    public String getFromEmail() { return fromEmail; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public String getFromUid() { return fromUid; }
    public void setFromUid(String fromUid) { this.fromUid = fromUid; }

    public String getFromProfileImage() { return fromProfileImage; }
    public void setFromProfileImage(String fromProfileImage) { this.fromProfileImage = fromProfileImage; }
}
