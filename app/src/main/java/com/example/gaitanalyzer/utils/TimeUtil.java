package com.example.gaitanalyzer.utils;

public class TimeUtil {
    public static String getReadableTime(long elapsedTimeInSeconds) {
        long hours = (int) (elapsedTimeInSeconds / 3600);
        elapsedTimeInSeconds = elapsedTimeInSeconds % 3600;
        return String.format("%d:%d:%02d", hours, elapsedTimeInSeconds / 60, elapsedTimeInSeconds % 60);
    }
}
