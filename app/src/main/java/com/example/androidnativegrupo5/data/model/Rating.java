package com.example.androidnativegrupo5.data.model;

import com.google.gson.annotations.SerializedName;
import java.time.LocalDateTime;

public class Rating {
    private Long id;
    private Long userId;
    private String username;
    private String userProfileImageUrl;
    private Long activityId;
    private String activityName;
    private Integer activityScore;
    private Integer guideScore;
    private String comment;
    private String createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getUserProfileImageUrl() { return userProfileImageUrl; }
    public void setUserProfileImageUrl(String userProfileImageUrl) { this.userProfileImageUrl = userProfileImageUrl; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public Integer getActivityScore() { return activityScore; }
    public void setActivityScore(Integer activityScore) { this.activityScore = activityScore; }
    public Integer getGuideScore() { return guideScore; }
    public void setGuideScore(Integer guideScore) { this.guideScore = guideScore; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
