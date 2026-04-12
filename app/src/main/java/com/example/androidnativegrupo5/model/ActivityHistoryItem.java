package com.example.androidnativegrupo5.model;

public class ActivityHistoryItem {

    private Long reservationId;
    private Long activityId;
    private String date;
    private String activityName;
    private String destination;
    private String guideName;
    private String duration;

    public Long getReservationId() {
        return reservationId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public String getDate() {
        return date;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getDestination() {
        return destination;
    }

    public String getGuideName() {
        return guideName;
    }

    public String getDuration() {
        return duration;
    }
}