package com.example.androidnativegrupo5.data.model;

import com.google.gson.annotations.SerializedName;

public class ActivityHistoryItem {

    @SerializedName("reservationId")
    private Long reservationId;

    @SerializedName("activityId")
    private Long activityId;

    @SerializedName("date")
    private String date;

    @SerializedName("activityName")
    private String activityName;

    @SerializedName("destination")
    private String destination;

    @SerializedName("guideName")
    private String guideName;

    @SerializedName("duration")
    private String duration;

    @SerializedName("activityScore")
    private Integer activityScore;

    @SerializedName("guideScore")
    private Integer guideScore;

    @SerializedName("comment")
    private String comment;

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getGuideName() { return guideName; }
    public void setGuideName(String guideName) { this.guideName = guideName; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public Integer getActivityScore() { return activityScore; }
    public void setActivityScore(Integer activityScore) { this.activityScore = activityScore; }

    public Integer getGuideScore() { return guideScore; }
    public void setGuideScore(Integer guideScore) { this.guideScore = guideScore; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
