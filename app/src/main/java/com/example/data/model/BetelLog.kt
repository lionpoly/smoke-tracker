package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "betel_logs")
data class BetelLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val logType: String, // SELF, SHARED_OUT or RECEIVED_IN
    val cost: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isDemo: Boolean = false
)
