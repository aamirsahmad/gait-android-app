package com.example.gaitanalyzer.infocard;

public class InfoCardData {
    private String userID;
    private String duration;
    private String filePath;

    private static InfoCardData instance;

    public static InfoCardData getInstance() {
        if (instance != null) {
            return instance;
        }

        instance = new InfoCardData();
        return instance;
    }

    private InfoCardData() {
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void clear() {
        userID = "";
        duration = "";
        filePath = "";
    }
}
