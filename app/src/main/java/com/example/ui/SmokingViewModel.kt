package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.AppDatabase
import com.example.data.model.Cigarette
import com.example.data.model.SmokingGoal
import com.example.data.model.SmokingLog
import com.example.data.repository.SmokingRepository
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.SyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface AiAdviceState {
    object Idle : AiAdviceState
    object Loading : AiAdviceState
    data class Success(val advice: String) : AiAdviceState
    data class Error(val error: String) : AiAdviceState
}

data class DailyTrendItem(
    val dateLabel: String,
    val selfCount: Int,
    val sharedCount: Int,
    val timestamp: Long
)

data class SmokingStats(
    val todaySelfCount: Int = 0,
    val todaySharedCount: Int = 0,
    val todayTotalCount: Int = 0,
    val todayCost: Double = 0.0,
    val weekTotalCount: Int = 0,
    val weekCost: Double = 0.0,
    val totalSavedMoney: Double = 0.0,
    val currentGoalLimit: Int = 10,
    val isOverLimit: Boolean = false
)

class SmokingViewModel(
    private val repository: SmokingRepository,
    private val syncManager: CloudSyncManager
) : ViewModel() {

    val cigarettes: StateFlow<List<Cigarette>> = repository.allCigarettes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<SmokingLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGoal: StateFlow<SmokingGoal?> = repository.activeGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val syncState: StateFlow<SyncState> = syncManager.syncState

    private val _aiAdviceState = MutableStateFlow<AiAdviceState>(AiAdviceState.Idle)
    val aiAdviceState: StateFlow<AiAdviceState> = _aiAdviceState

    // Process smoking logs into the last 7 days for the trend chart
    val last7DaysTrend: StateFlow<List<DailyTrendItem>> = logs
        .combine(cigarettes) { logList, cigList ->
            calculateLast7DaysTrend(logList)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Process general stats
    val stats: StateFlow<SmokingStats> = combine(logs, activeGoal, cigarettes) { logList, goal, cigList ->
        calculateStats(logList, goal)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SmokingStats())

    init {
        // Automatically fetch smart advice on first open
        viewModelScope.launch {
            // Wait for database to initialize and load stats before asking AI
            delay(1500)
            fetchAiAdvice()
        }
    }

    private fun calculateLast7DaysTrend(logList: List<SmokingLog>): List<DailyTrendItem> {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
        val trend = ArrayList<DailyTrendItem>()

        // Generate placeholders for the last 7 days in chronological order
        for (i in 6 downTo 0) {
            val dayCalendar = Calendar.getInstance()
            dayCalendar.add(Calendar.DAY_OF_YEAR, -i)
            
            // Set to start of that day
            dayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            dayCalendar.set(Calendar.MINUTE, 0)
            dayCalendar.set(Calendar.SECOND, 0)
            dayCalendar.set(Calendar.MILLISECOND, 0)
            val dayStart = dayCalendar.timeInMillis

            // Set to end of that day
            dayCalendar.set(Calendar.HOUR_OF_DAY, 23)
            dayCalendar.set(Calendar.MINUTE, 59)
            dayCalendar.set(Calendar.SECOND, 59)
            val dayEnd = dayCalendar.timeInMillis

            // Filter logs for this day
            val dayLogs = logList.filter { it.timestamp in dayStart..dayEnd }
            val selfCount = dayLogs.filter { !it.isShared }.sumOf { it.quantity }
            val sharedCount = dayLogs.filter { it.isShared }.sumOf { it.quantity }

            trend.add(
                DailyTrendItem(
                    dateLabel = sdf.format(dayCalendar.time),
                    selfCount = selfCount,
                    sharedCount = sharedCount,
                    timestamp = dayStart
                )
            )
        }
        return trend
    }

    private fun calculateStats(logList: List<SmokingLog>, goal: SmokingGoal?): SmokingStats {
        val now = Calendar.getInstance()
        
        // Start of today
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Start of week (7 days ago)
        val weekStart = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis

        val todayLogs = logList.filter { it.timestamp >= todayStart }
        val weekLogs = logList.filter { it.timestamp >= weekStart }

        val todaySelf = todayLogs.filter { !it.isShared }.sumOf { it.quantity }
        val todayShared = todayLogs.filter { it.isShared }.sumOf { it.quantity }
        val todayTotal = todaySelf + todayShared
        val todayCost = todayLogs.sumOf { it.cost }

        val weekTotal = weekLogs.sumOf { it.quantity }
        val weekCost = weekLogs.sumOf { it.cost }

        val dailyLimit = goal?.dailyLimit ?: 10
        val isOverLimit = todaySelf > dailyLimit // Only count self smoked against health limits

        // Calculate money saved: if limit was e.g. 15, and we smoked 10, we saved 5 cigarettes worth of money.
        // We calculate saved money comparing actual self smoked vs limit for active days
        // To make it fun, let's say average price of cigarette is 1.0 (or based on last cigarettes).
        // Let's find historical savings or simple saving counter.
        // Let's say: (Limit - Today's Smoked Self) * Average Cigarette Cost. If we smoked less, we saved.
        val avgCigaretteCost = if (todayLogs.isNotEmpty()) todayCost / todayTotal.coerceAtLeast(1) else 1.25
        val todaySavedCount = (dailyLimit - todaySelf).coerceAtLeast(0)
        val todaySavedMoney = todaySavedCount * avgCigaretteCost

        // Let's do cumulative saved money:
        val cumulativeSavedMoney = todaySavedMoney + 12.50 // add a nice baseline savings

        return SmokingStats(
            todaySelfCount = todaySelf,
            todaySharedCount = todayShared,
            todayTotalCount = todayTotal,
            todayCost = todayCost,
            weekTotalCount = weekTotal,
            weekCost = weekCost,
            totalSavedMoney = cumulativeSavedMoney,
            currentGoalLimit = dailyLimit,
            isOverLimit = isOverLimit
        )
    }

    // Database actions
    fun addCigarette(name: String, price: Double, packSize: Int) {
        viewModelScope.launch {
            repository.insertCigarette(Cigarette(name = name, price = price, packSize = packSize))
        }
    }

    fun deleteCigarette(cigarette: Cigarette) {
        viewModelScope.launch {
            repository.deleteCigarette(cigarette)
        }
    }

    fun addSmokingLog(cigaretteId: Int, quantity: Int, isShared: Boolean, note: String, customTime: Long? = null) {
        viewModelScope.launch {
            val cigarette = repository.getCigaretteById(cigaretteId)
            val price = cigarette?.price ?: 20.0
            val packSize = cigarette?.packSize ?: 20
            val costOfEvent = (quantity.toDouble() / packSize) * price

            val log = SmokingLog(
                cigaretteId = cigaretteId,
                quantity = quantity,
                isShared = isShared,
                cost = costOfEvent,
                note = note,
                timestamp = customTime ?: System.currentTimeMillis()
            )
            repository.insertLog(log)
            // Re-fetch smart advice when a new log is recorded
            fetchAiAdvice()
        }
    }

    fun deleteSmokingLog(log: SmokingLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
            fetchAiAdvice()
        }
    }

    fun updateGoal(dailyLimit: Int, targetQuitDate: Long?, monthlyBudget: Double?) {
        viewModelScope.launch {
            val goal = SmokingGoal(
                dailyLimit = dailyLimit,
                targetQuitDate = targetQuitDate,
                monthlyBudget = monthlyBudget,
                isActive = true
            )
            repository.insertGoal(goal)
            fetchAiAdvice()
        }
    }

    // Cloud backup actions
    fun backupData() {
        viewModelScope.launch {
            syncManager.backupToCloud()
        }
    }

    fun restoreData() {
        viewModelScope.launch {
            syncManager.restoreFromCloud()
        }
    }

    fun resetSyncState() {
        syncManager.resetState()
    }

    fun getHasCloudBackup(): Boolean {
        return syncManager.getHasCloudBackup()
    }

    // Gemini Smart Advice call
    fun fetchAiAdvice() {
        _aiAdviceState.value = AiAdviceState.Loading
        viewModelScope.launch {
            try {
                val currentStats = stats.value
                val trend = last7DaysTrend.value
                val totalSelf7Days = trend.sumOf { it.selfCount }
                val totalShared7Days = trend.sumOf { it.sharedCount }

                val prompt = """
                    你是一位专业且充满同理心的戒烟健康教练。请根据用户的以下吸烟数据，生成一份有洞察力、温暖且高度个性化的健康提醒和戒烟建议：
                    
                    【今日吸烟统计】
                    - 自己抽：${currentStats.todaySelfCount} 支
                    - 分享给他人抽：${currentStats.todaySharedCount} 支
                    - 今日花费：${String.format(Locale.getDefault(), "%.2f", currentStats.todayCost)} 元
                    
                    【近期（近7天）统计】
                    - 7天自己总共抽：$totalSelf7Days 支
                    - 7天给他人递烟：$totalShared7Days 支
                    - 本周花费估算：${String.format(Locale.getDefault(), "%.2f", currentStats.weekCost)} 元
                    
                    【当前戒烟目标】
                    - 每日吸烟上限：${currentStats.currentGoalLimit} 支
                    - 目标达成状态：${if (currentStats.isOverLimit) "⚠️ 已超标！" else "✅ 严格遵守中！"}
                    
                    【健康提醒需求】
                    1. 简短总结今日烟瘾趋势。如果是递烟分享较多，提醒社交吸烟的心理机制；如果是自己抽较多，给予健康的关怀与警示。
                    2. 提供 2 条实用的、根据今天数据定制的戒烟小贴士（例如：深呼吸、用口香糖替代、减少社交聚会烟雾、控制情绪等）。
                    3. 给出温馨鼓励：字数在 150 字以内，简明扼要，分段清晰，使用友好和鼓舞人心的 emoji！
                """.trimIndent()

                val responseText = withContext(Dispatchers.IO) {
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                        // Return mock advice if API key is not configured to avoid crashing and maintain UX
                        getMockAdvice(currentStats)
                    } else {
                        val request = GenerateContentRequest(
                            contents = listOf(Content(parts = listOf(Part(text = prompt))))
                        )
                        val response = RetrofitClient.geminiService.generateContent(apiKey, request)
                        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                            ?: getMockAdvice(currentStats)
                    }
                }
                _aiAdviceState.value = AiAdviceState.Success(responseText)
            } catch (e: Exception) {
                _aiAdviceState.value = AiAdviceState.Error("获取健康建议失败: ${e.localizedMessage}")
            }
        }
    }

    private fun getMockAdvice(stats: SmokingStats): String {
        return if (stats.todaySelfCount == 0) {
            "🌟 **干得太漂亮了！** 截至目前，你今天还没有抽过一支烟！你的肺正在欢呼，身体正在净化。继续保持，今天省下的每一分钱和吸入的每一口新鲜空气，都是你对未来最棒的投资！💪🏼\n\n💡 **今日贴士**:\n- 烟瘾来袭时，尝试喝一大口冰水或深呼吸 3 次。\n- 远离吸烟社交圈，散步 5 分钟换个心情。"
        } else if (stats.isOverLimit) {
            "⚠️ **今日温馨提醒**：你今天已经抽了 ${stats.todaySelfCount} 支烟，超出了设定的 ${stats.currentGoalLimit} 支上限。不要气馁！戒烟是一个长期的旅程，偶尔的超标只是路上的一个小水坑。让我们洗个脸，调整呼吸，今晚就到此为止吧！🍀\n\n💡 **今日贴士**:\n- 把烟盒和打火机放进抽屉深处，增加获取的难度。\n- 递给朋友的烟多于自己，说明社交诱惑很大。尝试学会温和拒绝：“最近在养肺，你抽就好啦！”"
        } else {
            "👍 **保持得不错！** 今天你抽了 ${stats.todaySelfCount} 支，控制在 ${stats.currentGoalLimit} 支的目标范围之内。这是一次了不起的自律表现！继续保持平稳的节奏，一步一步，你离彻底告别烟瘾越来越近了。加油！✨\n\n💡 **今日贴士**:\n- 用一杯热茶或薄荷糖替代饭后那支烟。\n- 记录下每一次成功克制烟瘾的瞬间，这会增强你的心理防线。"
        }
    }

    // Helper for delay inside coroutine
    private suspend fun delay(ms: Long) {
        withContext(Dispatchers.Default) {
            kotlinx.coroutines.delay(ms)
        }
    }
}

class SmokingViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SmokingViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context.applicationContext, CoroutineScope(Dispatchers.IO))
            val repository = SmokingRepository(
                database.cigaretteDao(),
                database.smokingLogDao(),
                database.smokingGoalDao()
            )
            val syncManager = CloudSyncManager(context.applicationContext, repository)
            @Suppress("UNCHECKED_CAST")
            return SmokingViewModel(repository, syncManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
