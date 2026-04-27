package com.example.androidnativegrupo5.di;

import android.content.Context;

import androidx.room.Room;

import com.example.androidnativegrupo5.data.local.db.AppDatabase;
import com.example.androidnativegrupo5.data.local.db.FavoriteDao;
import com.example.androidnativegrupo5.data.local.db.ReservaDao;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class StorageModule {

    @Provides @Singleton
    public AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, "reservas_db").build();
    }

    @Provides @Singleton
    public ReservaDao provideReservaDao(AppDatabase database) {
        return database.reservaDao();
    }

    @Provides @Singleton
    public FavoriteDao provideFavoriteDao(AppDatabase database) {
        return database.favoriteDao();
    }

}
