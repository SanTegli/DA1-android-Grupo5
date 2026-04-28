package com.example.androidnativegrupo5.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Reserva.class, FavoriteActivity.class}, version = 3, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ReservaDao reservaDao();
    public abstract FavoriteDao favoriteDao();
}