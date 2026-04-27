package com.example.androidnativegrupo5.data.local.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteDao {
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertFavorite(FavoriteEntity favorite);
    
    @Delete
    void removeFavorite(FavoriteEntity favorite);
    
    @Query("SELECT * FROM favorites WHERE id = :favoriteId")
    FavoriteEntity getFavoriteById(long favoriteId);
    
    @Query("SELECT * FROM favorites WHERE activityId = :activityId")
    FavoriteEntity getFavoriteByActivityId(long activityId);
    
    @Query("SELECT * FROM favorites ORDER BY markedAt DESC")
    LiveData<List<FavoriteEntity>> getAllFavorites();
    
    @Query("SELECT * FROM favorites WHERE hasPriceChanged = 1 ORDER BY markedAt DESC")
    LiveData<List<FavoriteEntity>> getFavoritesWithPriceChanges();
    
    @Query("SELECT * FROM favorites WHERE hasAvailabilityChanged = 1 ORDER BY markedAt DESC")
    LiveData<List<FavoriteEntity>> getFavoritesWithAvailabilityChanges();
    
    @Query("SELECT * FROM favorites WHERE hasPriceChanged = 1 OR hasAvailabilityChanged = 1 ORDER BY markedAt DESC")
    LiveData<List<FavoriteEntity>> getFavoritesWithChanges();
    
    @Query("SELECT COUNT(*) FROM favorites WHERE activityId = :activityId")
    int isFavorite(long activityId);
    
    @Query("UPDATE favorites SET hasPriceChanged = 0 WHERE activityId = :activityId")
    void clearPriceChangeIndicator(long activityId);
    
    @Query("UPDATE favorites SET hasAvailabilityChanged = 0 WHERE activityId = :activityId")
    void clearAvailabilityChangeIndicator(long activityId);
    
    @Query("UPDATE favorites SET hasPriceChanged = :hasPriceChanged, updatedAt = :updatedAt WHERE activityId = :activityId")
    void updatePriceChangeStatus(long activityId, boolean hasPriceChanged, long updatedAt);
    
    @Query("UPDATE favorites SET hasAvailabilityChanged = :hasAvailabilityChanged, updatedAt = :updatedAt WHERE activityId = :activityId")
    void updateAvailabilityChangeStatus(long activityId, boolean hasAvailabilityChanged, long updatedAt);
    
    @Query("DELETE FROM favorites WHERE activityId = :activityId")
    void removeFavoriteByActivityId(long activityId);
    
    @Query("SELECT COUNT(*) FROM favorites")
    int getFavoritesCount();
}
