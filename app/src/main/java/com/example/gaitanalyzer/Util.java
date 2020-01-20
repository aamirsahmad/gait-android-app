package com.example.gaitanalyzer;

public class Util {
    public static int getFrequency(int collectionRateMs) {
        float seconds = (float) collectionRateMs / 1000;
        float frequency = (float) 1 / seconds;

        return (int) frequency;
    }
}
