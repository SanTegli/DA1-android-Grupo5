package com.example.androidnativegrupo5.network;

import com.example.androidnativegrupo5.model.Activity;
import com.example.androidnativegrupo5.model.AuthResponse;
import com.example.androidnativegrupo5.model.LoginRequest;
import com.example.androidnativegrupo5.model.MessageResponse;
import com.example.androidnativegrupo5.model.OtpRequest;
import com.example.androidnativegrupo5.model.OtpVerifyRequest;
import com.example.androidnativegrupo5.model.PaginatedResponse;
import com.example.androidnativegrupo5.model.RegisterRequest;

import java.util.List;

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
            @Query("size") int size
    );

    @GET("/api/v1/activities/{id}")
    Call<Activity> getActivityById(@Path("id") Long id);
}
