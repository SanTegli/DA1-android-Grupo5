package com.example.androidnativegrupo5.model;

import com.google.gson.annotations.SerializedName;

public class CreateReservationRequest {
    @SerializedName("activityId")
    private long activityId;

    @SerializedName("participants")
    private int slots;

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    public CreateReservationRequest(Long activityId, Integer slots, String date, String time) {
        this.activityId = activityId;
        this.slots = slots;
        this.date = date;
        this.time = time;
    }

    // Getters and Setters
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public Integer getSlots() { return slots; }
    public void setSlots(Integer slots) { this.slots = slots; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}
