package com.example.androidnativegrupo5.data.model;

import com.google.gson.annotations.SerializedName;

public class CreateReservationRequest {
    @SerializedName("activityId")
    private Long activityId;

    @SerializedName("participants")
    private Integer participants;

    @SerializedName("date")
    private String date;

    @SerializedName("time")
    private String time;

    @SerializedName("cardDetails")
    private CardDetails cardDetails;

    public CreateReservationRequest(Long activityId, Integer participants, String date, String time) {
        this.activityId = activityId;
        this.participants = participants;
        this.date = date;
        this.time = time;
    }

    public static class CardDetails {
        @SerializedName("cardNumber")
        private String cardNumber;
        @SerializedName("cardHolderName")
        private String cardHolderName;
        @SerializedName("expirationDate")
        private String expirationDate;
        @SerializedName("cvv")
        private String cvv;

        public CardDetails(String cardNumber, String cardHolderName, String expirationDate, String cvv) {
            this.cardNumber = cardNumber;
            this.cardHolderName = cardHolderName;
            this.expirationDate = expirationDate;
            this.cvv = cvv;
        }
    }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public Integer getParticipants() { return participants; }
    public void setParticipants(Integer participants) { this.participants = participants; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public CardDetails getCardDetails() { return cardDetails; }
    public void setCardDetails(CardDetails cardDetails) { this.cardDetails = cardDetails; }
}
