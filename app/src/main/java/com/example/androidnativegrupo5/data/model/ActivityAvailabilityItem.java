package com.example.androidnativegrupo5.data.model;

import com.google.gson.annotations.SerializedName;

public class ActivityAvailabilityItem {

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    @SerializedName("availableSlots")
    private int availableSlots;

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }
}
