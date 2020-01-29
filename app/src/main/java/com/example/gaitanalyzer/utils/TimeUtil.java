package com.example.gaitanalyzer.utils;

public class TimeUtil {
    public static String getReadableTime(long elapsedTime) {
        long hours = (int) (elapsedTime / 3600);
        elapsedTime = elapsedTime % 3600;
        return String.format("%d : %d : %02d", hours, elapsedTime / 60, elapsedTime % 60);
    }
}
