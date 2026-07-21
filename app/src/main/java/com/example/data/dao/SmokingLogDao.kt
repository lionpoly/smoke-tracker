package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SmokingLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SmokingLogDao {
    @Query("SELECT * FROM smoking_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SmokingLog>>

    @Query("SELECT * FROM smoking_logs WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    fun getLogsInTimeRange(startTime: Long, endTime: Long): Flow<List<SmokingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SmokingLog): Long

    @Delete
    suspend fun deleteLog(log: SmokingLog)

    @Query("DELETE FROM smoking_logs")
    suspend fun deleteAllLogs()
}
