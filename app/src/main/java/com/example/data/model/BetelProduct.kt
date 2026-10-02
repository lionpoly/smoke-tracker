package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "betel_products")
data class BetelProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val packPrice: Double,
    val piecesPerPack: Int,
    val isActive: Boolean = false
)
