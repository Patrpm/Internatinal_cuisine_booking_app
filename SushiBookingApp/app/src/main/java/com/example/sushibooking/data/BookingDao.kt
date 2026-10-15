package com.example.sushibooking.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY id DESC")
    fun observeBookings(): Flow<List<Booking>>

    @Insert
    suspend fun insert(booking: Booking)

    @Query("DELETE FROM bookings WHERE id = :bookingId")
    suspend fun deleteById(bookingId: Int)
}
