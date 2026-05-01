package com.example.androidnativegrupo5.data.local.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReservaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Reserva reserva);

    @Update
    void update(Reserva reserva);

    @Query("SELECT * FROM reservas ORDER BY date DESC")
    List<Reserva> getAllReservas();

    @Query("SELECT * FROM reservas WHERE id = :reservaId")
    Reserva getReservaById(Long reservaId);

    @Query("DELETE FROM reservas")
    void deleteAll();

    @Query("UPDATE reservas SET status = 'CANCELLED', pendingCancellation = 1 WHERE id = :reservaId")
    void markPendingCancellation(Long reservaId);

    @Query("SELECT * FROM reservas WHERE pendingCancellation = 1")
    List<Reserva> getPendingCancellations();

    @Query("UPDATE reservas SET pendingCancellation = 0 WHERE id = :reservaId")
    void clearPendingCancellation(Long reservaId);

    @Query("SELECT * FROM reservas WHERE pendingSync = 1")
    List<Reserva> getPendingSync();
    
}
