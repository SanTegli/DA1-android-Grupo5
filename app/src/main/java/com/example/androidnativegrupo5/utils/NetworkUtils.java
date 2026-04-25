package com.example.androidnativegrupo5.utils;

import android.content.Context;
import android.net.ConnectivityManager;

public class NetworkUtils {

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        android.net.Network capabilities = cm.getActiveNetwork();
        if (capabilities == null) return false;

        android.net.NetworkCapabilities lp = cm.getNetworkCapabilities(capabilities);
        return lp != null && (lp.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                lp.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR));
    }
}
