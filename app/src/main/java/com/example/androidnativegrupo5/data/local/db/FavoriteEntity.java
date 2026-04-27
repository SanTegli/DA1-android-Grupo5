package com.example.androidnativegrupo5.data.local.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class FavoriteEntity {
    @PrimaryKey(autoGenerate = true)
    private long id = 0;
    
    private long activityId;
    private String activityName;
    private String description;
    private String destination;
    private String category;
    private String duration;
    private double price;
    private int availableSlots;
    private String imageUrl;
    
    // Campos de tracking de cambios
    private double priceWhenMarked;
    private int availableSlotsWhenMarked;
    
    private boolean hasPriceChanged = false;
    private boolean hasAvailabilityChanged = false;
    
    private long markedAt = System.currentTimeMillis();
    private long updatedAt = System.currentTimeMillis();

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getActivityId() { return activityId; }
    public void setActivityId(long activityId) { this.activityId = activityId; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(int availableSlots) { this.availableSlots = availableSlots; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public double getPriceWhenMarked() { return priceWhenMarked; }
    public void setPriceWhenMarked(double priceWhenMarked) { this.priceWhenMarked = priceWhenMarked; }
    public int getAvailableSlotsWhenMarked() { return availableSlotsWhenMarked; }
    public void setAvailableSlotsWhenMarked(int availableSlotsWhenMarked) { this.availableSlotsWhenMarked = availableSlotsWhenMarked; }
    public boolean isHasPriceChanged() { return hasPriceChanged; }
    public void setHasPriceChanged(boolean hasPriceChanged) { this.hasPriceChanged = hasPriceChanged; }
    public boolean isHasAvailabilityChanged() { return hasAvailabilityChanged; }
    public void setHasAvailabilityChanged(boolean hasAvailabilityChanged) { this.hasAvailabilityChanged = hasAvailabilityChanged; }
    public long getMarkedAt() { return markedAt; }
    public void setMarkedAt(long markedAt) { this.markedAt = markedAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
