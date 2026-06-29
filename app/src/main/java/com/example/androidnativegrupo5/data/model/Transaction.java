package com.example.androidnativegrupo5.data.model;

import com.google.gson.annotations.SerializedName;

public class Transaction {
    @SerializedName("id")
    private Long id;

    @SerializedName("amount")
    private double amount;

    @SerializedName("status")
    private String status;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("maskedCard")
    private String maskedCard;

    @SerializedName("reservationId")
    private Long reservationId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getMaskedCard() { return maskedCard; }
    public void setMaskedCard(String maskedCard) { this.maskedCard = maskedCard; }

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
}
