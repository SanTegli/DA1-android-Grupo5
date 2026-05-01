package com.example.androidnativegrupo5.data.local.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AvailabilityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CachedAvailability> slots);

    @Query("SELECT * FROM activity_availability WHERE activityId = :activityId")
    List<CachedAvailability> getAvailabilityByActivityId(Long activityId);

    @Query("DELETE FROM activity_availability WHERE activityId = :activityId")
    void deleteByActivityId(Long activityId);
}
