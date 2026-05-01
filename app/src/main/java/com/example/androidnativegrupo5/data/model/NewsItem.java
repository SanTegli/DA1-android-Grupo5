package com.example.androidnativegrupo5.data.model;

import com.google.gson.annotations.SerializedName;

public class NewsItem {

    private Long id;
    private String title;
    private String description;

    @SerializedName("imageUrl")
    private String imageUrl;

    private String linkUrl;
    private Long activityId;
    private String tag; 

    public NewsItem(Long id, String title, String description, String imageUrl, String linkUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getLinkUrl() { return linkUrl; }
    public Long getActivityId() { return activityId; }
    public String getTag() { return tag; }
    
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public void setTag(String tag) { this.tag = tag; }
}
