package com.example.androidnativegrupo5.data.model;

public class RescheduleReservationRequest {

    private String newDate;
    private String newTime;
    private int participants;

    public RescheduleReservationRequest(String newDate, String newTime, int participants) {
        this.newDate = newDate;
        this.newTime = newTime;
        this.participants = participants;
    }

    public String getNewDate() { return newDate; }
    public void setNewDate(String newDate) { this.newDate = newDate; }

    public String getNewTime() { return newTime; }
    public void setNewTime(String newTime) { this.newTime = newTime; }

    public int getParticipants() { return participants; }
    public void setParticipants(int participants) { this.participants = participants; }
}
