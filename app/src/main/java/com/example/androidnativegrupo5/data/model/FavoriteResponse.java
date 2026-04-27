package com.example.androidnativegrupo5.data.model;

public class FavoriteResponse {
    private Long favoriteId;
    private Long id;
    private String name;
    private String description;
    private String destination;
    private String category;
    private String duration;
    private Double price;
    private Integer availableSlots;
    private String imageUrl;
    private Double priceWhenMarked;
    private Integer availableSlotsWhenMarked;
    private Boolean hasPriceChanged;
    private Boolean hasAvailabilityChanged;
    private Double priceDifference;
    private Integer availabilityDifference;
    private String markedAt;

    // Getters
    public Long getFavoriteId() { return favoriteId; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getDestination() { return destination; }
    public String getCategory() { return category; }
    public String getDuration() { return duration; }
    public Double getPrice() { return price; }
    public Integer getAvailableSlots() { return availableSlots; }
    public String getImageUrl() { return imageUrl; }
    public Double getPriceWhenMarked() { return priceWhenMarked; }
    public Integer getAvailableSlotsWhenMarked() { return availableSlotsWhenMarked; }
    public Boolean getHasPriceChanged() { return hasPriceChanged; }
    public Boolean getHasAvailabilityChanged() { return hasAvailabilityChanged; }
    public Double getPriceDifference() { return priceDifference; }
    public Integer getAvailabilityDifference() { return availabilityDifference; }
    public String getMarkedAt() { return markedAt; }
}
