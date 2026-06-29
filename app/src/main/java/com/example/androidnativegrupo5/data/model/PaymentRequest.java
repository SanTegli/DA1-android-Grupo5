package com.example.androidnativegrupo5.data.model;

import com.google.gson.annotations.SerializedName;

public class PaymentRequest {
    @SerializedName("reservationRequest")
    private CreateReservationRequest reservationRequest;
    
    @SerializedName("cardNumber")
    private String cardNumber;
    
    @SerializedName("expiryDate")
    private String expiryDate;
    
    @SerializedName("cvv")
    private String cvv;
    
    @SerializedName("cardHolder")
    private String cardHolder;

    public PaymentRequest(CreateReservationRequest reservationRequest, String cardNumber, String expiryDate, String cvv, String cardHolder) {
        this.reservationRequest = reservationRequest;
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.cvv = cvv;
        this.cardHolder = cardHolder;
    }

    // Getters and Setters if needed
}
