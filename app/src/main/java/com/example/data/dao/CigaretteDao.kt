package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Cigarette
import kotlinx.coroutines.flow.Flow

@Dao
interface CigaretteDao {
    @Query("SELECT * FROM cigarettes ORDER BY name ASC")
    fun getAllCigarettes(): Flow<List<Cigarette>>

    @Query("SELECT * FROM cigarettes WHERE id = :id LIMIT 1")
    suspend fun getCigaretteById(id: Int): Cigarette?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCigarette(cigarette: Cigarette): Long

    @Update
    suspend fun updateCigarette(cigarette: Cigarette)

    @Delete
    suspend fun deleteCigarette(cigarette: Cigarette)
}
