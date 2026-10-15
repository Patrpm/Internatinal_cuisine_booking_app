package com.example.sushibooking.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val branch: String,
    val date: String,
    val time: String,
    val guests: Int
)
