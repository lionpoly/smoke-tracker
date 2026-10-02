package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "betel_goals")
data class BetelGoal(
    @PrimaryKey val id: Int = 1,
    val dailyLimit: Int,
    val monthlyBudget: Double? = null
)
