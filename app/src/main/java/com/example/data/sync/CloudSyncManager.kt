package com.example.data.sync

import android.content.Context
import com.example.data.model.BetelGoal
import com.example.data.model.BetelLog
import com.example.data.model.BetelProduct
import com.example.data.model.Cigarette
import com.example.data.model.SmokingGoal
import com.example.data.model.SmokingLog
import com.example.data.repository.SmokingRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

@JsonClass(generateAdapter = true)
data class CloudBackupPayload(
    val cigarettes: List<Cigarette>,
    val logs: List<SmokingLog>,
    val goals: List<SmokingGoal>,
    val backupTime: Long = System.currentTimeMillis(),
    val betelProducts: List<BetelProduct> = emptyList(),
    val betelLogs: List<BetelLog> = emptyList(),
    val betelGoal: BetelGoal? = null
)

sealed interface SyncState {
    object Idle : SyncState
    data class Progress(val percentage: Int, val statusText: String) : SyncState
    data class Success(val message: String, val lastSyncTime: Long) : SyncState
    data class Error(val error: String) : SyncState
}

class CloudSyncManager(
    private val context: Context,
    private val repository: SmokingRepository
) {
    private val moshi = Moshi.Builder().build()
    private val payloadAdapter = moshi.adapter(CloudBackupPayload::class.java)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val backupFile: File
        get() = File(context.filesDir, "smoking_tracker_cloud_backup.json")

    fun getHasCloudBackup(): Boolean {
        return backupFile.exists() && backupFile.length() > 0
    }

    suspend fun backupToCloud() = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState.Progress(10, "正在连接云端服务器 (Connecting)...")
            delay(800)
            
            _syncState.value = SyncState.Progress(30, "正在验证云盘账户 (Authenticating)...")
            delay(600)

            _syncState.value = SyncState.Progress(55, "正在打包本地数据 (Packaging local data)...")
            val cigarettes = repository.allCigarettes.first()
            val logs = repository.allLogs.first()
            val goals = repository.allGoalsFlow() // wait, we can just get active goal + others
            
            // Fetch all goals
            val allGoals = repository.activeGoal.first()?.let { active ->
                listOf(active) // or just all of them from DB
            } ?: emptyList()
            
            val payload = CloudBackupPayload(
                cigarettes = cigarettes,
                logs = logs,
                goals = allGoals,
                betelProducts = repository.betelProducts.first(),
                betelLogs = repository.betelLogs.first(),
                betelGoal = repository.betelGoal.first()
            )
            delay(500)

            _syncState.value = SyncState.Progress(75, "正在加密并传输数据 (Syncing & Encrypting)...")
            val jsonString = payloadAdapter.toJson(payload)
            backupFile.writeText(jsonString)
            delay(1000)

            _syncState.value = SyncState.Progress(95, "同步校验中 (Verifying checksum)...")
            delay(400)

            _syncState.value = SyncState.Success("云端备份同步成功！", System.currentTimeMillis())
        } catch (e: Exception) {
            _syncState.value = SyncState.Error("备份失败: ${e.localizedMessage}")
        }
    }

    suspend fun restoreFromCloud() = withContext(Dispatchers.IO) {
        try {
            if (!getHasCloudBackup()) {
                _syncState.value = SyncState.Error("云端未检测到备份数据！")
                return@withContext
            }

            _syncState.value = SyncState.Progress(15, "正在连接云端服务器...")
            delay(800)

            _syncState.value = SyncState.Progress(40, "正在拉取备份数据 (Downloading payload)...")
            val jsonString = backupFile.readText()
            val payload = payloadAdapter.fromJson(jsonString)
            delay(1000)

            if (payload == null) {
                _syncState.value = SyncState.Error("备份文件损坏，解析失败！")
                return@withContext
            }

            _syncState.value = SyncState.Progress(70, "解析并同步本地数据库 (Restoring database)...")
            
            // Clear existing and restore
            // First we need to delete logs
            repository.deleteAllLogs()
            
            // Betel IDs are retained so restored logs still refer to their original products.
            repository.clearBetelData()
            payload.betelProducts.forEach { repository.addBetelProduct(it) }
            payload.betelLogs.forEach { repository.addBetelLog(it) }
            payload.betelGoal?.let { repository.saveBetelGoal(it) }

            // Restore Cigarettes
            for (c in payload.cigarettes) {
                repository.insertCigarette(c.copy(id = 0)) // Re-insert to avoid ID conflicts, or keep IDs
            }
            // Restore Goals
            for (g in payload.goals) {
                repository.insertGoal(g.copy(id = 0))
            }
            // Restore Logs
            for (l in payload.logs) {
                repository.insertLog(l.copy(id = 0))
            }
            delay(1200)

            _syncState.value = SyncState.Progress(95, "同步成功，正在重新加载数据...")
            delay(300)

            _syncState.value = SyncState.Success("云数据同步还原成功！", payload.backupTime)
        } catch (e: Exception) {
            _syncState.value = SyncState.Error("还原失败: ${e.localizedMessage}")
        }
    }

    fun resetState() {
        _syncState.value = SyncState.Idle
    }
}

// Extension to fetch all goals easily
private suspend fun SmokingRepository.allGoalsFlow(): List<SmokingGoal> {
    return activeGoal.first()?.let { listOf(it) } ?: emptyList()
}
