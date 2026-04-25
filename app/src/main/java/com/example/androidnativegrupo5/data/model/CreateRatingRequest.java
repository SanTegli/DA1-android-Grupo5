package com.example.androidnativegrupo5.data.model;

public class CreateRatingRequest {
    private Integer activityScore;
    private Integer guideScore;
    private String comment;

    public CreateRatingRequest(Integer activityScore, Integer guideScore, String comment) {
        this.activityScore = activityScore;
        this.guideScore = guideScore;
        this.comment = comment;
    }

    public Integer getActivityScore() { return activityScore; }
    public void setActivityScore(Integer activityScore) { this.activityScore = activityScore; }
    public Integer getGuideScore() { return guideScore; }
    public void setGuideScore(Integer guideScore) { this.guideScore = guideScore; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
