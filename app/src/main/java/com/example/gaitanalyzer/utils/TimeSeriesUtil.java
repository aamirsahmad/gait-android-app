package com.example.gaitanalyzer.utils;

public class TimeSeriesUtil {
    public static int getFrequency(int collectionRateNs) {
        //(1/f)*1000 = ms
        double frequency = (1.0/ (double) collectionRateNs) * 1000000000.0;

        return (int) frequency;
    }
}
