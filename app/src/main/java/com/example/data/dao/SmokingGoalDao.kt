package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SmokingGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface SmokingGoalDao {
    @Query("SELECT * FROM smoking_goals WHERE isActive = 1 LIMIT 1")
    fun getActiveGoal(): Flow<SmokingGoal?>

    @Query("SELECT * FROM smoking_goals ORDER BY startDate DESC")
    fun getAllGoals(): Flow<List<SmokingGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SmokingGoal): Long

    @Update
    suspend fun updateGoal(goal: SmokingGoal)

    @Query("UPDATE smoking_goals SET isActive = 0 WHERE id != :activeGoalId")
    suspend fun deactivateOtherGoals(activeGoalId: Int)
}
