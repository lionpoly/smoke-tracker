package com.example.data.repository

import com.example.data.dao.CigaretteDao
import com.example.data.dao.SmokingGoalDao
import com.example.data.dao.SmokingLogDao
import com.example.data.model.Cigarette
import com.example.data.model.SmokingGoal
import com.example.data.model.SmokingLog
import kotlinx.coroutines.flow.Flow

class SmokingRepository(
    private val cigaretteDao: CigaretteDao,
    private val smokingLogDao: SmokingLogDao,
    private val smokingGoalDao: SmokingGoalDao
) {
    val allCigarettes: Flow<List<Cigarette>> = cigaretteDao.getAllCigarettes()
    val allLogs: Flow<List<SmokingLog>> = smokingLogDao.getAllLogs()
    val activeGoal: Flow<SmokingGoal?> = smokingGoalDao.getActiveGoal()

    fun getLogsInTimeRange(startTime: Long, endTime: Long): Flow<List<SmokingLog>> {
        return smokingLogDao.getLogsInTimeRange(startTime, endTime)
    }

    suspend fun getCigaretteById(id: Int): Cigarette? {
        return cigaretteDao.getCigaretteById(id)
    }

    suspend fun getCigaretteByEan(ean: String): Cigarette? {
        return cigaretteDao.getCigaretteByEan(ean)
    }

    suspend fun insertCigarette(cigarette: Cigarette): Long {
        return cigaretteDao.insertCigarette(cigarette)
    }

    suspend fun updateCigarette(cigarette: Cigarette) {
        cigaretteDao.updateCigarette(cigarette)
    }

    suspend fun setActiveCigarette(id: Int) {
        cigaretteDao.clearActiveStatus()
        cigaretteDao.setActiveStatus(id)
    }

    suspend fun deleteCigarette(cigarette: Cigarette) {
        cigaretteDao.deleteCigarette(cigarette)
    }

    suspend fun insertLog(log: SmokingLog): Long {
        return smokingLogDao.insertLog(log)
    }

    suspend fun insertLogs(logs: List<SmokingLog>): List<Long> {
        return smokingLogDao.insertLogs(logs)
    }

    suspend fun deleteLog(log: SmokingLog) {
        smokingLogDao.deleteLog(log)
    }

    suspend fun deleteDemoLogs() {
        smokingLogDao.deleteDemoLogs()
    }

    suspend fun deleteAllLogs() {
        smokingLogDao.deleteAllLogs()
    }

    suspend fun insertGoal(goal: SmokingGoal): Long {
        val id = smokingGoalDao.insertGoal(goal)
        smokingGoalDao.deactivateOtherGoals(id.toInt())
        return id
    }

    suspend fun updateGoal(goal: SmokingGoal) {
        smokingGoalDao.updateGoal(goal)
    }
}
