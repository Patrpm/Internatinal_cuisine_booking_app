package com.example.sushibooking.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Booking::class], version = 1, exportSchema = false)
abstract class BookingDatabase : RoomDatabase() {
    abstract fun bookingDao(): BookingDao

    companion object {
        @Volatile private var INSTANCE: BookingDatabase? = null

        fun getDatabase(context: Context): BookingDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BookingDatabase::class.java,
                    "sushi_booking_database"
                ).build().also { INSTANCE = it }
            }
    }
}
