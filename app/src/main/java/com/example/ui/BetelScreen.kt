package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.CallMade
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BetelLog
import com.example.data.model.BetelGoal
import com.example.data.model.BetelProduct
import com.example.ui.i18n.AppLanguage
import java.text.SimpleDateFormat
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class BetelSummary(
    val todayConsumed: Int,
    val todayShared: Int,
    val todayCost: Double,
    val monthCost: Double,
    val weekCounts: List<Int>
)

internal fun summarizeBetel(logs: List<BetelLog>, now: Long): BetelSummary {
    val start = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayStart = start.timeInMillis
    val weekStarts = (6 downTo 0).map { days ->
        (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -days) }.timeInMillis
    }
    val monthStart = (start.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis
    val today = logs.filter { it.timestamp in todayStart..now }
    return BetelSummary(
        todayConsumed = today.filter { it.logType != "SHARED_OUT" }.sumOf { it.quantity },
        todayShared = today.filter { it.logType == "SHARED_OUT" }.sumOf { it.quantity },
        todayCost = today.sumOf { it.cost },
        monthCost = logs.filter { it.timestamp in monthStart..now }.sumOf { it.cost },
        weekCounts = weekStarts.mapIndexed { index, dayStart ->
            val end = weekStarts.getOrNull(index + 1) ?: (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
            logs.filter { it.timestamp in dayStart until end && it.logType != "SHARED_OUT" }.sumOf { it.quantity }
        }
    )
}

private fun label(zh: String, en: String, lang: AppLanguage) = if (lang == AppLanguage.EN) en else zh

@Composable
fun BetelHomeScreen(viewModel: SmokingViewModel) {
    val products by viewModel.betelProducts.collectAsStateWithLifecycle()
    val logs by viewModel.betelLogs.collectAsStateWithLifecycle()
    val goal by viewModel.betelGoal.collectAsStateWithLifecycle()
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currency by viewModel.appCurrency.collectAsStateWithLifecycle()
    val now by rememberCurrentTime()
    val summary = remember(logs, now) { summarizeBetel(logs, now) }
    val active = products.firstOrNull { it.isActive } ?: products.firstOrNull()
    val todayStart = remember(now) { Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis }
    val todayLogs = remember(logs, todayStart) { logs.filter { it.timestamp >= todayStart }.sortedByDescending { it.timestamp } }
    val self = todayLogs.filter { it.logType == "SELF" }.sumOf { it.quantity }
    val received = todayLogs.filter { it.logType == "RECEIVED_IN" }.sumOf { it.quantity }
    val saved = todayLogs.filter { it.logType == "RECEIVED_IN" }.sumOf { log ->
        val product = products.firstOrNull { it.id == log.productId }
        if (product != null) log.quantity * product.packPrice / product.piecesPerPack else 0.0
    }
    val dailyLimit = goal?.dailyLimit
    val overLimit = dailyLimit != null && summary.todayConsumed > dailyLimit
    var showLogDialog by remember { mutableStateOf(false) }
    var showAllLogs by remember { mutableStateOf(false) }
    val visibleLogs = if (showAllLogs) logs.sortedByDescending { it.timestamp }.take(50) else todayLogs
    val money: (Double) -> String = { "${currency.symbol}${String.format(Locale.getDefault(), "%.2f", it)}" }
    val piece = label("颗", "pcs", lang)
    val lastConsumed = logs.filter { it.logType != "SHARED_OUT" }.maxOfOrNull { it.timestamp }
    var timerNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            timerNow = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
    }
    val elapsedSeconds = lastConsumed?.let { ((timerNow - it).coerceAtLeast(0L) / 1_000) }
    val elapsedText = elapsedSeconds?.let { "%02d:%02d:%02d".format(it / 3_600, (it / 60) % 60, it % 60) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (overLimit) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(label("今日食用已超过上限 $dailyLimit 颗", "Daily limit of $dailyLimit pieces exceeded", lang),
                        color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(label("当前槟榔品牌", "Active betel brand", lang), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    Spacer(Modifier.height(4.dp))
                    Text(active?.name ?: label("请先在品牌页添加槟榔", "Add a product in Brands first", lang),
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                active?.let {
                    Spacer(Modifier.width(12.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                        Text("${money(it.packPrice)}${label("/包", "/ pack", lang)}",
                            Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = label("今日实际食用 (自购+接收)", "Consumed today", lang),
                value = "${summary.todayConsumed} $piece",
                detailText = label("自购 $self$piece · 接收 $received$piece", "Own $self · Received $received", lang),
                subtitle = goal?.let { label("目标 ${it.dailyLimit} $piece/天", "Goal ${it.dailyLimit} $piece/day", lang) }
                    ?: label("尚未设定目标", "No goal set", lang),
                icon = Icons.Rounded.Spa,
                iconTint = if (overLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(2f)
            )
            StatCard(
                title = label("今日递出", "Shared today", lang), value = if (lang == AppLanguage.EN) "${summary.todayShared}" else "${summary.todayShared} $piece",
                detailText = if (lang == AppLanguage.EN) "pieces" else null,
                subtitle = label("社交开销", "Given away", lang), icon = Icons.Rounded.CallMade,
                iconTint = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(title = label("今日开销", "Spent today", lang), value = money(summary.todayCost),
                subtitle = label("自购与递出", "Own + shared", lang), icon = Icons.Rounded.Payments,
                modifier = Modifier.weight(1f))
            StatCard(title = label("接收折算", "Received value", lang), value = money(saved),
                subtitle = label("按当前品牌价格估算", "Estimated at current price", lang),
                icon = Icons.Rounded.CardGiftcard, iconTint = Color(0xFF2E7D32),
                color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.weight(1f))
        }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(label("快捷记录", "Quick log", lang), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (elapsedText == null) label("暂无食用记录", "No consumption yet", lang)
                            else label("距上次 $elapsedText", "Since last consumption: $elapsedText", lang),
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple("SELF", label("食用 +1", "Consume +1", lang), Icons.Rounded.Spa),
                        Triple("SHARED_OUT", label("递出 +1", "Share +1", lang), Icons.Rounded.CallMade),
                        Triple("RECEIVED_IN", label("接收 +1", "Receive +1", lang), Icons.Rounded.CallReceived)
                    ).forEach { (kind, title, icon) ->
                        Button(
                            onClick = { active?.let { viewModel.addBetelLog(it, 1, kind) } },
                            enabled = active != null, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = if (kind == "SHARED_OUT") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 10.dp)
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(title, fontSize = 11.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
                OutlinedButton(onClick = { showLogDialog = true }, enabled = active != null, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(label("自定义记录", "Custom log", lang))
                }
            }
        }

        Card(shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(label("食用观察", "Consumption insight", lang), fontWeight = FontWeight.Bold)
                }
                Text(when {
                    dailyLimit == null -> label("设置每日上限，便于观察自己的食用变化。", "Set a daily limit to track your consumption.", lang)
                    overLimit -> label("今天已超过设定上限，可考虑减少后续食用。", "You've exceeded today's limit; consider consuming less.", lang)
                    summary.todayConsumed == 0 -> label("今天尚未食用，继续保持记录。", "No consumption recorded today. Keep tracking.", lang)
                    else -> label("今天已食用 ${summary.todayConsumed} 颗，距离上限还有 ${(dailyLimit - summary.todayConsumed).coerceAtLeast(0)} 颗。",
                        "${summary.todayConsumed} consumed today; ${(dailyLimit - summary.todayConsumed).coerceAtLeast(0)} until your limit.", lang)
                }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (dailyLimit != null && dailyLimit > 0) {
                    LinearProgressIndicator(progress = { (summary.todayConsumed.toFloat() / dailyLimit).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label(if (showAllLogs) "历史记录" else "今日记录", if (showAllLogs) "Recent logs" else "Today's logs", lang),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = { showAllLogs = !showAllLogs }) {
                Text(label(if (showAllLogs) "仅看今日" else "查看历史", if (showAllLogs) "Today" else "History", lang))
            }
        }
        if (visibleLogs.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                Text(label(if (showAllLogs) "还没有槟榔记录" else "今天还没有槟榔记录",
                    if (showAllLogs) "No betel logs yet" else "No betel logs today", lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        visibleLogs.forEach { log ->
            val kind = when (log.logType) {
                "SHARED_OUT" -> label("递出", "Shared", lang)
                "RECEIVED_IN" -> label("接收食用", "Received", lang)
                else -> label("自购食用", "Consumed", lang)
            }
            val badgeColor = when (log.logType) {
                "SHARED_OUT" -> MaterialTheme.colorScheme.secondary
                "RECEIVED_IN" -> Color(0xFF2E7D32)
                else -> MaterialTheme.colorScheme.primary
            }
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(6.dp), color = badgeColor.copy(alpha = 0.15f)) {
                        Text(kind, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = badgeColor,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("${log.productName} ×${log.quantity}$piece", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${if (showAllLogs) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(log.timestamp)) else DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(log.timestamp))} · ${money(log.cost)}",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { viewModel.deleteBetelLog(log.id) }) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = label("删除记录", "Delete log", lang))
                    }
                }
            }
        }
    }
    if (showLogDialog && active != null) BetelLogDialog(lang, onDismiss = { showLogDialog = false }) { count, kind ->
        viewModel.addBetelLog(active, count, kind)
        showLogDialog = false
    }
}

internal fun betelTrendItems(logs: List<BetelLog>, start: Long, end: Long): List<TrendDataItem> {
    if (start > end) return emptyList()
    val day = Calendar.getInstance().apply {
        timeInMillis = start
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val endDay = Calendar.getInstance().apply { timeInMillis = end; startOfDay() }
    val earliest = (endDay.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -365) }
    if (day.before(earliest)) day.timeInMillis = earliest.timeInMillis
    val result = mutableListOf<TrendDataItem>()
    while (day.timeInMillis <= end && result.size < 366) {
        val from = day.timeInMillis
        day.add(Calendar.DAY_OF_YEAR, 1)
        val dayLogs = logs.filter { it.timestamp >= from && it.timestamp < day.timeInMillis && it.timestamp <= end }
        val date = Calendar.getInstance().apply { timeInMillis = from }
        val consumedLogs = dayLogs.filter { it.logType != "SHARED_OUT" }.sortedBy { it.timestamp }
        val intervals = consumedLogs.zipWithNext { previous, current ->
            (current.timestamp - previous.timestamp) / 60_000
        }
        val peakHour = consumedLogs.groupBy { Calendar.getInstance().apply { timeInMillis = it.timestamp }.get(Calendar.HOUR_OF_DAY) }
            .maxByOrNull { (_, entries) -> entries.sumOf { it.quantity } }?.key
        result += TrendDataItem(
            dateLabel = "${date.get(Calendar.MONTH) + 1}/${date.get(Calendar.DAY_OF_MONTH)}",
            selfCount = dayLogs.filter { it.logType == "SELF" }.sumOf { it.quantity },
            sharedCount = dayLogs.filter { it.logType == "SHARED_OUT" }.sumOf { it.quantity },
            receivedCount = dayLogs.filter { it.logType == "RECEIVED_IN" }.sumOf { it.quantity },
            selfCost = dayLogs.filter { it.logType == "SELF" }.sumOf { it.cost },
            sharedCost = dayLogs.filter { it.logType == "SHARED_OUT" }.sumOf { it.cost },
            timestamp = from,
            avgIntervalMinutes = if (intervals.isEmpty()) 0 else intervals.average().roundToInt(),
            peakHourSlot = peakHour?.let { "%02d:00–%02d:00".format(it, it + 1) } ?: "—"
        )
    }
    return result
}

@Composable
fun BetelAnalysisScreen(viewModel: SmokingViewModel) {
    val logs by viewModel.betelLogs.collectAsStateWithLifecycle()
    val goal by viewModel.betelGoal.collectAsStateWithLifecycle()
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currency by viewModel.appCurrency.collectAsStateWithLifecycle()
    val now by rememberCurrentTime()
    val context = LocalContext.current
    var range by remember { mutableStateOf(TrendTimeRange.LAST_7_DAYS) }
    var chartType by remember { mutableStateOf(ChartType.BAR) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var selectedMonth by remember { mutableLongStateOf(now) }
    var customStart by remember { mutableLongStateOf(now - 6L * 24 * 60 * 60 * 1000) }
    var customEnd by remember { mutableLongStateOf(now) }
    val nowDay = Calendar.getInstance().apply { timeInMillis = now; startOfDay() }
    val firstDay = (nowDay.clone() as Calendar).apply {
        when (range) {
            TrendTimeRange.LAST_7_DAYS -> add(Calendar.DAY_OF_YEAR, -6)
            TrendTimeRange.THIS_WEEK -> {
                while (get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) add(Calendar.DAY_OF_YEAR, -1)
            }
            TrendTimeRange.THIS_MONTH -> set(Calendar.DAY_OF_MONTH, 1)
            TrendTimeRange.SPECIFIC_MONTH -> {
                timeInMillis = selectedMonth; startOfDay(); set(Calendar.DAY_OF_MONTH, 1)
            }
            TrendTimeRange.SPECIFIC_YEAR -> {
                timeInMillis = selectedMonth; startOfDay(); set(Calendar.DAY_OF_YEAR, 1)
            }
            TrendTimeRange.CUSTOM_RANGE -> { timeInMillis = customStart; startOfDay() }
        }
    }
    val lastDay = (nowDay.clone() as Calendar).apply {
        when (range) {
            TrendTimeRange.SPECIFIC_MONTH -> {
                timeInMillis = selectedMonth; startOfDay(); set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            }
            TrendTimeRange.SPECIFIC_YEAR -> {
                timeInMillis = selectedMonth; startOfDay(); set(Calendar.DAY_OF_YEAR, getActualMaximum(Calendar.DAY_OF_YEAR))
            }
            TrendTimeRange.CUSTOM_RANGE -> { timeInMillis = customEnd; startOfDay() }
            else -> Unit
        }
    }
    val end = minOf(now, (lastDay.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis - 1)
    val trendData = remember(logs, firstDay.timeInMillis, end) { betelTrendItems(logs, firstDay.timeInMillis, end) }
    val consumed = trendData.sumOf { it.selfCount + it.receivedCount }
    val shared = trendData.sumOf { it.sharedCount }
    val selfCost = trendData.sumOf { it.selfCost }
    val sharedCost = trendData.sumOf { it.sharedCost }
    val money: (Double) -> String = { "${currency.symbol}${String.format(Locale.getDefault(), "%.2f", it)}" }
    val piece = label("颗", "pcs", lang)
    val todaySummary = remember(logs, now) { summarizeBetel(logs, now) }
    val monthStart = (nowDay.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
    val monthSpent = logs.filter { it.timestamp in monthStart..now }.sumOf { it.cost }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)
        .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label("时间范围", "Time range", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TrendTimeRange.values().forEach { choice ->
                FilterChip(selected = range == choice, onClick = { range = choice; selectedIndex = -1 },
                    label = { Text(choice.getLabel(lang), fontSize = 12.sp) })
            }
        }
        if (range == TrendTimeRange.SPECIFIC_MONTH || range == TrendTimeRange.SPECIFIC_YEAR || range == TrendTimeRange.CUSTOM_RANGE) {
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (range == TrendTimeRange.CUSTOM_RANGE) {
                        OutlinedButton(onClick = { pickBetelDate(context, customStart) { customStart = it; selectedIndex = -1 } }, modifier = Modifier.weight(1f)) {
                            Text(DateFormat.getDateInstance(DateFormat.SHORT).format(Date(customStart)), fontSize = 11.sp, maxLines = 1)
                        }
                        Text(label("至", "to", lang))
                        OutlinedButton(onClick = { pickBetelDate(context, customEnd) { customEnd = it; selectedIndex = -1 } }, modifier = Modifier.weight(1f)) {
                            Text(DateFormat.getDateInstance(DateFormat.SHORT).format(Date(customEnd)), fontSize = 11.sp, maxLines = 1)
                        }
                    } else {
                        Text(label("目标日期", "Target date", lang), fontSize = 12.sp, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { pickBetelDate(context, selectedMonth) { selectedMonth = it; selectedIndex = -1 } }) {
                            Text(SimpleDateFormat(if (range == TrendTimeRange.SPECIFIC_MONTH) "yyyy/MM" else "yyyy", Locale.getDefault()).format(Date(selectedMonth)))
                        }
                    }
                }
            }
        }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(label("槟榔食用与行为趋势", "Betel consumption trends", lang), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(title = label("日均食用", "Daily average", lang),
                        value = String.format(Locale.getDefault(), "%.1f $piece", consumed.toDouble() / trendData.size.coerceAtLeast(1)),
                        subtitle = label("合计 $consumed $piece", "Total $consumed $piece", lang), modifier = Modifier.weight(1f))
                    StatCard(title = label("期间递出", "Shared in period", lang), value = "$shared $piece",
                        subtitle = label("不计入食用量", "Excluded from consumption", lang), modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(title = label("最高单日", "Peak day", lang),
                        value = "${trendData.maxOfOrNull { it.selfCount + it.receivedCount } ?: 0} $piece",
                        subtitle = label("实际食用", "Actual consumption", lang), modifier = Modifier.weight(1f))
                    StatCard(title = label("控制目标", "Daily goal", lang),
                        value = goal?.let { "${it.dailyLimit} $piece" } ?: label("未设定", "Not set", lang),
                        subtitle = label("今日 ${todaySummary.todayConsumed} $piece", "Today ${todaySummary.todayConsumed} $piece", lang), modifier = Modifier.weight(1f))
                }
            }
        }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label("食用习惯观察", "Consumption pattern", lang), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                val activeDays = trendData.count { it.selfCount + it.receivedCount > 0 }
                val peakDay = trendData.maxByOrNull { it.selfCount + it.receivedCount }
                Text(label("记录天数 $activeDays / ${trendData.size} · 最高食用日 ${peakDay?.dateLabel ?: "—"}",
                    "$activeDays / ${trendData.size} days logged · Peak day ${peakDay?.dateLabel ?: "—"}", lang),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (peakDay != null && peakDay.selfCount + peakDay.receivedCount > 0) {
                    Text(label("高峰时段 ${peakDay.peakHourSlot} · ${if (peakDay.avgIntervalMinutes > 0) "当日平均间隔 ${peakDay.avgIntervalMinutes} 分钟" else "单次记录"}",
                        "Peak window ${peakDay.peakHourSlot} · ${if (peakDay.avgIntervalMinutes > 0) "Average gap ${peakDay.avgIntervalMinutes} min" else "Single log"}", lang),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${range.getLabel(lang)} - ${chartType.getLabel(lang)}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChartType.values().forEach { type ->
                        FilterChip(selected = chartType == type, onClick = { chartType = type; selectedIndex = -1 },
                            label = { Text(type.getLabel(lang), fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                    }
                }
                val selected = trendData.getOrNull(selectedIndex)
                Surface(shape = RoundedCornerShape(8.dp), color = if (selected != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    Text(selected?.let {
                        label("【${it.dateLabel}】食用 ${it.selfCount + it.receivedCount}$piece（自购 ${it.selfCount}、接收 ${it.receivedCount}） · 递出 ${it.sharedCount}$piece · 开销 ${money(it.selfCost + it.sharedCost)}",
                            "[${it.dateLabel}] Consumed ${it.selfCount + it.receivedCount} (own ${it.selfCount}, received ${it.receivedCount}) · Shared ${it.sharedCount} · Spent ${money(it.selfCost + it.sharedCost)}", lang)
                    } ?: label("点击图表查看当日详情", "Tap the chart for daily details", lang),
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically) {
                    LegendItem(MaterialTheme.colorScheme.primary, label("自购食用", "Own", lang))
                    Spacer(Modifier.width(12.dp))
                    LegendItem(MaterialTheme.colorScheme.secondary, label("递出", "Shared", lang))
                    Spacer(Modifier.width(12.dp))
                    LegendItem(Color(0xFF2E7D32), label("接收食用", "Received", lang))
                }
                if (trendData.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().height(240.dp)) {
                        TrendChartComposable(trendData, chartType, selectedIndex, { selectedIndex = it }, lang, "颗")
                    }
                } else {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(label("所选时间范围暂无记录", "No records in this range", lang))
                    }
                }
                selected?.let {
                    Text(label("${it.dateLabel} · 食用 ${it.selfCount + it.receivedCount}$piece · 递出 ${it.sharedCount}$piece",
                        "${it.dateLabel} · Consumed ${it.selfCount + it.receivedCount} · Shared ${it.sharedCount}", lang),
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(label("槟榔开销与社交分析", "Betel spending and sharing", lang), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text(label("自购开销", "Own spending", lang), fontSize = 11.sp); Text(money(selfCost), fontWeight = FontWeight.Bold) }
                    Column { Text(label("递出开销", "Shared spending", lang), fontSize = 11.sp); Text(money(sharedCost), fontWeight = FontWeight.Bold) }
                    Column { Text(label("期间食用", "Consumed", lang), fontSize = 11.sp); Text("$consumed $piece", fontWeight = FontWeight.Bold) }
                }
                HorizontalDivider()
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label("期间总开销", "Total spent", lang), fontWeight = FontWeight.Bold)
                    Text(money(selfCost + sharedCost), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
                goal?.monthlyBudget?.let { budget ->
                    Text(label("本月 ${money(monthSpent)} / 预算 ${money(budget)}", "This month ${money(monthSpent)} / budget ${money(budget)}", lang), fontSize = 12.sp)
                    if (budget > 0) LinearProgressIndicator(progress = { (monthSpent / budget).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    if (monthSpent > budget) Text(label("已超出月预算", "Over monthly budget", lang), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun Calendar.startOfDay() {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}

private fun pickBetelDate(context: android.content.Context, initial: Long, onSelect: (Long) -> Unit) {
    val date = Calendar.getInstance().apply { timeInMillis = initial }
    DatePickerDialog(context, { _, year, month, day ->
        onSelect(Calendar.getInstance().apply { set(year, month, day); startOfDay() }.timeInMillis)
    }, date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH)).show()
}

@Composable
fun BetelBrandsScreen(viewModel: SmokingViewModel) {
    val products by viewModel.betelProducts.collectAsStateWithLifecycle()
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currency by viewModel.appCurrency.collectAsStateWithLifecycle()
    var showProductDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var sortByPrice by remember { mutableStateOf(false) }
    val active = products.firstOrNull { it.isActive } ?: products.firstOrNull()
    val visibleProducts = products.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
        .let { matching -> if (sortByPrice) matching.sortedBy { it.packPrice / it.piecesPerPack } else matching.sortedBy { it.name.lowercase(Locale.getDefault()) } }
    val money: (Double) -> String = { "${currency.symbol}${String.format(Locale.getDefault(), "%.2f", it)}" }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(label("槟榔品牌盒", "Betel brands", lang), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(label("管理价格、颗数与当前品牌", "Manage prices, pack sizes and active brand", lang),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { showProductDialog = true }, shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(label("添加", "Add", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(label("右滑设为当前品牌 · 左滑删除 · 点箭头展开编辑",
            "Swipe right to select · left to delete · tap arrow to edit", lang),
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it },
            label = { Text(label("搜索品牌", "Search brands", lang)) }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Sort, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(label("排序:", "Sort:", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            FilterChip(selected = !sortByPrice, onClick = { sortByPrice = false }, label = { Text(label("名称", "Name", lang)) })
            FilterChip(selected = sortByPrice, onClick = { sortByPrice = true }, label = { Text(label("每颗价格", "Price per piece", lang)) })
        }
        if (products.isEmpty()) {
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text(label("先添加一款槟榔，填写每包价格和颗数。", "Add a product with its pack price and pieces per pack.", lang),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (visibleProducts.isEmpty()) {
            Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text(label("没有匹配的品牌", "No matching brands", lang), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visibleProducts, key = { it.id }) { product ->
                    val selected = product.id == active?.id
                    BetelBrandCard(product, selected, lang, money,
                        onSelect = { viewModel.setActiveBetelProduct(product.id) },
                        onDelete = { productToDelete = product.id },
                        onUpdate = { name, price, pieces -> viewModel.updateBetelProduct(product, name, price, pieces) })
                }
            }
        }
    }
    if (productToDelete != 0) AlertDialog(
        onDismissRequest = { productToDelete = 0 },
        title = { Text(label("删除品牌？", "Delete brand?", lang)) },
        text = { Text(label("已有记录会保留名称和当时的开销。", "Existing logs retain their name and original cost.", lang)) },
        confirmButton = { TextButton(onClick = { viewModel.deleteBetelProduct(productToDelete); productToDelete = 0 }) { Text(label("删除", "Delete", lang)) } },
        dismissButton = { TextButton(onClick = { productToDelete = 0 }) { Text(label("取消", "Cancel", lang)) } }
    )
    if (showProductDialog) BetelProductDialog(lang, onDismiss = { showProductDialog = false }) { name, price, pieces ->
        viewModel.addBetelProduct(name, price, pieces)
        showProductDialog = false
    }

}

@Composable
private fun BetelBrandCard(
    product: BetelProduct, selected: Boolean, lang: AppLanguage,
    money: (Double) -> String, onSelect: () -> Unit, onDelete: () -> Unit,
    onUpdate: (String, Double, Int) -> Unit
) {
    var expanded by remember(product.id) { mutableStateOf(false) }
    var name by remember(product.id, product.name) { mutableStateOf(product.name) }
    var price by remember(product.id, product.packPrice) { mutableStateOf(product.packPrice.toString()) }
    var pieces by remember(product.id, product.piecesPerPack) { mutableStateOf(product.piecesPerPack.toString()) }
    var offsetX by remember(product.id) { mutableFloatStateOf(0f) }
    val animatedX by animateFloatAsState(offsetX, spring(stiffness = Spring.StiffnessMediumLow), label = "betel_swipe")
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
        Row(Modifier.matchParentSize().background(when {
            offsetX > 20f -> Color(0xFF2E7D32)
            offsetX < -20f -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.surfaceVariant
        }).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (offsetX > 0) Arrangement.Start else Arrangement.End) {
            if (offsetX > 20f) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(label("设为当前", "Set active", lang), color = Color.White, fontWeight = FontWeight.Bold)
            } else if (offsetX < -20f) {
                Text(label("删除", "Delete", lang), color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = Color.White)
            }
        }
        Card(modifier = Modifier.fillMaxWidth().offset { IntOffset(animatedX.roundToInt(), 0) }
            .pointerInput(product.id, selected) {
                detectHorizontalDragGestures(onDragEnd = {
                    if (offsetX > 100f && !selected) onSelect()
                    if (offsetX < -100f) onDelete()
                    offsetX = 0f
                }, onDragCancel = { offsetX = 0f }, onHorizontalDrag = { _, amount ->
                    offsetX = (offsetX + amount).coerceIn(-180f, 180f)
                })
            }.animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface),
            border = BorderStroke(if (selected) 1.5.dp else 1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(42.dp), shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Spa, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(label("每包 ${product.piecesPerPack} 颗", "${product.piecesPerPack} pieces per pack", lang),
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (selected) Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primary) {
                        Text(label("当前", "Active", lang), Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                        Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = label("展开编辑", "Toggle details", lang))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        label("每包", "Pack", lang) to money(product.packPrice),
                        label("颗数", "Pieces", lang) to "${product.piecesPerPack}",
                        label("每颗", "Each", lang) to money(product.packPrice / product.piecesPerPack.coerceAtLeast(1))
                    ).forEach { (caption, value) ->
                        Surface(Modifier.weight(1f), shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                            Column(Modifier.padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(caption, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary, maxLines = 1)
                            }
                        }
                    }
                }
                if (expanded) {
                    HorizontalDivider()
                    OutlinedTextField(name, { name = it }, label = { Text(label("品牌名称", "Brand name", lang)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(price, { price = it }, label = { Text(label("每包价格", "Pack price", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(pieces, { pieces = it }, label = { Text(label("每包颗数", "Pieces per pack", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDelete) { Text(label("删除", "Delete", lang)) }
                        Row {
                            if (!selected) TextButton(onClick = onSelect) { Text(label("设为当前", "Set active", lang)) }
                            Button(onClick = {
                                onUpdate(name.trim(), price.toDouble(), pieces.toInt()); expanded = false
                            }, enabled = name.isNotBlank() && (price.toDoubleOrNull() ?: -1.0).let { it >= 0 && it.isFinite() }
                                && (pieces.toIntOrNull() ?: 0) > 0) { Text(label("保存", "Save", lang)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberCurrentTime(): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now.longValue = System.currentTimeMillis()
            kotlinx.coroutines.delay(60_000)
        }
    }
    return now
}

@Composable
private fun BetelProductDialog(lang: AppLanguage, existing: BetelProduct? = null, onDismiss: () -> Unit, onSave: (String, Double, Int) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var price by remember(existing) { mutableStateOf(existing?.packPrice?.toString() ?: "") }
    var pieces by remember(existing) { mutableStateOf(existing?.piecesPerPack?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) label("添加槟榔品类", "Add betel product", lang) else label("编辑槟榔品类", "Edit betel product", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(label("名称", "Name", lang)) }, singleLine = true)
                OutlinedTextField(price, { price = it }, label = { Text(label("每包价格", "Pack price", lang)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(pieces, { pieces = it }, label = { Text(label("每包颗数", "Pieces per pack", lang)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, price.toDouble(), pieces.toInt()) }, enabled = name.isNotBlank() && (price.toDoubleOrNull() ?: -1.0).let { it >= 0 && it.isFinite() } && (pieces.toIntOrNull() ?: 0) > 0) { Text(label("保存", "Save", lang)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(label("取消", "Cancel", lang)) } }
    )
}

@Composable
internal fun BetelGoalDialog(lang: AppLanguage, goal: BetelGoal?, onDismiss: () -> Unit, onSave: (Int, Double?) -> Unit) {
    var daily by remember(goal) { mutableStateOf(goal?.dailyLimit?.toString() ?: "") }
    var budget by remember(goal) { mutableStateOf(goal?.monthlyBudget?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label("槟榔目标", "Betel goal", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(daily, { daily = it }, label = { Text(label("每日上限（颗，0 表示不食用）", "Daily limit (pieces; 0 means none)", lang)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(budget, { budget = it }, label = { Text(label("月预算（可选）", "Monthly budget (optional)", lang)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(daily.toInt(), budget.takeIf { it.isNotBlank() }?.toDouble()) }, enabled = (daily.toIntOrNull() ?: -1) >= 0 && (budget.isBlank() || (budget.toDoubleOrNull() ?: -1.0).let { it >= 0 && it.isFinite() })) { Text(label("保存", "Save", lang)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(label("取消", "Cancel", lang)) } }
    )
}

@Composable
private fun BetelLogDialog(lang: AppLanguage, onDismiss: () -> Unit, onSave: (Int, String) -> Unit) {
    var count by remember { mutableStateOf("1") }
    var kind by remember { mutableStateOf("SELF") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label("记录槟榔", "Log betel nut", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(count, { count = it }, label = { Text(label("颗数", "Pieces", lang)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                listOf(
                    "SELF" to label("自购食用", "Consumed", lang),
                    "SHARED_OUT" to label("递给他人", "Shared out", lang),
                    "RECEIVED_IN" to label("接受并食用", "Received and consumed", lang)
                ).forEach { (value, text) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = kind == value, onClick = { kind = value })
                        Text(text)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(count.toInt(), kind) }, enabled = (count.toIntOrNull() ?: 0) > 0) { Text(label("记录", "Log", lang)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(label("取消", "Cancel", lang)) } }
    )
}
