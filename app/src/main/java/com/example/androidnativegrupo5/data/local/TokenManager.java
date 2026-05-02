package com.example.androidnativegrupo5.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.androidnativegrupo5.data.model.UserResponse;
import com.google.gson.Gson;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class TokenManager {

    private SharedPreferences prefs;
    private final Gson gson;

    private static final String PREF_NAME = "auth";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_PROFILE = "user_profile";
    private static final String KEY_PROFILE_IMAGE_URI = "profile_image_uri";

    @Inject
    public TokenManager(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public String getToken() {
        String token = prefs.getString(KEY_TOKEN, null);
        Log.d("TOKEN_DEBUG", "Obteniendo token: " + token);
        return token;
    }

    public void saveToken(String token) {
        Log.d("TOKEN_DEBUG", "Guardando token: " + token);
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER_PROFILE).remove(KEY_PROFILE_IMAGE_URI).apply();
    }

    public void saveUserProfile(UserResponse user) {
        String json = gson.toJson(user);
        prefs.edit().putString(KEY_USER_PROFILE, json).apply();
    }

    public UserResponse getUserProfile() {
        String json = prefs.getString(KEY_USER_PROFILE, null);
        if (json == null) return null;
        return gson.fromJson(json, UserResponse.class);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply();
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean("biometric_enabled", false);
    }

    public void saveProfileImageUri(String uri) {
        prefs.edit().putString(KEY_PROFILE_IMAGE_URI, uri).apply();
    }

    public String getProfileImageUri() {
        return prefs.getString(KEY_PROFILE_IMAGE_URI, null);
    }
}
