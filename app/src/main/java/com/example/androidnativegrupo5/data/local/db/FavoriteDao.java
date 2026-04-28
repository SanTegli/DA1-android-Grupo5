package com.example.androidnativegrupo5.data.local.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteActivity favorite);

    @Delete
    void delete(FavoriteActivity favorite);

    @Query("SELECT * FROM favorites")
    List<FavoriteActivity> getAllFavorites();

    @Query("SELECT EXISTS(SELECT * FROM favorites WHERE id = :activityId)")
    boolean isFavorite(Long activityId);

    @Query("SELECT * FROM favorites WHERE id = :activityId")
    FavoriteActivity getFavoriteById(Long activityId);

    @Update
    void update(FavoriteActivity favorite);
}
