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
    private String imageUrl;
    private boolean pendingCancellation;

    private boolean pendingSync;
    private String newDate;
    private String newTime;
    private Integer newParticipants;

    private String meetingPoint;
    private String guideName;
    private String voucherCode; 

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
        local.setImageUrl(response.getImageUrl());

        local.setMeetingPoint(response.getMeetingPointAddress() != null ? response.getMeetingPointAddress() : "Punto de encuentro a confirmar");
        local.setGuideName(response.getGuideName());
        local.setVoucherCode("VOU-" + response.getId());
        
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

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isPendingCancellation() { return pendingCancellation; }
    public void setPendingCancellation(boolean pendingCancellation) { this.pendingCancellation = pendingCancellation; }

    public boolean isPendingSync() { return pendingSync; }
    public void setPendingSync(boolean pendingSync) { this.pendingSync = pendingSync; }

    public String getNewDate() { return newDate; }
    public void setNewDate(String newDate) { this.newDate = newDate; }

    public String getNewTime() { return newTime; }
    public void setNewTime(String newTime) { this.newTime = newTime; }

    public Integer getNewParticipants() { return newParticipants; }
    public void setNewParticipants(Integer newParticipants) { this.newParticipants = newParticipants; }

    public String getMeetingPoint() { return meetingPoint; }
    public void setMeetingPoint(String meetingPoint) { this.meetingPoint = meetingPoint; }

    public String getGuideName() { return guideName; }
    public void setGuideName(String guideName) { this.guideName = guideName; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public ReservationResponse toResponse() {
        ReservationResponse res = new ReservationResponse();
        res.setId(this.getId());
        res.setActivityId(this.getActivityId());
        res.setActivityName(this.getActivityName());
        res.setImageUrl(this.getImageUrl());
        res.setDate(this.pendingSync ? this.newDate : this.getDate());
        res.setTime(this.pendingSync ? this.newTime : this.getTime());
        res.setParticipants(this.pendingSync ? this.newParticipants : this.getParticipants());
        res.setStatus(this.getStatus());
        res.setTotalPrice(this.getTotalPrice());
        res.setMeetingPointAddress(this.getMeetingPoint());
        res.setGuideName(this.getGuideName());
        res.setPendingSync(this.pendingSync || this.pendingCancellation);
        return res;
    }

}
