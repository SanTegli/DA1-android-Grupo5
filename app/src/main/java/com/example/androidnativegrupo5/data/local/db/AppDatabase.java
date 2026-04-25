package com.example.androidnativegrupo5.data.local.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Reserva.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ReservaDao reservaDao();
}