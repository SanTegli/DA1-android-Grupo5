package com.example.androidnativegrupo5.network;

import com.example.androidnativegrupo5.model.Activity;
import com.example.androidnativegrupo5.model.ActivityHistoryItem;
import com.example.androidnativegrupo5.model.AuthResponse;
import com.example.androidnativegrupo5.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.model.LoginRequest;
import com.example.androidnativegrupo5.model.MessageResponse;
import com.example.androidnativegrupo5.model.OtpRequest;
import com.example.androidnativegrupo5.model.OtpVerifyRequest;
import com.example.androidnativegrupo5.model.PaginatedResponse;
import com.example.androidnativegrupo5.model.CreateRatingRequest;
import com.example.androidnativegrupo5.model.Rating;
import com.example.androidnativegrupo5.model.RatingStatsResponse;
import com.example.androidnativegrupo5.model.RegisterRequest;
import com.example.androidnativegrupo5.model.CreateReservationRequest;
import com.example.androidnativegrupo5.model.ReservationResponse;

import com.example.androidnativegrupo5.model.UserResponse;

import java.util.List;

import retrofit2.http.DELETE;
import retrofit2.http.PATCH;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest request);

    @POST("auth/otp/request")
    Call<MessageResponse> requestOtp(@Body OtpRequest request);

    @POST("auth/otp/verify")
    Call<AuthResponse> verifyOtp(@Body OtpVerifyRequest request);

    @POST("auth/otp/resend")
    Call<MessageResponse> resendOtp(@Body OtpRequest request);

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest request);

    @GET("/api/v1/activities")
    Call<PaginatedResponse<Activity>> getActivities(
            @Query("page") int page,
            @Query("size") int size,
            @Query("category") String category,
            @Query("destination") String destination,
            @Query("minPrice") Integer minPrice,
            @Query("maxPrice") Integer maxPrice,
            @Query("search") String search
    );

    @GET("/api/v1/activities/recommended")
    Call<PaginatedResponse<Activity>> getRecommendedActivities(@Header("Authorization") String token);

    @GET("/api/v1/activities/{id}")
    Call<Activity> getActivityById(@Path("id") Long id);

    @POST("/api/v1/reservations")
    Call<ReservationResponse> createReservation(@Header("Authorization") String token, @Body CreateReservationRequest request);

    @GET("/api/v1/activities/{id}/availability")
    Call<List<AvailabilitySlotResponse>> getAvailability(@Path("id") Long id);

    @GET("api/v1/reservations/me")
    Call<List<ReservationResponse>> getMyReservations(@Header("Authorization") String token);

    @PATCH("/api/v1/reservations/{id}/cancel")
    Call<Void> cancelReservation(@Header("Authorization") String token, @Path("id") Long id);
    @GET("/api/v1/users/me")
    Call<UserResponse> getMyProfile(@Header("Authorization") String token);

    @PUT("/api/v1/users/me")
    Call<UserResponse> updateProfile(@Header("Authorization") String token, @Body UserResponse user);

    @GET("api/v1/history")
    Call<List<ActivityHistoryItem>> getHistory(
            @Query("fromDate") String fromDate,
            @Query("toDate") String toDate,
            @Query("destination") String destination
    );

    @POST("/api/v1/ratings/activity/{activityId}")
    Call<Rating> createRating(
            @Header("Authorization") String token,
            @Path("activityId") Long activityId,
            @Body CreateRatingRequest request
    );

    @GET("/api/v1/ratings/activity/{activityId}")
    Call<List<Rating>> getRatingsByActivity(@Path("activityId") Long activityId);

    @GET("/api/v1/ratings/activity/{activityId}/stats")
    Call<RatingStatsResponse> getRatingStats(@Path("activityId") Long activityId);

    @GET("/api/v1/ratings/my-ratings")
    Call<List<Rating>> getMyRatings(@Header("Authorization") String token);

    @DELETE("/api/v1/ratings/{id}")
    Call<Void> deleteRating(@Header("Authorization") String token, @Path("id") Long id);

}
