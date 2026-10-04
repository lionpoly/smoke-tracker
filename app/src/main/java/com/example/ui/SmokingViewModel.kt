package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.OpenAiChatMessage
import com.example.data.api.OpenAiChatRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.AppDatabase
import com.example.data.model.BetelGoal
import com.example.data.model.BetelLog
import com.example.data.model.BetelProduct
import com.example.data.model.Cigarette
import com.example.data.model.SmokingGoal
import com.example.data.model.SmokingLog
import com.example.data.repository.SmokingRepository
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.SyncState
import com.example.ui.i18n.AppColorPreset
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

enum class AppFontFamily(
    val code: String,
    val labelZh: String,
    val labelEn: String,
    val fontFamily: androidx.compose.ui.text.font.FontFamily
) {
    DEFAULT("DEFAULT", "系统默认 (Sans-Serif)", "Default Sans-Serif", androidx.compose.ui.text.font.FontFamily.Default),
    SERIF("SERIF", "优雅衬线体 (Serif)", "Serif", androidx.compose.ui.text.font.FontFamily.Serif),
    MONOSPACE("MONOSPACE", "极客等宽 (Monospace)", "Monospace", androidx.compose.ui.text.font.FontFamily.Monospace),
    CURSIVE("CURSIVE", "手写圆体 (Cursive)", "Cursive", androidx.compose.ui.text.font.FontFamily.Cursive);

    fun getLabel(lang: AppLanguage = AppLanguage.ZH): String = if (lang == AppLanguage.EN) labelEn else labelZh
}

enum class AppCurrency(
    val code: String,
    val symbol: String,
    val flag: String,
    val nameZh: String,
    val nameEn: String
) {
    CNY("CNY", "¥", "🇨🇳", "人民币", "CN Yuan"),
    USD("USD", "$", "🇺🇸", "美元", "US Dollar"),
    EUR("EUR", "€", "🇪🇺", "欧元", "Euro"),
    JPY("JPY", "¥", "🇯🇵", "日元", "JP Yen"),
    GBP("GBP", "£", "🇬🇧", "英镑", "UK Pound"),
    HKD("HKD", "HK$", "🇭🇰", "港币", "HK Dollar"),
    TWD("TWD", "NT$", "🇹🇼", "新台币", "TW Dollar");

    fun getOptionLabel(lang: AppLanguage = AppLanguage.ZH): String {
        val name = if (lang == AppLanguage.EN) nameEn else nameZh
        return "$flag $symbol $code ($name)"
    }
}

enum class CigaretteSortOption(val code: String, val labelKey: String) {
    DEFAULT("DEFAULT", "sort_default"),
    NAME_ASC("NAME_ASC", "sort_name_asc"),
    NAME_DESC("NAME_DESC", "sort_name_desc"),
    PRICE_ASC("PRICE_ASC", "sort_price_asc"),
    PRICE_DESC("PRICE_DESC", "sort_price_desc")
}

enum class AiApiProtocol(val code: String, val labelZh: String, val labelEn: String) {
    OPENAI_COMPATIBLE("openai", "OpenAI 兼容协议 (支持各大厂商)", "OpenAI Compatible"),
    GOOGLE_GEMINI("gemini", "Google Gemini 原生协议", "Google Gemini")
}

enum class AiAdviceSource(val labelZh: String, val labelEn: String) {
    OPENAI_COMPATIBLE("在线大模型", "Cloud LLM"),
    GEMINI_CLOUD("Gemini 云端", "Gemini Cloud"),
    LOCAL_FALLBACK("离线保底", "Local Fallback")
}

enum class AiCoachPersona(
    val code: String,
    val labelZh: String,
    val labelEn: String,
    val promptInstruction: String
) {
    WARM(
        "warm",
        "温暖同理心 (推荐)",
        "Empathetic & Warm",
        "以温暖、包容、充满同理心且具鼓励性的健康教练口吻，肯定用户的微小进步与自律，用关怀方式提醒控烟，严禁生硬说教或指责。"
    ),
    STRICT(
        "strict",
        "自律严厉型",
        "Strict & Direct",
        "以严肃、强调自律与健康的口吻，直面吸烟与超标危害，督促用户坚守承诺，给出坚定切实的控制要求和克制动作。"
    ),
    ANALYTICAL(
        "analytical",
        "理性数据型",
        "Analytical & Objective",
        "以客观、理性的健康数据分析师口吻，基于开销、自抽/社交烟比例及目标偏差进行逻辑归纳与习惯打断策略建议。"
    )
}

data class AiAdviceResult(
    val advice: String,
    val source: AiAdviceSource,
    val modelName: String,
    val persona: AiCoachPersona = AiCoachPersona.WARM,
    val timestamp: Long = System.currentTimeMillis()
)

sealed interface AiAdviceState {
    object Idle : AiAdviceState
    data class Loading(val previous: AiAdviceResult? = null) : AiAdviceState
    data class Success(val result: AiAdviceResult) : AiAdviceState {
        val advice: String get() = result.advice
    }
    data class Error(val error: String, val previous: AiAdviceResult? = null) : AiAdviceState
}

data class AiCustomConfig(
    val enabled: Boolean = false,
    val protocol: AiApiProtocol = AiApiProtocol.OPENAI_COMPATIBLE,
    val apiKey: String = "",
    val baseUrl: String = "https://api.deepseek.com/v1",
    val modelName: String = "deepseek-chat",
    val persona: AiCoachPersona = AiCoachPersona.WARM
)

enum class TrendTimeRange(val labelZh: String, val labelEn: String) {
    LAST_7_DAYS("近7天", "7 Days"),
    THIS_WEEK("本周", "This Week"),
    THIS_MONTH("本月", "This Month"),
    SPECIFIC_MONTH("按月份", "By Month"),
    SPECIFIC_YEAR("按年份", "By Year"),
    CUSTOM_RANGE("自定义范围", "Custom Range");

    fun getLabel(lang: AppLanguage): String = if (lang == AppLanguage.EN) labelEn else labelZh
}

enum class ChartType(val labelZh: String, val labelEn: String) {
    BAR("柱状图", "Bar"),
    LINE("折线图", "Line"),
    SCATTER("散点图", "Scatter"),
    PIE("扇形图", "Pie");

    fun getLabel(lang: AppLanguage): String = if (lang == AppLanguage.EN) labelEn else labelZh
}

data class TrendDataItem(
    val dateLabel: String,
    val selfCount: Int = 0,
    val sharedCount: Int = 0,
    val receivedCount: Int = 0,
    val selfCost: Double = 0.0,
    val sharedCost: Double = 0.0,
    val receivedSaved: Double = 0.0,
    val timestamp: Long = 0L,
    val avgIntervalMinutes: Int = 0,
    val peakHourSlot: String = "无打卡"
)

enum class TrackingSubject { TOBACCO, BETEL }

data class SmokingStats(
    val todaySelfCount: Int = 0,
    val todaySharedCount: Int = 0,
    val todayReceivedCount: Int = 0,
    val todayTotalCount: Int = 0,
    val todayCost: Double = 0.0,
    val todaySavedFromReceived: Double = 0.0,
    val weekTotalCount: Int = 0,
    val weekCost: Double = 0.0,
    val monthTotalCount: Int = 0,
    val monthCost: Double = 0.0,
    val totalSelfCost: Double = 0.0,
    val totalSharedCost: Double = 0.0,
    val totalReceivedSaved: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalSavedMoney: Double = 0.0,
    val currentGoalLimit: Int = 10,
    val isOverLimit: Boolean = false
)

