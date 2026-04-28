package com.example.androidnativegrupo5.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class TokenManager {

    private SharedPreferences prefs;

    private static final String PREF_NAME = "auth";
    private static final String KEY_TOKEN = "token";

    @Inject
    public TokenManager(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply();
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean("biometric_enabled", false);
    }
}
