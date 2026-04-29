package com.example.androidnativegrupo5.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites")
public class FavoriteActivity {
    @PrimaryKey
    @NonNull
    private Long id;
    private String name;
    private String destination;
    private double lastKnownPrice;
    private int lastKnownSlots;
    private String imageUrl;
    private boolean hasPriceChange;
    private boolean hasSlotChange;

    public FavoriteActivity(@NonNull Long id, String name, String destination, double lastKnownPrice, int lastKnownSlots, String imageUrl) {
        this.id = id;
        this.name = name;
        this.destination = destination;
        this.lastKnownPrice = lastKnownPrice;
        this.lastKnownSlots = lastKnownSlots;
        this.imageUrl = imageUrl;
    }

    @NonNull
    public Long getId() { return id; }
    public void setId(@NonNull Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public double getLastKnownPrice() { return lastKnownPrice; }
    public void setLastKnownPrice(double lastKnownPrice) { this.lastKnownPrice = lastKnownPrice; }

    public int getLastKnownSlots() { return lastKnownSlots; }
    public void setLastKnownSlots(int lastKnownSlots) { this.lastKnownSlots = lastKnownSlots; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isHasPriceChange() { return hasPriceChange; }
    public void setHasPriceChange(boolean hasPriceChange) { this.hasPriceChange = hasPriceChange; }

    public boolean isHasSlotChange() { return hasSlotChange; }
    public void setHasSlotChange(boolean hasSlotChange) { this.hasSlotChange = hasSlotChange; }
}
