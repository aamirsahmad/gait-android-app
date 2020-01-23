package com.example.gaitanalyzer.utils;

import android.content.Context;

import com.example.gaitanalyzer.R;

public class Defaults {
    Context context;

    private String refreshRate;
    private String hostname;
    private String port;
    private boolean stream;
    private String userId;

    public Defaults(Context context) {
        this.context = context;

        refreshRate = this.context.getResources().getString(R.string.refresh_rate);
        hostname = this.context.getResources().getString(R.string.hostname);
        port = this.context.getResources().getString(R.string.port);
        stream = Boolean.parseBoolean(this.context.getResources().getString(R.string.stream));
        userId = this.context.getResources().getString(R.string.userId);
    }

    public String getRefreshRate() {
        return refreshRate;
    }

    public String getHostname() {
        return hostname;
    }

    public String getPort() {
        return port;
    }

    public boolean getStream() {
        return stream;
    }

    public String getUserId() {
        return userId;
    }
}
