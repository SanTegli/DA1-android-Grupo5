package com.example.androidnativegrupo5.network;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;

import dagger.hilt.android.qualifiers.ApplicationContext;

public class TokenManager {

    private SharedPreferences prefs;

    @Inject
    public TokenManager(@ApplicationContext Context context) {
        prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
    }

    public String getToken() {
        return prefs.getString("token", null);
    }

    public void saveToken(String token) {
        prefs.edit().putString("token", token).apply();
    }
}