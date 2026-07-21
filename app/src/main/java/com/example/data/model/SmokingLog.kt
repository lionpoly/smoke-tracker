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
    val isShared: Boolean = false, // Kept for legacy backward compatibility
    val logType: String = if (isShared) "SHARED_OUT" else "SELF", // "SELF" (自购自抽), "SHARED_OUT" (社交递烟), "RECEIVED_IN" (社交接烟)
    val timestamp: Long = System.currentTimeMillis(),
    val cost: Double = 0.0,
    val note: String = "",
    val isDemo: Boolean = false // Flag for demo data mode
)

