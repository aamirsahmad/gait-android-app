package com.example.gaitanalyzer.logs;

import android.text.Html;
import android.text.Spanned;

public class LogData {
    private static final String COLOR_PINK = "#CC6666";
    private static final String COLOR_YELLOW = "#FFCC66";
    private static final String NEW_LINE = "\n\n";


    // Sensor logs
    long index;
    String userID;
    long timeMs;
    double accX;
    double accY;
    double accZ;
    double vSum;

    // Internal Android logs
    int queueSize;
    long dataPointsCollected;
    int collectionRateMs;
    int messages;
    double frequency;

    String currentRecordingAbsolutePath;


    public LogData() {
    }

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public void setTimeMs(long timeMs) {
        this.timeMs = timeMs;
    }

    public double getAccX() {
        return accX;
    }

    public void setAccX(double accX) {
        this.accX = accX;
    }

    public double getAccY() {
        return accY;
    }

    public void setAccY(double accY) {
        this.accY = accY;
    }

    public double getAccZ() {
        return accZ;
    }

    public void setAccZ(double accZ) {
        this.accZ = accZ;
    }

    public double getvSum() {
        return vSum;
    }

    public void setvSum(double vSum) {
        this.vSum = vSum;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public long getDataPointsCollected() {
        return dataPointsCollected;
    }

    public void setDataPointsCollected(long dataPointsCollected) {
        this.dataPointsCollected = dataPointsCollected;
    }

    public int getCollectionRateMs() {
        return collectionRateMs;
    }

    public void setCollectionRateMs(int collectionRateMs) {
        this.collectionRateMs = collectionRateMs;
    }

    public int getMessages() {
        return messages;
    }

    public void setMessages(int messages) {
        this.messages = messages;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public String getCurrentRecordingAbsolutePath() {
        return currentRecordingAbsolutePath;
    }

    public void setCurrentRecordingAbsolutePath(String currentRecordingAbsolutePath) {
        this.currentRecordingAbsolutePath = currentRecordingAbsolutePath;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String space = "\t\t\t\t";
        sb.append("index=" + space + userID + NEW_LINE);
        sb.append("timeMs=" + space + timeMs + NEW_LINE);
        sb.append("accX=" + space + accX + NEW_LINE);
        sb.append("accY=" + space + accY + NEW_LINE);
        sb.append("accZ=" + space + accZ + NEW_LINE);
        sb.append("vSum=" + space + vSum + NEW_LINE);
        sb.append(NEW_LINE);
        sb.append("queueSize=" + space + queueSize + NEW_LINE);
        sb.append("dataPointsCollected=" + space + dataPointsCollected + NEW_LINE);
        sb.append("collectionRateMs=" + space + collectionRateMs + NEW_LINE);
        sb.append("ws messages=" + space + messages + NEW_LINE);
        sb.append("current file path=" + space + currentRecordingAbsolutePath + NEW_LINE);
        sb.append("frequency=" + space + frequency + " Hz");
        return sb.toString();
    }


    public Spanned toHTMLString() {
        String str = toString();
        StringBuilder stringBuilder = new StringBuilder();
        for (String line : str.split("\n")) {
            if (line.trim().equals("")) {
                continue;
            }
            String[] pairs = line.split("=");
            stringBuilder.append(getKeyColor(pairs[0]) + "  :  " + getValueColor(pairs[1]) + "<br><br>");
        }
        return Html.fromHtml(stringBuilder.toString());
    }


    private String getKeyColor(String text) {
        return getColoredSpanned(text, COLOR_PINK);
    }

    private String getValueColor(String text) {
        return getColoredSpanned(text, COLOR_YELLOW);
    }

    private String getColoredSpanned(String text, String color) {
        String input = "<font color=" + color + ">" + text + "</font>";
        return input;
    }
}