class SmokingViewModel(
    private val repository: SmokingRepository,
    private val syncManager: CloudSyncManager,
    private val context: Context? = null
) : ViewModel() {

    val trackingSubject = MutableStateFlow(TrackingSubject.TOBACCO)

    fun toggleTrackingSubject() {
        val next = if (trackingSubject.value == TrackingSubject.TOBACCO) TrackingSubject.BETEL else TrackingSubject.TOBACCO
        trackingSubject.value = next
        context?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            ?.edit()?.putString("tracking_subject", next.name)?.apply()
    }

    val appLanguage = MutableStateFlow(AppLanguage.ZH)
    val appThemeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    val appColorPreset = MutableStateFlow(AppColorPreset.DEFAULT)
    val appCurrency = MutableStateFlow(AppCurrency.CNY)
    val appFontFamily = MutableStateFlow(AppFontFamily.DEFAULT)

    fun setAppLanguage(lang: AppLanguage) {
        appLanguage.value = lang
        context?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            ?.edit()?.putString("language", lang.code)?.apply()
    }

    fun setAppThemeMode(mode: AppThemeMode) {
        appThemeMode.value = mode
        context?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            ?.edit()?.putString("theme_mode", mode.code)?.apply()
    }

    fun setAppColorPreset(preset: AppColorPreset) {
        appColorPreset.value = preset
        context?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            ?.edit()?.putString("color_preset", preset.code)?.apply()
    }

    fun setAppCurrency(currency: AppCurrency) {
        appCurrency.value = currency
        context?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            ?.edit()?.putString("currency_code", currency.code)?.apply()
    }

    fun setAppFontFamily(font: AppFontFamily) {
        appFontFamily.value = font
        context?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            ?.edit()?.putString("font_family", font.code)?.apply()
    }

    val betelProducts = repository.betelProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val betelLogs = repository.betelLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val betelGoal = repository.betelGoal.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val isBetelDemoMode = repository.betelLogs.map { logs -> logs.any { it.isDemo } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleBetelDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            // Only sample rows are removed; personal records and products are never touched.
            repository.deleteBetelDemoLogs()
            if (enabled) {
                val product = repository.betelProducts.firstOrNull().orEmpty()
                    .firstOrNull { it.isActive } ?: repository.betelProducts.firstOrNull().orEmpty().firstOrNull()
                val unitPrice = product?.let { it.packPrice / it.piecesPerPack.coerceAtLeast(1) } ?: 2.0
                val demoLogs = buildList {
                    val now = System.currentTimeMillis()
                    for (offset in 59 downTo 0) {
                        for ((hour, type) in listOf(9 to "SELF", 13 to "SELF", 16 to "RECEIVED_IN", 20 to "SHARED_OUT")) {
                            val time = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, -offset)
                                set(Calendar.HOUR_OF_DAY, hour)
                                set(Calendar.MINUTE, 10 + (offset * 7 + hour) % 40)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            if (time <= now) add(BetelLog(
                                productId = product?.id ?: 0,
                                productName = product?.name ?: "演示槟榔",
                                quantity = if (type == "SELF" && offset % 3 == 0) 2 else 1,
                                logType = type,
                                cost = if (type == "RECEIVED_IN") 0.0 else unitPrice * (if (type == "SELF" && offset % 3 == 0) 2 else 1),
                                timestamp = time,
                                isDemo = true
                            ))
                        }
                    }
                }
                repository.addBetelLogs(demoLogs)
            }
        }
    }

    fun addBetelProduct(name: String, price: Double, piecesPerPack: Int) {
        if (name.isBlank() || !price.isFinite() || price < 0 || piecesPerPack <= 0) return
        viewModelScope.launch {
            val first = repository.betelProducts.firstOrNull().orEmpty().isEmpty()
            repository.addBetelProduct(BetelProduct(name = name.trim(), packPrice = price, piecesPerPack = piecesPerPack, isActive = first))
        }
    }

    fun updateBetelProduct(product: BetelProduct, name: String, price: Double, piecesPerPack: Int) {
        if (name.isBlank() || !price.isFinite() || price < 0 || piecesPerPack <= 0) return
        viewModelScope.launch { repository.updateBetelProduct(product.copy(name = name.trim(), packPrice = price, piecesPerPack = piecesPerPack)) }
    }

    fun setActiveBetelProduct(id: Int) = viewModelScope.launch { repository.setActiveBetelProduct(id) }

    fun deleteBetelProduct(id: Int) = viewModelScope.launch { repository.deleteBetelProduct(id) }

    fun addBetelLog(product: BetelProduct, quantity: Int, logType: String) {
        if (quantity <= 0 || logType !in listOf("SELF", "SHARED_OUT", "RECEIVED_IN")) return
        viewModelScope.launch {
            val cost = if (logType == "RECEIVED_IN") 0.0 else quantity * product.packPrice / product.piecesPerPack.coerceAtLeast(1)
            repository.addBetelLog(BetelLog(productId = product.id, productName = product.name, quantity = quantity, logType = logType, cost = cost))
        }
    }

    fun deleteBetelLog(id: Int) = viewModelScope.launch { repository.deleteBetelLog(id) }

    fun saveBetelGoal(dailyLimit: Int, monthlyBudget: Double?) {
        if (dailyLimit < 0 || (monthlyBudget != null && (!monthlyBudget.isFinite() || monthlyBudget < 0))) return
        viewModelScope.launch { repository.saveBetelGoal(BetelGoal(dailyLimit = dailyLimit, monthlyBudget = monthlyBudget)) }
    }

    val cigarettes: StateFlow<List<Cigarette>> = repository.allCigarettes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<SmokingLog>> = repository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayLogs: StateFlow<List<SmokingLog>> = logs.map { logList ->
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        logList.filter { it.timestamp in todayStart..todayEnd }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGoal: StateFlow<SmokingGoal?> = repository.activeGoal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val syncState: StateFlow<SyncState> = syncManager.syncState

    private val _aiAdviceState = MutableStateFlow<AiAdviceState>(AiAdviceState.Idle)
    val aiAdviceState: StateFlow<AiAdviceState> = _aiAdviceState

    val aiCustomConfig = MutableStateFlow(AiCustomConfig())

    fun updateAiConfig(config: AiCustomConfig) {
        aiCustomConfig.value = config
        context?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)?.edit()?.apply {
            putBoolean("ai_custom_enabled", config.enabled)
            putString("ai_custom_protocol", config.protocol.code)
            putString("ai_custom_api_key", config.apiKey)
            putString("ai_custom_base_url", config.baseUrl)
            putString("ai_custom_model_name", config.modelName)
            putString("ai_custom_persona", config.persona.code)
            apply()
        }
        fetchAiAdvice(force = true)
    }

    fun resetAiConfig() {
        val defaultConfig = AiCustomConfig()
        updateAiConfig(defaultConfig)
    }

    suspend fun testAiConnection(
        protocol: AiApiProtocol,
        apiKey: String,
        baseUrl: String,
        model: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (protocol == AiApiProtocol.OPENAI_COMPATIBLE) {
                    val keyToUse = apiKey.trim()
                    if (keyToUse.isBlank()) {
                        return@withContext Result.failure(Exception("请先填写有效的 API Key"))
                    }
                    val targetUrl = RetrofitClient.normalizeOpenAiUrl(baseUrl.ifBlank { "https://api.deepseek.com/v1" })
                    val targetModel = model.ifBlank { "deepseek-chat" }.trim()
                    val bearerAuth = if (keyToUse.startsWith("Bearer ", ignoreCase = true)) keyToUse else "Bearer $keyToUse"
                    val startTime = System.currentTimeMillis()
                    val req = OpenAiChatRequest(
                        model = targetModel,
                        messages = listOf(
                            OpenAiChatMessage(role = "user", content = "Hello! Please reply 'OK' only.")
                        )
                    )
                    val resp = RetrofitClient.openAiService.chatCompletions(targetUrl, bearerAuth, req)
                    val elapsed = System.currentTimeMillis() - startTime
                    val reply = resp.choices?.firstOrNull()?.message?.content?.trim()
                    if (!reply.isNullOrBlank()) {
                        Result.success("连通成功！耗时 ${elapsed}ms，模型返回: $reply")
                    } else {
                        val err = resp.error?.message ?: "接口未返回有效内容，请检查模型名称或端点地址"
                        Result.failure(Exception(err))
                    }
                } else {
                    val keyToUse = if (apiKey.isNotBlank()) apiKey.trim() else BuildConfig.GEMINI_API_KEY
                    if (keyToUse.isBlank() || keyToUse == "MY_GEMINI_API_KEY") {
                        return@withContext Result.failure(Exception("请先填写有效的 Gemini API Key"))
                    }
                    val targetBaseUrl = if (baseUrl.isNotBlank()) baseUrl.trim() else RetrofitClient.DEFAULT_BASE_URL
                    val targetModel = if (model.isNotBlank()) model.trim() else "gemini-3.5-flash"
                    val testService = RetrofitClient.getGeminiService(targetBaseUrl)
                    val startTime = System.currentTimeMillis()
                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = "Hello! Please reply with 'OK'.")))),
                    )
                    val response = testService.generateContent(targetModel, keyToUse, request)
                    val elapsed = System.currentTimeMillis() - startTime
                    val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                    Result.success("连通成功！耗时 ${elapsed}ms，模型返回: $reply")
                }
            } catch (e: Exception) {
                Result.failure(Exception("连接失败: ${e.localizedMessage ?: e.message}"))
            }
        }
    }

    val selectedTimeRange = MutableStateFlow(TrendTimeRange.LAST_7_DAYS)
    val selectedChartType = MutableStateFlow(ChartType.BAR)
    val selectedMonthYear = MutableStateFlow<Calendar>(Calendar.getInstance())
    val customStartDate = MutableStateFlow<Long?>(null)
    val customEndDate = MutableStateFlow<Long?>(null)

    val maxIntervalThresholdHours = MutableStateFlow(6)

    fun setMaxIntervalThresholdHours(hours: Int) {
        val valid = hours.coerceIn(1, 24)
        maxIntervalThresholdHours.value = valid
        context?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            ?.edit()?.putInt("max_interval_hours", valid)?.apply()
    }

    val cigaretteSortOption = MutableStateFlow(CigaretteSortOption.DEFAULT)

    fun setCigaretteSortOption(option: CigaretteSortOption) {
        cigaretteSortOption.value = option
    }

    val cigaretteJsonUrl = MutableStateFlow("https://raw.githubusercontent.com/example/cigarettes/main/cigarettes.json")
    val isCigaretteSyncing = MutableStateFlow(false)
    val cigaretteSyncResult = MutableStateFlow<String?>(null)

    init {
        val savedUrl = context?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            ?.getString("cigarette_json_url", null)
        if (!savedUrl.isNullOrBlank()) {
            cigaretteJsonUrl.value = savedUrl
        }
        viewModelScope.launch {
            val existing = repository.allCigarettes.firstOrNull() ?: emptyList()
            val defaultNames = listOf("白沙", "黄果树", "双喜")
            val hasNewBrands = existing.any { cig -> defaultNames.any { brand -> cig.name.contains(brand) } }
            if (existing.isEmpty() || !hasNewBrands) {
                seedDefaultCigarettes()
            }
        }
    }

    fun seedDefaultCigarettes() {
        viewModelScope.launch {
            val existing = repository.allCigarettes.firstOrNull() ?: emptyList()
            val defaults = listOf(
                Cigarette(name = "中华 (软中华 / Soft Chunghwa)", price = 65.0, packSize = 20, priceType = "PACK", cartonPrice = 650.0, packsPerCarton = 10, ean = "6901028000018", image = "", tarAmount = "11mg", isActive = existing.none { it.isActive }),
                Cigarette(name = "炫赫门 (南京细支 / Xuanhemen)", price = 18.0, packSize = 20, priceType = "PACK", cartonPrice = 180.0, packsPerCarton = 10, ean = "6901028190016", image = "", tarAmount = "8mg", isActive = false),
                Cigarette(name = "万宝路 (薄荷双爆 / Marlboro Double Burst)", price = 30.0, packSize = 20, priceType = "PACK", cartonPrice = 300.0, packsPerCarton = 10, ean = "7622210000015", image = "", tarAmount = "8mg", isActive = false),
                Cigarette(name = "白沙 (硬精品 / Baisha Fine Hard)", price = 11.0, packSize = 20, priceType = "PACK", cartonPrice = 110.0, packsPerCarton = 10, ean = "6901028113008", image = "", tarAmount = "11mg", isActive = false),
                Cigarette(name = "白沙 (和天下 / Baisha Hetianxia)", price = 100.0, packSize = 20, priceType = "PACK", cartonPrice = 1000.0, packsPerCarton = 10, ean = "6901028113886", image = "", tarAmount = "10mg", isActive = false),
                Cigarette(name = "黄果树 (佳品 / Huangguoshu Jiapin)", price = 10.0, packSize = 20, priceType = "PACK", cartonPrice = 100.0, packsPerCarton = 10, ean = "6901028240018", image = "", tarAmount = "10mg", isActive = false),
                Cigarette(name = "黄果树 (长香思 / Huangguoshu Changxiangsi)", price = 13.0, packSize = 20, priceType = "PACK", cartonPrice = 130.0, packsPerCarton = 10, ean = "6901028240056", image = "", tarAmount = "10mg", isActive = false),
                Cigarette(name = "双喜 (软经典 / Shuangxi Soft Classic)", price = 10.0, packSize = 20, priceType = "PACK", cartonPrice = 100.0, packsPerCarton = 10, ean = "6901028010017", image = "", tarAmount = "11mg", isActive = false),
                Cigarette(name = "双喜 (硬经典1906 / Shuangxi Classic 1906)", price = 18.0, packSize = 20, priceType = "PACK", cartonPrice = 180.0, packsPerCarton = 10, ean = "6901028011908", image = "", tarAmount = "10mg", isActive = false),
                Cigarette(name = "利群 (新版 / Liqun New Version)", price = 16.0, packSize = 20, priceType = "PACK", cartonPrice = 160.0, packsPerCarton = 10, ean = "6901028207110", image = "", tarAmount = "11mg", isActive = false),
                Cigarette(name = "玉溪 (软 / Yuxi Soft)", price = 23.0, packSize = 20, priceType = "PACK", cartonPrice = 230.0, packsPerCarton = 10, ean = "6901028180017", image = "", tarAmount = "11mg", isActive = false),
                Cigarette(name = "芙蓉王 (硬黄 / Furongwang Hard Yellow)", price = 25.0, packSize = 20, priceType = "PACK", cartonPrice = 250.0, packsPerCarton = 10, ean = "6901028193017", image = "", tarAmount = "11mg", isActive = false)
            )
            val existingNames = existing.map { it.name }.toSet()
            for (cig in defaults) {
                if (!existingNames.contains(cig.name)) {
                    repository.insertCigarette(cig)
                }
            }
        }
    }

    fun setCigaretteJsonUrl(url: String) {
        cigaretteJsonUrl.value = url
        context?.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            ?.edit()?.putString("cigarette_json_url", url)?.apply()
    }

    fun syncCigarettesFromJsonUrl(url: String = cigaretteJsonUrl.value) {
        viewModelScope.launch(Dispatchers.IO) {
            isCigaretteSyncing.value = true
            cigaretteSyncResult.value = if (appLanguage.value == AppLanguage.EN) "Fetching JSON..." else "正在获取 JSON 数据..."
            try {
                val targetUrl = url.trim()
                val jsonText = if (targetUrl.isBlank() || targetUrl.equals("DEFAULT", ignoreCase = true) || targetUrl.equals("LOCAL", ignoreCase = true)) {
                    context?.assets?.open("cigarettes.json")?.bufferedReader()?.use { it.readText() }
                        ?: throw Exception("Local asset cigarettes.json not found")
                } else if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                    java.net.URL(targetUrl).readText()
                } else {
                    targetUrl
                }

                val jsonArray = org.json.JSONArray(jsonText)
                val existingList = repository.allCigarettes.firstOrNull() ?: emptyList()
                val existingByEan = existingList.filter { it.ean.isNotBlank() }.associateBy { it.ean }
                val existingByName = existingList.associateBy { it.name }

                var updatedCount = 0
                var addedCount = 0

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val ean = obj.optString("ean", "").trim()
                    val name = obj.optString("name", "Unknown Cigarette").trim()
                    val price = obj.optDouble("price", 20.0)
                    val packSize = obj.optInt("packSize", 20)
                    val priceType = obj.optString("priceType", "PACK")
                    val cartonPrice = obj.optDouble("cartonPrice", price * 10)
                    val packsPerCarton = obj.optInt("packsPerCarton", 10)
                    val image = obj.optString("image", "").trim()
                    val tarAmount = obj.optString("tarAmount", obj.optString("tar", "")).trim()

                    val matched = if (ean.isNotBlank()) existingByEan[ean] else existingByName[name]
                    if (matched != null) {
                        val updated = matched.copy(
                            name = name,
                            price = price,
                            packSize = packSize,
                            priceType = priceType,
                            cartonPrice = cartonPrice,
                            packsPerCarton = packsPerCarton,
                            ean = if (ean.isNotBlank()) ean else matched.ean,
                            image = if (image.isNotBlank()) image else matched.image,
                            tarAmount = if (tarAmount.isNotBlank()) tarAmount else matched.tarAmount
                        )
                        repository.updateCigarette(updated)
                        updatedCount++
                    } else {
                        val newCig = Cigarette(
                            name = name,
                            price = price,
                            packSize = packSize,
                            priceType = priceType,
                            cartonPrice = cartonPrice,
                            packsPerCarton = packsPerCarton,
                            ean = ean,
                            image = image,
                            tarAmount = tarAmount,
                            isActive = false
                        )
                        repository.insertCigarette(newCig)
                        addedCount++
                    }
                }

                cigaretteSyncResult.value = if (appLanguage.value == AppLanguage.EN) {
                    "Sync complete! Updated: $updatedCount, Added: $addedCount"
                } else {
                    "同步成功！已匹配更新 $updatedCount 种，新增 $addedCount 种品牌"
                }
            } catch (e: Exception) {
                cigaretteSyncResult.value = if (appLanguage.value == AppLanguage.EN) {
                    "Sync failed: ${e.localizedMessage}"
                } else {
                    "同步失败: ${e.localizedMessage}"
                }
            } finally {
                isCigaretteSyncing.value = false
            }
        }
    }

    val isDemoMode = MutableStateFlow(false)

    // Process smoking logs based on selected time range
    val trendData: StateFlow<List<TrendDataItem>> = combine(
        logs,
        selectedTimeRange,
        selectedMonthYear,
        customStartDate,
        customEndDate,
        maxIntervalThresholdHours,
        cigarettes
    ) { flows: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        calculateTrendData(
            logList = flows[0] as List<SmokingLog>,
            timeRange = flows[1] as TrendTimeRange,
            cal = flows[2] as Calendar,
            startMs = flows[3] as? Long,
            endMs = flows[4] as? Long,
            maxIntervalHours = flows[5] as Int,
            cigList = flows[6] as List<Cigarette>
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // A date boundary must refresh today's totals even if the database has not changed.
    val dayNow: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(millisUntilNextDay(System.currentTimeMillis()))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

    val stats: StateFlow<SmokingStats> = combine(logs, activeGoal, cigarettes, dayNow) { logList, goal, cigList, _ ->
        calculateStats(logList, goal, cigList, System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SmokingStats())

    init {
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            trackingSubject.value = runCatching {
                TrackingSubject.valueOf(prefs.getString("tracking_subject", TrackingSubject.TOBACCO.name) ?: TrackingSubject.TOBACCO.name)
            }.getOrDefault(TrackingSubject.TOBACCO)
            val langCode = prefs.getString("language", AppLanguage.ZH.code) ?: AppLanguage.ZH.code
            val themeCode = prefs.getString("theme_mode", AppThemeMode.SYSTEM.code) ?: AppThemeMode.SYSTEM.code
            val colorCode = prefs.getString("color_preset", AppColorPreset.DEFAULT.code) ?: AppColorPreset.DEFAULT.code
            val currencyCode = prefs.getString("currency_code", AppCurrency.CNY.code) ?: AppCurrency.CNY.code
            val fontCode = prefs.getString("font_family", AppFontFamily.DEFAULT.code) ?: AppFontFamily.DEFAULT.code
            val maxInterval = prefs.getInt("max_interval_hours", 6)

            val aiCustomEnabled = prefs.getBoolean("ai_custom_enabled", false)
            val aiCustomProtocolCode = prefs.getString("ai_custom_protocol", "openai") ?: "openai"
            val aiProtocol = AiApiProtocol.values().firstOrNull { it.code == aiCustomProtocolCode } ?: AiApiProtocol.OPENAI_COMPATIBLE
            val aiCustomApiKey = prefs.getString("ai_custom_api_key", "") ?: ""
            val aiCustomBaseUrl = prefs.getString("ai_custom_base_url", if (aiProtocol == AiApiProtocol.OPENAI_COMPATIBLE) "https://api.deepseek.com/v1" else RetrofitClient.DEFAULT_BASE_URL)
                ?: "https://api.deepseek.com/v1"
            val aiCustomModel = prefs.getString("ai_custom_model_name", if (aiProtocol == AiApiProtocol.OPENAI_COMPATIBLE) "deepseek-chat" else "gemini-3.5-flash")
                ?: "deepseek-chat"
            val aiCustomPersonaCode = prefs.getString("ai_custom_persona", "warm") ?: "warm"
            val persona = AiCoachPersona.values().firstOrNull { it.code == aiCustomPersonaCode } ?: AiCoachPersona.WARM
            aiCustomConfig.value = AiCustomConfig(
                enabled = aiCustomEnabled,
                protocol = aiProtocol,
                apiKey = aiCustomApiKey,
                baseUrl = aiCustomBaseUrl,
                modelName = aiCustomModel,
                persona = persona
            )

            appLanguage.value = AppLanguage.values().firstOrNull { it.code == langCode } ?: AppLanguage.ZH
            appThemeMode.value = AppThemeMode.values().firstOrNull { it.code == themeCode } ?: AppThemeMode.SYSTEM
            appColorPreset.value = AppColorPreset.values().firstOrNull { it.code == colorCode } ?: AppColorPreset.DEFAULT
            appCurrency.value = AppCurrency.values().firstOrNull { it.code == currencyCode } ?: AppCurrency.CNY
            appFontFamily.value = AppFontFamily.values().firstOrNull { it.code == fontCode } ?: AppFontFamily.DEFAULT
            maxIntervalThresholdHours.value = maxInterval.coerceIn(1, 24)
        }

        viewModelScope.launch {
            delay(1500)
            fetchAiAdvice()
        }
    }

    private fun computeIntervalAndPeak(logs: List<SmokingLog>, maxIntervalHours: Int = 6, lang: AppLanguage = AppLanguage.ZH): Pair<Int, String> {
        val noLogText = if (lang == AppLanguage.EN) "No logs" else "无打卡"
        if (logs.isEmpty()) return Pair(0, noLogText)

        val sorted = logs.sortedBy { it.timestamp }
        var totalIntervalMs = 0L
        var count = 0
        val maxIntervalMs = maxIntervalHours.coerceIn(1, 24) * 3_600_000L
        for (i in 0 until sorted.size - 1) {
            val diff = sorted[i + 1].timestamp - sorted[i].timestamp
            if (diff in 60_000L..maxIntervalMs) {
                totalIntervalMs += diff
                count++
            }
        }
        val avgMin = if (count > 0) (totalIntervalMs / (count * 60_000L)).toInt() else 0

        val hourBins = IntArray(24)
        logs.forEach { log ->
            val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourBins[hour] += log.quantity
        }

        var maxCount = 0
        var peakStartHour = -1
        for (h in 0..22) {
            val countInWindow = hourBins[h] + hourBins[h + 1]
            if (countInWindow > maxCount) {
                maxCount = countInWindow
                peakStartHour = h
            }
        }

        val peakSlot = if (peakStartHour >= 0 && maxCount > 0) {
            String.format("%02d:00-%02d:00", peakStartHour, peakStartHour + 2)
        } else if (logs.isNotEmpty()) {
            val h = Calendar.getInstance().apply { timeInMillis = logs[0].timestamp }.get(Calendar.HOUR_OF_DAY)
            String.format("%02d:00-%02d:00", h, (h + 2) % 24)
        } else {
            noLogText
        }

        return Pair(avgMin, peakSlot)
    }

    private fun calculateTrendData(
        logList: List<SmokingLog>,
        timeRange: TrendTimeRange,
        cal: Calendar,
        startMs: Long?,
        endMs: Long?,
        maxIntervalHours: Int = 6,
        cigList: List<Cigarette> = emptyList()
    ): List<TrendDataItem> {
        val trend = ArrayList<TrendDataItem>()
        val sdfDay = SimpleDateFormat("MM/dd", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("yyyy/MM", Locale.getDefault())

        val cigUnitPriceMap = cigList.associate { cig ->
            cig.id to (cig.price / cig.packSize.coerceAtLeast(1))
        }
        val defaultUnitPrice = cigList.firstOrNull { it.isActive }?.let { it.price / it.packSize.coerceAtLeast(1) }
            ?: cigList.firstOrNull()?.let { it.price / it.packSize.coerceAtLeast(1) }
            ?: 1.25

        fun computeReceivedSaved(subLogs: List<SmokingLog>): Double {
            return subLogs.filter { getLogType(it) == "RECEIVED_IN" }.sumOf { log ->
                val unitPrice = cigUnitPriceMap[log.cigaretteId] ?: defaultUnitPrice
                log.quantity * unitPrice
            }
        }

        val lang = appLanguage.value
        when (timeRange) {
            TrendTimeRange.LAST_7_DAYS -> {
                for (i in 6 downTo 0) {
                    val dayCal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -i)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val dayStart = dayCal.timeInMillis
                    val dayEnd = dayCal.apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis

                    val dayLogs = logList.filter { it.timestamp in dayStart..dayEnd }
                    val selfCount = dayLogs.filter { getLogType(it) == "SELF" }.sumOf { it.quantity }
                    val sharedCount = dayLogs.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.quantity }
                    val receivedCount = dayLogs.filter { getLogType(it) == "RECEIVED_IN" }.sumOf { it.quantity }
                    val selfCost = dayLogs.filter { getLogType(it) == "SELF" }.sumOf { it.cost }
                    val sharedCost = dayLogs.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.cost }
                    val receivedSaved = computeReceivedSaved(dayLogs)
                    val (avgMin, peakSlot) = computeIntervalAndPeak(dayLogs, maxIntervalHours, lang)

                    trend.add(
                        TrendDataItem(
                            dateLabel = sdfDay.format(dayCal.time),
                            selfCount = selfCount,
                            sharedCount = sharedCount,
                            receivedCount = receivedCount,
                            selfCost = selfCost,
                            sharedCost = sharedCost,
                            receivedSaved = receivedSaved,
                            timestamp = dayStart,
                            avgIntervalMinutes = avgMin,
                            peakHourSlot = peakSlot
                        )
                    )
                }
            }
            TrendTimeRange.THIS_WEEK -> {
                val currentCal = Calendar.getInstance()
                currentCal.firstDayOfWeek = Calendar.MONDAY
                currentCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                currentCal.set(Calendar.HOUR_OF_DAY, 0)
                currentCal.set(Calendar.MINUTE, 0)
                currentCal.set(Calendar.SECOND, 0)
                currentCal.set(Calendar.MILLISECOND, 0)

                val dayNames = if (lang == AppLanguage.EN)
                    arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                else
                    arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                for (i in 0..6) {
                    val dayCal = currentCal.clone() as Calendar
                    dayCal.add(Calendar.DAY_OF_YEAR, i)
                    val dayStart = dayCal.timeInMillis
                    val dayEnd = (dayCal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis

                    val dayLogs = logList.filter { it.timestamp in dayStart..dayEnd }
                    val selfCount = dayLogs.filter { getLogType(it) == "SELF" }.sumOf { it.quantity }
                    val sharedCount = dayLogs.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.quantity }
                    val receivedCount = dayLogs.filter { getLogType(it) == "RECEIVED_IN" }.sumOf { it.quantity }
                    val selfCost = dayLogs.filter { getLogType(it) == "SELF" }.sumOf { it.cost }
                    val sharedCost = dayLogs.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.cost }
                    val receivedSaved = computeReceivedSaved(dayLogs)
                    val (avgMin, peakSlot) = computeIntervalAndPeak(dayLogs, maxIntervalHours, lang)

                    trend.add(
                        TrendDataItem(
                            dateLabel = dayNames[i],
                            selfCount = selfCount,
                            sharedCount = sharedCount,
                            receivedCount = receivedCount,
                            selfCost = selfCost,
                            sharedCost = sharedCost,
                            receivedSaved = receivedSaved,
                            timestamp = dayStart,
                            avgIntervalMinutes = avgMin,
                            peakHourSlot = peakSlot
                        )
                    )
                }
            }
            TrendTimeRange.THIS_MONTH, TrendTimeRange.SPECIFIC_MONTH -> {
                val targetCal = if (timeRange == TrendTimeRange.THIS_MONTH) Calendar.getInstance() else cal
                val maxDays = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val monthFormat = SimpleDateFormat("M/d", Locale.getDefault())

                for (day in 1..maxDays) {
                    val dayCal = (targetCal.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val dayStart = dayCal.timeInMillis
                    val dayEnd = (dayCal.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis

                    val dayLogs = logList.filter { it.timestamp in dayStart..dayEnd }
                    val selfCount = dayLogs.filter { getLogType(it) == "SELF" }.sumOf { it.quantity }
                    val sharedCount = dayLogs.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.quantity }
                    val receivedCount = dayLogs.filter { getLogType(it) == "RECEIVED_IN" }.sumOf { it.quantity }
                    val selfCost = dayLogs.filter { getLogType(it) == "SELF" }.sumOf { it.cost }
                    val sharedCost = dayLogs.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.cost }
                    val receivedSaved = computeReceivedSaved(dayLogs)
                    val (avgMin, peakSlot) = computeIntervalAndPeak(dayLogs, maxIntervalHours, lang)

                    trend.add(
                        TrendDataItem(
                            dateLabel = monthFormat.format(dayCal.time),
                            selfCount = selfCount,
                            sharedCount = sharedCount,
                            receivedCount = receivedCount,
                            selfCost = selfCost,
                            sharedCost = sharedCost,
                            receivedSaved = receivedSaved,
                            timestamp = dayStart,
                            avgIntervalMinutes = avgMin,
                            peakHourSlot = peakSlot
                        )
                    )
                }
            }
            TrendTimeRange.SPECIFIC_YEAR -> {
                val targetYear = cal.get(Calendar.YEAR)
                val monthNames = if (lang == AppLanguage.EN)
                    arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                else
                    arrayOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
                for (month in 0..11) {
                    val monthStartCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, targetYear)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val monthEndCal = (monthStartCal.clone() as Calendar).apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }

                    val monthLogs = logList.filter { it.timestamp in monthStartCal.timeInMillis..monthEndCal.timeInMillis }
                    val selfCount = monthLogs.filter { getLogType(it) == "SELF" }.sumOf { it.quantity }
                    val sharedCount = monthLogs.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.quantity }
                    val receivedCount = monthLogs.filter { getLogType(it) == "RECEIVED_IN" }.sumOf { it.quantity }
                    val selfCost = monthLogs.filter { getLogType(it) == "SELF" }.sumOf { it.cost }
                    val sharedCost = monthLogs.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.cost }
                    val receivedSaved = computeReceivedSaved(monthLogs)
                    val (avgMin, peakSlot) = computeIntervalAndPeak(monthLogs, maxIntervalHours, lang)

                    trend.add(
                        TrendDataItem(
                            dateLabel = monthNames[month],
                            selfCount = selfCount,
                            sharedCount = sharedCount,
                            receivedCount = receivedCount,
                            selfCost = selfCost,
                            sharedCost = sharedCost,
                            receivedSaved = receivedSaved,
                            timestamp = monthStartCal.timeInMillis,
                            avgIntervalMinutes = avgMin,
                            peakHourSlot = peakSlot
                        )
                    )
                }
            }
            TrendTimeRange.CUSTOM_RANGE -> {
                val start = startMs ?: (System.currentTimeMillis() - 14 * 86400000L)
                val end = endMs ?: System.currentTimeMillis()
                var current = Calendar.getInstance().apply { timeInMillis = start; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }

                while (current.timeInMillis <= end) {
                    val dayStart = current.timeInMillis
                    val dayEnd = (current.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59) }.timeInMillis

                    val dayLogs = logList.filter { it.timestamp in dayStart..dayEnd }
                    val selfCount = dayLogs.filter { getLogType(it) == "SELF" }.sumOf { it.quantity }
                    val sharedCount = dayLogs.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.quantity }
                    val receivedCount = dayLogs.filter { getLogType(it) == "RECEIVED_IN" }.sumOf { it.quantity }
                    val selfCost = dayLogs.filter { getLogType(it) == "SELF" }.sumOf { it.cost }
                    val sharedCost = dayLogs.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.cost }
                    val receivedSaved = computeReceivedSaved(dayLogs)
                    val (avgMin, peakSlot) = computeIntervalAndPeak(dayLogs, maxIntervalHours, lang)

                    trend.add(
                        TrendDataItem(
                            dateLabel = sdfDay.format(current.time),
                            selfCount = selfCount,
                            sharedCount = sharedCount,
                            receivedCount = receivedCount,
                            selfCost = selfCost,
                            sharedCost = sharedCost,
                            receivedSaved = receivedSaved,
                            timestamp = dayStart,
                            avgIntervalMinutes = avgMin,
                            peakHourSlot = peakSlot
                        )
                    )
                    current.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
        }

        return trend
    }

    private fun getLogType(log: SmokingLog): String {
        return when {
            log.logType.isNotEmpty() -> log.logType
            log.isShared -> "SHARED_OUT"
            else -> "SELF"
        }
    }

    private fun calculateStats(logList: List<SmokingLog>, goal: SmokingGoal?, cigList: List<Cigarette>, now: Long): SmokingStats {
        val todayStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val weekStart = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
        val monthStart = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_MONTH, -30) }.timeInMillis

        val todayLogs = logList.filter { it.timestamp in todayStart..now }
        val weekLogs = logList.filter { it.timestamp in weekStart..now }
        val monthLogs = logList.filter { it.timestamp in monthStart..now }

        val todaySelf = todayLogs.filter { getLogType(it) == "SELF" }.sumOf { it.quantity }
        val todayShared = todayLogs.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.quantity }
        val todayReceived = todayLogs.filter { getLogType(it) == "RECEIVED_IN" }.sumOf { it.quantity }
        val todayTotal = todaySelf + todayReceived // Personal consumption
        val todayCost = todayLogs.filter { getLogType(it) != "RECEIVED_IN" }.sumOf { it.cost }

        val cigUnitPriceMap = cigList.associate { cig ->
            cig.id to (cig.price / cig.packSize.coerceAtLeast(1))
        }
        val activeCig = cigList.firstOrNull { it.isActive } ?: cigList.firstOrNull()
        val defaultUnitPrice = activeCig?.let { it.price / it.packSize.coerceAtLeast(1) } ?: 1.25

        fun computeReceivedSaved(subLogs: List<SmokingLog>): Double {
            return subLogs.filter { getLogType(it) == "RECEIVED_IN" }.sumOf { log ->
                val unitPrice = cigUnitPriceMap[log.cigaretteId] ?: defaultUnitPrice
                log.quantity * unitPrice
            }
        }

        val todaySavedFromReceived = computeReceivedSaved(todayLogs)

        val weekTotal = weekLogs.sumOf { it.quantity }
        val weekCost = weekLogs.filter { getLogType(it) != "RECEIVED_IN" }.sumOf { it.cost }

        val monthTotal = monthLogs.sumOf { it.quantity }
        val monthCost = monthLogs.filter { getLogType(it) != "RECEIVED_IN" }.sumOf { it.cost }

        val totalSelfCost = logList.filter { getLogType(it) == "SELF" }.sumOf { it.cost }
        val totalSharedCost = logList.filter { getLogType(it) == "SHARED_OUT" }.sumOf { it.cost }
        val totalReceivedSaved = computeReceivedSaved(logList)
        val totalSpent = totalSelfCost + totalSharedCost

        val dailyLimit = goal?.dailyLimit ?: 10
        val isOverLimit = (todaySelf + todayReceived) > dailyLimit

        val todaySavedCount = (dailyLimit - (todaySelf + todayReceived)).coerceAtLeast(0)
        val todaySavedMoney = todaySavedCount * defaultUnitPrice + todaySavedFromReceived

        return SmokingStats(
            todaySelfCount = todaySelf,
            todaySharedCount = todayShared,
            todayReceivedCount = todayReceived,
            todayTotalCount = todayTotal,
            todayCost = todayCost,
            todaySavedFromReceived = todaySavedFromReceived,
            weekTotalCount = weekTotal,
            weekCost = weekCost,
            monthTotalCount = monthTotal,
            monthCost = monthCost,
            totalSelfCost = totalSelfCost,
            totalSharedCost = totalSharedCost,
            totalReceivedSaved = totalReceivedSaved,
            totalSpent = totalSpent,
            totalSavedMoney = todaySavedMoney,
            currentGoalLimit = dailyLimit,
            isOverLimit = isOverLimit
        )
    }

    // Database actions
    fun addCigarette(
        name: String,
        price: Double,
        packSize: Int,
        priceType: String = "PACK",
        cartonPrice: Double = price * 10,
        packsPerCarton: Int = 10,
        ean: String = "",
        image: String = "",
        tarAmount: String = ""
    ) {
        viewModelScope.launch {
            val calculatedPrice = if (priceType == "CARTON") cartonPrice / packsPerCarton.coerceAtLeast(1) else price
            repository.insertCigarette(
                Cigarette(
                    name = name,
                    price = calculatedPrice,
                    packSize = packSize,
                    priceType = priceType,
                    cartonPrice = cartonPrice,
                    packsPerCarton = packsPerCarton,
                    ean = ean,
                    image = image,
                    tarAmount = tarAmount,
                    isActive = cigarettes.value.isEmpty()
                )
            )
        }
    }

    fun updateCigarette(cigarette: Cigarette) {
        viewModelScope.launch {
            repository.updateCigarette(cigarette)
        }
    }

    fun setActiveCigarette(cigaretteId: Int) {
        viewModelScope.launch {
            repository.setActiveCigarette(cigaretteId)
        }
    }

    fun deleteCigarette(cigarette: Cigarette) {
        viewModelScope.launch {
            repository.deleteCigarette(cigarette)
        }
    }

    fun addSmokingLog(
        cigaretteId: Int,
        quantity: Int,
        logType: String, // "SELF", "SHARED_OUT", "RECEIVED_IN"
        note: String,
        customTime: Long? = null
    ) {
        viewModelScope.launch {
            if (quantity <= 0 || logType !in setOf("SELF", "SHARED_OUT", "RECEIVED_IN")) return@launch
            val cigarette = repository.getCigaretteById(cigaretteId) ?: return@launch
            val price = cigarette.price
            val packSize = cigarette.packSize.coerceAtLeast(1)

            val costOfEvent = if (logType == "RECEIVED_IN") 0.0 else (quantity.toDouble() / packSize) * price

            val log = SmokingLog(
                cigaretteId = cigarette.id,
                quantity = quantity,
                isShared = (logType == "SHARED_OUT"),
                logType = logType,
                cost = costOfEvent,
                note = note,
                timestamp = customTime ?: System.currentTimeMillis()
            )
            repository.insertLog(log)
            fetchAiAdvice()
        }
    }

    suspend fun deleteSmokingLog(log: SmokingLog) {
        repository.deleteLog(log)
        fetchAiAdvice()
    }

    suspend fun restoreSmokingLog(log: SmokingLog) {
        repository.insertLog(log)
        fetchAiAdvice()
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

    // Toggle Demo Data Mode
    fun toggleDemoMode(enabled: Boolean) {
        isDemoMode.value = enabled
        viewModelScope.launch {
            if (enabled) {
                seedDemoData()
            } else {
                repository.deleteDemoLogs()
            }
            fetchAiAdvice()
        }
    }

    private suspend fun seedDemoData() {
        val activeCigs = cigarettes.value
        val defaultCigId = activeCigs.firstOrNull()?.id ?: 1
        val packPrice = activeCigs.firstOrNull()?.price ?: 25.0
        val packSize = activeCigs.firstOrNull()?.packSize ?: 20
        val unitPrice = packPrice / packSize

        val demoLogs = ArrayList<SmokingLog>()
        val nowMs = System.currentTimeMillis()

        // Generate 60 days of realistic smoker logs
        for (dayOffset in 59 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -dayOffset)
            }

            // Define daily time slots for a realistic smoker:
            // 1. Morning awakening (07:20 - 08:30)
            // 2. Work morning break (10:15 - 10:45)
            // 3. Post-lunch (12:30 - 13:20)
            // 4. Afternoon tea / stress break (15:00 - 16:15)
            // 5. Off-work / commute / dinner social (18:15 - 19:30)
            // 6. Night leisure before bed (21:00 - 22:45)

            val slots = listOf(
                Pair(7, 30) to listOf("SELF" to "晨起第1支唤醒烟", "SELF" to "洗漱后提神"),
                Pair(10, 20) to listOf("SELF" to "工间休息提神", "SHARED_OUT" to "给同事递烟", "RECEIVED_IN" to "接受同事递烟"),
                Pair(12, 45) to listOf("SELF" to "饭后一根烟", "SHARED_OUT" to "午餐后散烟社交", "RECEIVED_IN" to "午饭后蹭烟"),
                Pair(15, 30) to listOf("SELF" to "下午茶提神醒脑", "SELF" to "工作遇到难题抽根烟", "RECEIVED_IN" to "客户递烟交流"),
                Pair(18, 40) to listOf("SELF" to "下班路途中", "SHARED_OUT" to "晚餐聚餐递烟", "RECEIVED_IN" to "聚会接烟"),
                Pair(21, 30) to listOf("SELF" to "晚间自娱自乐", "SELF" to "睡前放松总结", "SELF" to "阳台独处放空")
            )

            for ((timePair, notesList) in slots) {
                val hour = timePair.first
                val minute = timePair.second + Random.nextInt(-10, 15)
                val logCal = (dayCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute.coerceIn(0, 59))
                    set(Calendar.SECOND, Random.nextInt(0, 59))
                }

                val logTime = logCal.timeInMillis
                if (logTime <= nowMs) {
                    val (type, note) = notesList[Random.nextInt(notesList.size)]
                    demoLogs.add(
                        SmokingLog(
                            cigaretteId = defaultCigId,
                            quantity = 1,
                            logType = type,
                            isShared = (type == "SHARED_OUT"),
                            cost = if (type == "RECEIVED_IN") 0.0 else unitPrice,
                            note = note,
                            timestamp = logTime,
                            isDemo = true
                        )
                    )
                }
            }
        }

        repository.insertLogs(demoLogs)
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
    private var adviceJob: Job? = null

    fun fetchAiAdvice(force: Boolean = false) {
        adviceJob?.cancel()
        val previousResult = when (val s = _aiAdviceState.value) {
            is AiAdviceState.Success -> s.result
            is AiAdviceState.Loading -> s.previous
            is AiAdviceState.Error -> s.previous
            else -> null
        }
        _aiAdviceState.value = AiAdviceState.Loading(previousResult)

        adviceJob = viewModelScope.launch {
            try {
                if (!force) {
                    delay(500)
                }
                val currentStats = stats.value
                val trend = trendData.value
                val totalSelf7Days = trend.sumOf { it.selfCount }
                val totalShared7Days = trend.sumOf { it.sharedCount }
                val totalReceived7Days = trend.sumOf { it.receivedCount }

                val config = aiCustomConfig.value
                val persona = config.persona

                val prompt = """
                    你是一位专业且充满同理心的戒烟健康教练。你的指导风格要求：【${persona.labelZh} - ${persona.promptInstruction}】。
                    请根据用户的以下真实吸烟数据，生成一份有洞察力、温暖且高度个性化的健康提醒和戒烟建议：
                    
                    【今日吸烟统计】
                    - 自购自抽：${currentStats.todaySelfCount} 支
                    - 社交递烟：${currentStats.todaySharedCount} 支
                    - 社交接烟（他人递烟）：${currentStats.todayReceivedCount} 支
                    - 今日花费：${String.format(Locale.getDefault(), "%.2f", currentStats.todayCost)} 元
                    
                    【近期统计】
                    - 近7天自抽总数：$totalSelf7Days 支
                    - 近7天给他人递烟：$totalShared7Days 支
                    - 近7天接他人递烟：$totalReceived7Days 支
                    - 近期支出：${String.format(Locale.getDefault(), "%.2f", currentStats.weekCost)} 元
                    
                    【当前戒烟目标】
                    - 每日吸烟上限：${currentStats.currentGoalLimit} 支
                    - 目标达成状态：${if (currentStats.isOverLimit) "⚠️ 已超标！" else "✅ 严格遵守中！"}
                    
                    【健康提醒需求】
                    1. 简短总结今日烟瘾趋势。分析自抽、社交递烟与社交接烟（他人递烟）的特点；
                    2. 提供 2 条实用的、根据今天数据定制的戒烟与社交控烟小贴士；
                    3. 给出贴切教练风格的鼓励：字数在 150 字以内，简明扼要，分段清晰，使用恰当的 emoji！
                """.trimIndent()

                val (adviceText, source, modelUsed) = withContext(Dispatchers.IO) {
                    if (config.enabled) {
                        if (config.protocol == AiApiProtocol.OPENAI_COMPATIBLE) {
                            val effectiveKey = config.apiKey.trim()
                            val effectiveBaseUrl = config.baseUrl.trim().ifBlank { "https://api.deepseek.com/v1" }
                            val effectiveModel = config.modelName.trim().ifBlank { "deepseek-chat" }

                            if (effectiveKey.isBlank()) {
                                Triple(getMockAdvice(currentStats, persona), AiAdviceSource.LOCAL_FALLBACK, "本地规则引擎")
                            } else {
                                try {
                                    val targetUrl = RetrofitClient.normalizeOpenAiUrl(effectiveBaseUrl)
                                    val bearerAuth = if (effectiveKey.startsWith("Bearer ", ignoreCase = true)) effectiveKey else "Bearer $effectiveKey"
                                    val req = OpenAiChatRequest(
                                        model = effectiveModel,
                                        messages = listOf(
                                            OpenAiChatMessage(role = "system", content = "你是一位专业且充满同理心的戒烟健康教练。你的指导风格要求：【${persona.labelZh} - ${persona.promptInstruction}】。"),
                                            OpenAiChatMessage(role = "user", content = prompt)
                                        )
                                    )
                                    val resp = RetrofitClient.openAiService.chatCompletions(targetUrl, bearerAuth, req)
                                    val reply = resp.choices?.firstOrNull()?.message?.content?.trim()
                                    if (!reply.isNullOrBlank()) {
                                        Triple(reply, AiAdviceSource.OPENAI_COMPATIBLE, effectiveModel)
                                    } else {
                                        Triple(getMockAdvice(currentStats, persona), AiAdviceSource.LOCAL_FALLBACK, "本地规则引擎")
                                    }
                                } catch (e: Exception) {
                                    Triple(getMockAdvice(currentStats, persona), AiAdviceSource.LOCAL_FALLBACK, "离线保底")
                                }
                            }
                        } else {
                            // Custom Google Gemini protocol
                            val effectiveKey = config.apiKey.trim().ifBlank { BuildConfig.GEMINI_API_KEY }
                            val effectiveBaseUrl = config.baseUrl.trim().ifBlank { RetrofitClient.DEFAULT_BASE_URL }
                            val effectiveModel = config.modelName.trim().ifBlank { "gemini-3.5-flash" }

                            if (effectiveKey.isBlank() || effectiveKey == "MY_GEMINI_API_KEY") {
                                Triple(getMockAdvice(currentStats, persona), AiAdviceSource.LOCAL_FALLBACK, "本地规则引擎")
                            } else {
                                try {
                                    val request = GenerateContentRequest(
                                        contents = listOf(Content(parts = listOf(Part(text = prompt))))
                                    )
                                    val service = RetrofitClient.getGeminiService(effectiveBaseUrl)
                                    val response = service.generateContent(effectiveModel, effectiveKey, request)
                                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                    if (!text.isNullOrBlank()) {
                                        Triple(text.trim(), AiAdviceSource.GEMINI_CLOUD, effectiveModel)
                                    } else {
                                        Triple(getMockAdvice(currentStats, persona), AiAdviceSource.LOCAL_FALLBACK, "本地规则引擎")
                                    }
                                } catch (e: Exception) {
                                    Triple(getMockAdvice(currentStats, persona), AiAdviceSource.LOCAL_FALLBACK, "离线保底")
                                }
                            }
                        }
                    } else {
                        // Standard default: Gemini if key provided, else smart local fallback
                        val defaultKey = BuildConfig.GEMINI_API_KEY
                        if (defaultKey.isNotBlank() && defaultKey != "MY_GEMINI_API_KEY") {
                            try {
                                val request = GenerateContentRequest(
                                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                                )
                                val service = RetrofitClient.geminiService
                                val response = service.generateContent("gemini-3.5-flash", defaultKey, request)
                                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                                if (!text.isNullOrBlank()) {
                                    Triple(text.trim(), AiAdviceSource.GEMINI_CLOUD, "gemini-3.5-flash")
                                } else {
                                    Triple(getMockAdvice(currentStats, persona), AiAdviceSource.LOCAL_FALLBACK, "本地规则引擎")
                                }
                            } catch (e: Exception) {
                                Triple(getMockAdvice(currentStats, persona), AiAdviceSource.LOCAL_FALLBACK, "离线保底")
                            }
                        } else {
                            Triple(getMockAdvice(currentStats, persona), AiAdviceSource.LOCAL_FALLBACK, "本地规则引擎")
                        }
                    }
                }

                val result = AiAdviceResult(
                    advice = adviceText,
                    source = source,
                    modelName = modelUsed,
                    persona = persona,
                    timestamp = System.currentTimeMillis()
                )
                _aiAdviceState.value = AiAdviceState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiAdviceState.value = AiAdviceState.Error("获取健康建议失败: ${e.localizedMessage ?: e.message}", previousResult)
            }
        }
    }

    private fun getMockAdvice(stats: SmokingStats, persona: AiCoachPersona = AiCoachPersona.WARM): String {
        return when (persona) {
            AiCoachPersona.STRICT -> {
                if (stats.todaySelfCount == 0 && stats.todayReceivedCount == 0) {
                    "🛡️ **严守防线！** 今日吸烟量为 0 支。烟瘾是一场意志力的持久战，不要在任何社交场合掉以轻心。继续坚守！🎯\n\n💡 **自律守则**:\n- 绝不抽第一口，不给大脑任何妥协让步的借口。\n- 遇到朋友递烟，直接且坚决拒绝，切勿半推半就。"
                } else if (stats.isOverLimit) {
                    "🚨 **警报超标！** 今日总吸烟数 ${stats.todayTotalCount} 支已突破目标上限 ${stats.currentGoalLimit} 支！放任只会让前面的努力付诸东流。立即停止！🛑\n\n💡 **行动指令**:\n- 今晚将烟盒彻底收起远离视线，立刻洗漱清口打断渴求。\n- 复盘超标场景：下次在相同诱因出现前提前离开现场。"
                } else {
                    "⚖️ **处于警戒线内。** 今日吸烟 ${stats.todayTotalCount} 支，虽然未超标，但自控仍需紧绷，绝不能在晚上松懈。保持克制！⚡\n\n💡 **自律守则**:\n- 无论他人如何劝烟，守住底线。\n- 烟瘾峰值一般仅持续3-5分钟，强制转移注意力即可度过。"
                }
            }
            AiCoachPersona.ANALYTICAL -> {
                if (stats.todaySelfCount == 0 && stats.todayReceivedCount == 0) {
                    "📊 **数据模型评级: A+**\n今日吸烟 0 支，节省开销 ¥0.00，心率与一氧化碳水平持续回归基准值。健康收益曲线处于高位。✨\n\n💡 **分析建议**:\n- 维持低诱因环境，防止晚间波峰出现。\n- 持续记录无烟状态以巩固行为学正反馈闭环。"
                } else if (stats.isOverLimit) {
                    val excess = stats.todayTotalCount - stats.currentGoalLimit
                    val rate = if (stats.currentGoalLimit > 0) ((stats.todayTotalCount.toDouble() / stats.currentGoalLimit - 1) * 100).toInt() else 100
                    "📈 **指标偏离警告**\n今日摄入 ${stats.todayTotalCount} 支（自购 ${stats.todaySelfCount} 支 / 接烟 ${stats.todayReceivedCount} 支），超出限额 $excess 支 (+$rate%)。⚠️\n\n💡 **策略调整**:\n- 社交接烟占比较高时，需将'他人递烟'设定为阻断型条件反射触发点。\n- 计算吸烟财务开销累计值，设定强制惩罚性储蓄。"
                } else {
                    val rate = if (stats.currentGoalLimit > 0) ((stats.todayTotalCount.toDouble() / stats.currentGoalLimit) * 100).toInt() else 0
                    "📉 **数据模型稳定**\n今日吸烟 ${stats.todayTotalCount} 支，目标达成率 $rate%，今日烟资 ¥${String.format(Locale.getDefault(), "%.2f", stats.todayCost)}。各指标在可控区间。👍\n\n💡 **分析建议**:\n- 监测高频吸烟时段（如饭后或午休），提前设定替代物阻断。\n- 逐步将每日上限由 ${stats.currentGoalLimit} 阶梯式递减至下一阶段目标。"
                }
            }
            AiCoachPersona.WARM -> {
                if (stats.todaySelfCount == 0 && stats.todayReceivedCount == 0) {
                    "🌟 **干得太漂亮了！** 截至目前，你今天还没有抽过一支烟！你的肺正在欢呼，身体正在净化。继续保持！💪🏼\n\n💡 **今日贴士**:\n- 烟瘾来袭时，尝试喝一口冰水或做 3 次深呼吸。\n- 社交递烟时多用口香糖递给对方，换种温暖健康的社交方式。"
                } else if (stats.isOverLimit) {
                    "⚠️ **今日温馨提醒**：你今天共抽了 ${stats.todayTotalCount} 支烟（自抽 ${stats.todaySelfCount} 支，接烟 ${stats.todayReceivedCount} 支），超出了 ${stats.currentGoalLimit} 支上限。不要灰心！调整呼吸，今晚就到此为止吧！🍀\n\n💡 **今日贴士**:\n- 面对他人递烟，学会礼貌拒绝：“最近在养肺，多谢好意啦！”\n- 社交场合多手里拿杯茶水，减少接烟手势习惯。"
                } else {
                    "👍 **保持得不错！** 今天你自抽 ${stats.todaySelfCount} 支，接烟 ${stats.todayReceivedCount} 支，控制在 ${stats.currentGoalLimit} 支的目标范围之内。这是一次了不起的自律表现！✨\n\n💡 **今日贴士**:\n- 饭后用薄荷糖替代烟草，打断习惯性烟瘾。\n- 记录下每次成功克制烟瘾的瞬间，为你点赞！"
                }
            }
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
                database.smokingGoalDao(),
                database.betelDao()
            )
            val syncManager = CloudSyncManager(context.applicationContext, repository)
            @Suppress("UNCHECKED_CAST")
            return SmokingViewModel(repository, syncManager, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

internal fun millisUntilNextDay(now: Long): Long = Calendar.getInstance().apply {
    timeInMillis = now
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    add(Calendar.DAY_OF_YEAR, 1)
}.timeInMillis.let { (it - now).coerceAtLeast(1L) }
