package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.BetelGoal
import com.example.data.model.BetelLog
import com.example.data.model.BetelProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface BetelDao {
    @Query("SELECT * FROM betel_products ORDER BY isActive DESC, name ASC")
    fun products(): Flow<List<BetelProduct>>

    @Query("SELECT * FROM betel_logs ORDER BY timestamp DESC")
    fun logs(): Flow<List<BetelLog>>

    @Query("SELECT * FROM betel_goals WHERE id = 1 LIMIT 1")
    fun goal(): Flow<BetelGoal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: BetelProduct): Long

    @Update
    suspend fun updateProduct(product: BetelProduct)

    @Query("UPDATE betel_products SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE betel_products SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: Int)

    @Transaction
    suspend fun setActive(id: Int) {
        clearActive()
        activate(id)
    }

    @Query("DELETE FROM betel_products WHERE id = :id")
    suspend fun deleteProduct(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BetelLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<BetelLog>)

    @Query("DELETE FROM betel_logs WHERE isDemo = 1")
    suspend fun deleteDemoLogs()

    @Query("DELETE FROM betel_logs WHERE id = :id")
    suspend fun deleteLog(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGoal(goal: BetelGoal)

    @Query("DELETE FROM betel_logs")
    suspend fun clearLogs()

    @Query("DELETE FROM betel_products")
    suspend fun clearProducts()

    @Query("DELETE FROM betel_goals")
    suspend fun clearGoals()
}
