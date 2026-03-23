package com.example.androidnativegrupo5.network;

import com.example.androidnativegrupo5.model.AuthResponse;
import com.example.androidnativegrupo5.model.LoginRequest;
import com.example.androidnativegrupo5.model.MessageResponse;
import com.example.androidnativegrupo5.model.OtpRequest;
import com.example.androidnativegrupo5.model.OtpVerifyRequest;
import com.example.androidnativegrupo5.model.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

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

}
