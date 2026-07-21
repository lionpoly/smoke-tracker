package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "smoking_logs")
data class SmokingLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cigaretteId: Int, // Refers to Cigarette.id
    val quantity: Int = 1,
    val isShared: Boolean = false, // false = self smoked, true = shared with others
    val timestamp: Long = System.currentTimeMillis(),
    val cost: Double = 0.0,
    val note: String = ""
)
