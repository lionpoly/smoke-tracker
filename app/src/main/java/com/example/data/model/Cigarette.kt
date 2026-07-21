package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "cigarettes")
data class Cigarette(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double, // Price per pack
    val packSize: Int = 20, // Quantity per pack, typically 20
    val createdAt: Long = System.currentTimeMillis()
)
