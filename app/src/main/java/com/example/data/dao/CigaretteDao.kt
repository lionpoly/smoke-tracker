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
    @Query("SELECT * FROM cigarettes ORDER BY isActive DESC, name ASC")
    fun getAllCigarettes(): Flow<List<Cigarette>>

    @Query("SELECT * FROM cigarettes WHERE id = :id LIMIT 1")
    suspend fun getCigaretteById(id: Int): Cigarette?

    @Query("SELECT * FROM cigarettes WHERE ean = :ean AND ean != '' LIMIT 1")
    suspend fun getCigaretteByEan(ean: String): Cigarette?

    @Query("SELECT * FROM cigarettes WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveCigarette(): Cigarette?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCigarette(cigarette: Cigarette): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCigarettes(cigarettes: List<Cigarette>): List<Long>

    @Update
    suspend fun updateCigarette(cigarette: Cigarette)

    @Delete
    suspend fun deleteCigarette(cigarette: Cigarette)

    @Query("UPDATE cigarettes SET isActive = 0")
    suspend fun clearActiveStatus()

    @Query("UPDATE cigarettes SET isActive = 1 WHERE id = :id")
    suspend fun setActiveStatus(id: Int)
}

