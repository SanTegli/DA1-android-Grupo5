package com.example.androidnativegrupo5.data.model;

public class UserPreferences {
    private String preferredCategory;
    private Integer maxPrice;
    private String preferredDestination;
    private String activityDuration;

    public UserPreferences() {}

    public UserPreferences(String preferredCategory, Integer maxPrice, String preferredDestination, String activityDuration) {
        this.preferredCategory = preferredCategory;
        this.maxPrice = maxPrice;
        this.preferredDestination = preferredDestination;
        this.activityDuration = activityDuration;
    }

    public String getPreferredCategory() { return preferredCategory; }
    public void setPreferredCategory(String preferredCategory) { this.preferredCategory = preferredCategory; }

    public Integer getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Integer maxPrice) { this.maxPrice = maxPrice; }

    public String getPreferredDestination() { return preferredDestination; }
    public void setPreferredDestination(String preferredDestination) { this.preferredDestination = preferredDestination; }

    public String getActivityDuration() { return activityDuration; }
    public void setActivityDuration(String activityDuration) { this.activityDuration = activityDuration; }
}
