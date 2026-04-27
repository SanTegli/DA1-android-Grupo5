package com.example.androidnativegrupo5.data.network;

import com.example.androidnativegrupo5.data.model.Activity;
import com.example.androidnativegrupo5.data.model.ActivityHistoryItem;
import com.example.androidnativegrupo5.data.model.AuthResponse;
import com.example.androidnativegrupo5.data.model.AvailabilitySlotResponse;
import com.example.androidnativegrupo5.data.model.LoginRequest;
import com.example.androidnativegrupo5.data.model.CheckFavoriteResponse;
import com.example.androidnativegrupo5.data.model.FavoriteResponse;
import com.example.androidnativegrupo5.data.model.MessageResponse;
import com.example.androidnativegrupo5.data.model.OtpRequest;
import com.example.androidnativegrupo5.data.model.OtpVerifyRequest;
import com.example.androidnativegrupo5.data.model.PaginatedResponse;
import com.example.androidnativegrupo5.data.model.CreateRatingRequest;
import com.example.androidnativegrupo5.data.model.Rating;
import com.example.androidnativegrupo5.data.model.RatingStatsResponse;
import com.example.androidnativegrupo5.data.model.RegisterRequest;
import com.example.androidnativegrupo5.data.model.CreateReservationRequest;
import com.example.androidnativegrupo5.data.model.RescheduleReservationRequest;
import com.example.androidnativegrupo5.data.model.ReservationResponse;

import com.example.androidnativegrupo5.data.model.UserResponse;

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
    Call<PaginatedResponse<Activity>> getRecommendedActivities();

    @GET("/api/v1/activities/{id}")
    Call<Activity> getActivityById(@Path("id") Long id);

    @POST("/api/v1/reservations")
    Call<ReservationResponse> createReservation(@Body CreateReservationRequest request);

    @GET("/api/v1/activities/{id}/availability")
    Call<List<AvailabilitySlotResponse>> getAvailability(@Path("id") Long id);

    @GET("api/v1/reservations/me")
    Call<List<ReservationResponse>> getMyReservations();

    @PATCH("/api/v1/reservations/{id}/cancel")
    Call<Void> cancelReservation(@Path("id") Long id);

    @GET("/api/v1/users/me")
    Call<UserResponse> getMyProfile();

    @PUT("/api/v1/users/me")
    Call<UserResponse> updateProfile(@Body UserResponse user);

    @GET("api/v1/history")
    Call<List<ActivityHistoryItem>> getHistory(
            @Query("fromDate") String fromDate,
            @Query("toDate") String toDate,
            @Query("destination") String destination
    );

    @POST("/api/v1/ratings/activity/{activityId}")
    Call<Rating> createRating(
            @Path("activityId") Long activityId,
            @Body CreateRatingRequest request
    );

    @GET("/api/v1/ratings/activity/{activityId}")
    Call<List<Rating>> getRatingsByActivity(@Path("activityId") Long activityId);

    @GET("/api/v1/ratings/activity/{activityId}/stats")
    Call<RatingStatsResponse> getRatingStats(@Path("activityId") Long activityId);

    @GET("/api/v1/ratings/my-ratings")
    Call<List<Rating>> getMyRatings();

    @DELETE("/api/v1/ratings/{id}")
    Call<Void> deleteRating(@Path("id") Long id);

    @PUT("/api/v1/reservations/{id}/reschedule")
    Call<ReservationResponse> rescheduleReservation(
            @Path("id") Long id,
            @Body RescheduleReservationRequest request
    );

    @POST("/api/v1/favorites/{activityId}")
    Call<FavoriteResponse> addToFavorites(@Path("activityId") Long activityId);

    @DELETE("/api/v1/favorites/{activityId}")
    Call<Void> removeFromFavorites(@Path("activityId") Long activityId);

    @GET("/api/v1/favorites")
    Call<List<FavoriteResponse>> getMyFavorites();

    @GET("/api/v1/favorites/{activityId}/check")
    Call<CheckFavoriteResponse> checkFavoriteStatus(@Path("activityId") Long activityId);

    @PATCH("/api/v1/favorites/{favoriteId}/clear-indicators")
    Call<Void> clearChangeIndicators(@Path("favoriteId") Long favoriteId);
}
