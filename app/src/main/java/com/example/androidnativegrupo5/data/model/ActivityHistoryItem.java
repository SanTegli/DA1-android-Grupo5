package com.example.androidnativegrupo5.data.model;

public class ActivityHistoryItem {

    private Long reservationId;
    private Long activityId;
    private String date;
    private String activityName;
    private String destination;
    private String guideName;
    private String duration;
    
    // Agregados para mostrar la calificación en el historial
    private Integer activityScore;
    private Integer guideScore;
    private String comment;

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

    public Integer getActivityScore() {
        return activityScore;
    }

    public Integer getGuideScore() {
        return guideScore;
    }

    public String getComment() {
        return comment;
    }
}