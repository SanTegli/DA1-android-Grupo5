package com.example.androidnativegrupo5.data.local.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "activity_availability")
public class CachedAvailability {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private Long activityId;
    private String date;
    private String time;
    private int availableSlots;

    public CachedAvailability() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(int availableSlots) { this.availableSlots = availableSlots; }
}
