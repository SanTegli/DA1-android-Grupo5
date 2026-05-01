package com.example.androidnativegrupo5.data.model;

public class AvailabilitySlotResponse {
    private String date;
    private String time;
    private int availableSlots;

    public AvailabilitySlotResponse() {}

    public AvailabilitySlotResponse(String date, String time, int availableSlots) {
        this.date = date;
        this.time = time;
        this.availableSlots = availableSlots;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(int availableSlots) { this.availableSlots = availableSlots; }
}
