package com.example.androidnativegrupo5.data.model;

import com.google.gson.annotations.SerializedName;

public class Activity {
    private Long id;
    private String name;
    private String description;
    private String destination;
    private String category;
    private String duration;
    private double price;

    @SerializedName("guideName")
    private String guideName;

    @SerializedName("available_slots")
    private int availableSlots;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("average_rating")
    private Double averageRating;

    @SerializedName("rating_count")
    private Integer ratingCount;

    @SerializedName("meetingPointAddress")
    private String meetingPointAddress;

    @SerializedName("meetingPointLat")
    private Double meetingPointLat;

    @SerializedName("meetingPointLng")
    private Double meetingPointLng;

    public Activity() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDestination() {
        return destination;
    }

    public String getCategory() {
        return category;
    }

    public String getDuration() {
        return duration;
    }

    public double getPrice() {
        return price;
    }

    public String getGuideName() {
        return guideName;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public String getMeetingPointAddress() {
        return meetingPointAddress;
    }

    public Double getMeetingPointLat() {
        return meetingPointLat;
    }

    public Double getMeetingPointLng() {
        return meetingPointLng;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public void setGuideName(String guideName) {
        this.guideName = guideName;
    }

    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public void setMeetingPointAddress(String meetingPointAddress) {
        this.meetingPointAddress = meetingPointAddress;
    }

    public void setMeetingPointLat(Double meetingPointLat) {
        this.meetingPointLat = meetingPointLat;
    }

    public void setMeetingPointLng(Double meetingPointLng) {
        this.meetingPointLng = meetingPointLng;
    }
}