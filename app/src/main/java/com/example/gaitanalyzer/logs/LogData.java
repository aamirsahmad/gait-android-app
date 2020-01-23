package com.example.gaitanalyzer.logs;

public class LogData {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String space = "\t\t\t\t";
        String newLine = "\n\n";
        sb.append("index" + space + userID + newLine);
        sb.append("timeMs" + space + timeMs + newLine);
        sb.append("accX" + space + accX + newLine);
        sb.append("accY" + space + accY + newLine);
        sb.append("accZ" + space + accZ + newLine);
        sb.append("vSum" + space + vSum + newLine);
        sb.append(newLine);
        sb.append("queueSize" + space + queueSize + newLine);
        sb.append("dataPointsCollected" + space + dataPointsCollected + newLine);
        sb.append("collectionRateMs" + space + collectionRateMs + newLine);
        sb.append("ws messages" + space + messages + newLine);
        sb.append("frequency" + space + frequency + " Hz");
        return sb.toString();
    }
}
