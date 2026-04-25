package com.example.androidnativegrupo5.data.local.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.androidnativegrupo5.data.model.ReservationResponse;

@Entity(tableName = "reservas")
public class Reserva {

    @PrimaryKey
    @NonNull
    private Long id;

    private Long activityId;
    private String activityName;
    private Integer participants;
    private String date;
    private String time;
    private String status;
    private double totalPrice;
    private boolean pendingCancellation;

    public Reserva() {}

    public static Reserva fromResponse(ReservationResponse response) {
        Reserva local = new Reserva();
        local.setId(response.getId());
        local.setActivityId(response.getActivityId());
        local.setActivityName(response.getActivityName());
        local.setParticipants(response.getParticipants());
        local.setDate(response.getDate());
        local.setTime(response.getTime());
        local.setStatus(response.getStatus());
        local.setTotalPrice(response.getTotalPrice());
        return local;
    }

    @NonNull
    public Long getId() { return id; }
    public void setId(@NonNull Long id) { this.id = id; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public Integer getParticipants() { return participants; }
    public void setParticipants(Integer participants) { this.participants = participants; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public boolean isPendingCancellation() { return pendingCancellation; }
    public void setPendingCancellation(boolean pendingCancellation) { this.pendingCancellation = pendingCancellation; }

    public ReservationResponse toResponse() {
        ReservationResponse res = new ReservationResponse();
        res.setId(this.getId());
        res.setActivityName(this.getActivityName());
        res.setDate(this.getDate());
        res.setTime(this.getTime());
        res.setParticipants(this.getParticipants());
        res.setStatus(this.getStatus());
        res.setTotalPrice(this.getTotalPrice());
        return res;
    }
}