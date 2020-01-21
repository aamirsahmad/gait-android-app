package com.example.gaitanalyzer.utils;

public class TimeSeriesUtil {
    public static int getFrequency(int collectionRateMs) {
        float seconds = (float) collectionRateMs / 1000;
        float frequency = (float) 1 / seconds;

        return (int) frequency;
    }
}
