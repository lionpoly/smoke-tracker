package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "smoking_goals")
data class SmokingGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dailyLimit: Int = 0, // 0 means cold turkey or unlimited depending on targetQuitDate
    val startDate: Long = System.currentTimeMillis(),
    val targetQuitDate: Long? = null, // Optional target date to fully quit
    val monthlyBudget: Double? = null, // Optional monthly budget
    val isActive: Boolean = true
)
