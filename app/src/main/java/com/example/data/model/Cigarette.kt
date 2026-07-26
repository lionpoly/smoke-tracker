package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "cigarettes")
data class Cigarette(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double, // Price per pack (calculated or direct)
    val packSize: Int = 20, // Quantity per pack, typically 20
    val priceType: String = "PACK", // "PACK" (单包零售价) or "CARTON" (单条零售价)
    val cartonPrice: Double = price * 10, // Price per carton (1 carton = 10 packs)
    val packsPerCarton: Int = 10, // Packs per carton, typically 10
    val ean: String = "", // EAN 商品条形码
    val image: String = "", // 香烟图片链接
    val tarAmount: String = "", // 焦油量 (例如: "10mg")
    val isActive: Boolean = false, // Currently selected/active cigarette
    val createdAt: Long = System.currentTimeMillis()
)

