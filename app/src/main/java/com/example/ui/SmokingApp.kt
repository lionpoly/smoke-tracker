package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Cigarette
import com.example.data.model.SmokingLog
import com.example.data.sync.SyncState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmokingApp(viewModel: SmokingViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SmokeFree,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedTab) {
                                0 -> "吸烟跟踪Guard"
                                1 -> "趋势与统计"
                                2 -> "我的烟盒"
                                else -> "设置与备份"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "首页") },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.BarChart, contentDescription = "趋势") },
                    label = { Text("趋势") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Inventory2, contentDescription = "烟盒") },
                    label = { Text("烟盒") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "设置") },
                    label = { Text("设置") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(viewModel = viewModel, stats = stats)
                1 -> ChartsScreen(viewModel = viewModel, stats = stats)
                2 -> StoreScreen(viewModel = viewModel)
                3 -> SettingsScreen(viewModel = viewModel, stats = stats)
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: SmokingViewModel, stats: SmokingStats) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val cigarettes by viewModel.cigarettes.collectAsStateWithLifecycle()
    val aiAdviceState by viewModel.aiAdviceState.collectAsStateWithLifecycle()

    var showAddLogDialog by remember { mutableStateOf(false) }

    val activeCigarette = cigarettes.firstOrNull { it.isActive } ?: cigarettes.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warning Banner if Over Limit
        AnimatedVisibility(visible = stats.isOverLimit) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "今日吸烟（${stats.todayTotalCount}支）已超出目标限制（${stats.currentGoalLimit}支），请注意健康并尽量少抽！",
                        color = Color(0xFFC62828),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Active Cigarette Info Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "当前使用烟草品牌",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeCigarette?.name ?: "未设置 (默认中华)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "¥${String.format(Locale.getDefault(), "%.2f", activeCigarette?.price ?: 25.0)} / 包",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Today Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "今日自抽",
                value = "${stats.todaySelfCount} 支",
                subtitle = "目标 ${stats.currentGoalLimit} 支",
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "社交递烟",
                value = "${stats.todaySharedCount} 支",
                subtitle = "分享他人",
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "社交接烟",
                value = "${stats.todayReceivedCount} 支",
                subtitle = "免费蹭烟",
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(1f)
            )
        }

        // Today Financial Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                title = "今日吸烟开销",
                value = "¥${String.format(Locale.getDefault(), "%.2f", stats.todayCost)}",
                subtitle = "自购+递烟花费",
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "接烟省下金额",
                value = "¥${String.format(Locale.getDefault(), "%.2f", stats.todaySavedFromReceived)}",
                subtitle = "他请客省下的",
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Action Buttons (3 Scenes: Self, Shared Out, Received In)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "快速打卡极速记录",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick 1: Self
                    Button(
                        onClick = {
                            val cigId = activeCigarette?.id ?: 1
                            viewModel.addSmokingLog(cigId, 1, "SELF", "极速记录")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("🚬 自购自抽 +1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Quick 2: Shared Out
                    Button(
                        onClick = {
                            val cigId = activeCigarette?.id ?: 1
                            viewModel.addSmokingLog(cigId, 1, "SHARED_OUT", "社交递烟")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("🤝 社交递烟 +1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Quick 3: Received In
                    Button(
                        onClick = {
                            val cigId = activeCigarette?.id ?: 1
                            viewModel.addSmokingLog(cigId, 1, "RECEIVED_IN", "他人递烟")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Text("🎁 社交接烟 +1", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showAddLogDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("自定义多支 / 备注记录", fontSize = 13.sp)
                }
            }
        }

        // Gemini AI Smart Advice Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI 戒烟教练分析与建议",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.fetchAiAdvice() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "刷新 AI 建议",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (val state = aiAdviceState) {
                    is AiAdviceState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("AI 戒烟教练正在根据您的最新吸烟与社交接烟数据分析中...", fontSize = 13.sp)
                        }
                    }
                    is AiAdviceState.Success -> {
                        Text(
                            text = state.advice,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is AiAdviceState.Error -> {
                        Text(text = state.error, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {}
                }
            }
        }

        // Recent Logs List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "今日打卡记录明细",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "共 ${logs.size} 条",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "今日尚未打卡，点击下方按钮开始记录第一支吧！",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        } else {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                logs.take(10).forEach { log ->
                    val cig = cigarettes.firstOrNull { it.id == log.cigaretteId }
                    val logTypeName = when (log.logType) {
                        "SHARED_OUT" -> "社交递烟"
                        "RECEIVED_IN" -> "社交接烟"
                        else -> "自购自抽"
                    }
                    val badgeColor = when (log.logType) {
                        "SHARED_OUT" -> MaterialTheme.colorScheme.secondary
                        "RECEIVED_IN" -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = logTypeName,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = badgeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = "${cig?.name ?: "香烟"} x${log.quantity}支",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (log.note.isNotEmpty()) {
                                        Text(
                                            text = "备注: ${log.note}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = sdf.format(Date(log.timestamp)),
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = if (log.logType == "RECEIVED_IN") "¥0.00 (免费)" else "¥${String.format(Locale.getDefault(), "%.2f", log.cost)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.logType == "RECEIVED_IN") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteSmokingLog(log) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "删除记录",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddLogDialog) {
        AddLogDialog(
            cigarettes = cigarettes,
            onDismiss = { showAddLogDialog = false },
            onConfirm = { cigId, qty, logType, note ->
                viewModel.addSmokingLog(cigId, qty, logType, note)
                showAddLogDialog = false
            }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 11.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ChartsScreen(viewModel: SmokingViewModel, stats: SmokingStats) {
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsStateWithLifecycle()
    val selectedChartType by viewModel.selectedChartType.collectAsStateWithLifecycle()
    val trendData by viewModel.trendData.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Time Range Selector Chips
        Text(
            text = "选择统计分析时间范围",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TrendTimeRange.values().forEach { range ->
                FilterChip(
                    selected = selectedTimeRange == range,
                    onClick = { viewModel.selectedTimeRange.value = range },
                    label = { Text(range.label, fontSize = 12.sp) },
                    leadingIcon = if (selectedTimeRange == range) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        // Additional date selection section for Specific Month, Specific Year, or Custom Range
        AnimatedVisibility(
            visible = selectedTimeRange == TrendTimeRange.SPECIFIC_MONTH ||
                    selectedTimeRange == TrendTimeRange.SPECIFIC_YEAR ||
                    selectedTimeRange == TrendTimeRange.CUSTOM_RANGE
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedTimeRange == TrendTimeRange.SPECIFIC_MONTH || selectedTimeRange == TrendTimeRange.SPECIFIC_YEAR) {
                        val selectedCal by viewModel.selectedMonthYear.collectAsStateWithLifecycle()
                        val sdf = if (selectedTimeRange == TrendTimeRange.SPECIFIC_MONTH)
                            SimpleDateFormat("yyyy年MM月", Locale.getDefault())
                        else
                            SimpleDateFormat("yyyy年", Locale.getDefault())

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "目标日期: ${sdf.format(selectedCal.time)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, day)
                                            }
                                            viewModel.selectedMonthYear.value = newCal
                                        },
                                        selectedCal.get(Calendar.YEAR),
                                        selectedCal.get(Calendar.MONTH),
                                        selectedCal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("点击选择${if (selectedTimeRange == TrendTimeRange.SPECIFIC_MONTH) "月份" else "年份"}", fontSize = 12.sp)
                            }
                        }
                    } else if (selectedTimeRange == TrendTimeRange.CUSTOM_RANGE) {
                        val customStart by viewModel.customStartDate.collectAsStateWithLifecycle()
                        val customEnd by viewModel.customEndDate.collectAsStateWithLifecycle()
                        val dateSdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

                        val startText = customStart?.let { dateSdf.format(Date(it)) } ?: "选择开始日期"
                        val endText = customEnd?.let { dateSdf.format(Date(it)) } ?: "选择结束日期"

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("自定义时间范围:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    customStart?.let { cal.timeInMillis = it }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val selected = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, day)
                                                set(Calendar.HOUR_OF_DAY, 0)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                            }.timeInMillis
                                            viewModel.customStartDate.value = selected
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(startText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }

                            Text("至", fontSize = 12.sp, color = Color.Gray)

                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    customEnd?.let { cal.timeInMillis = it }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, day ->
                                            val selected = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, day)
                                                set(Calendar.HOUR_OF_DAY, 23)
                                                set(Calendar.MINUTE, 59)
                                                set(Calendar.SECOND, 59)
                                            }.timeInMillis
                                            viewModel.customEndDate.value = selected
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(endText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // Chart Type Selector
        Text(
            text = "图表展示形式",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChartType.values().forEach { type ->
                InputChip(
                    selected = selectedChartType == type,
                    onClick = { viewModel.selectedChartType.value = type },
                    label = { Text(type.label, fontSize = 12.sp) }
                )
            }
        }

        // Financial Expenditure Analysis Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("吸烟开销与社交性价比分析", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                val totalSelfCost = trendData.sumOf { it.selfCost }
                val totalSharedCost = trendData.sumOf { it.sharedCost }
                val totalReceivedSaved = trendData.sumOf { it.receivedSaved }
                val totalSpent = totalSelfCost + totalSharedCost

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("自购自抽支出", fontSize = 11.sp, color = Color.Gray)
                        Text("¥${String.format(Locale.getDefault(), "%.2f", totalSelfCost)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column {
                        Text("社交递烟支出", fontSize = 11.sp, color = Color.Gray)
                        Text("¥${String.format(Locale.getDefault(), "%.2f", totalSharedCost)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    Column {
                        Text("社交接烟节省", fontSize = 11.sp, color = Color.Gray)
                        Text("¥${String.format(Locale.getDefault(), "%.2f", totalReceivedSaved)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2E7D32))
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("当前时段实际总支金:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("¥${String.format(Locale.getDefault(), "%.2f", totalSpent)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Custom Canvas Chart rendering
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${selectedTimeRange.label} - ${selectedChartType.label}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = MaterialTheme.colorScheme.primary, text = "自购自抽")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = MaterialTheme.colorScheme.secondary, text = "社交递烟")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = Color(0xFF2E7D32), text = "社交接烟")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chart Graphic Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    if (trendData.isEmpty()) {
                        Text("暂无当前时段的数据记录", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    } else {
                        TrendChartComposable(
                            trendData = trendData,
                            chartType = selectedChartType
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun TrendChartComposable(
    trendData: List<TrendDataItem>,
    chartType: ChartType
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val greenColor = Color(0xFF2E7D32)

    val maxVal = trendData.maxOfOrNull { it.selfCount + it.sharedCount + it.receivedCount }?.coerceAtLeast(5) ?: 5

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val bottomMargin = 40f
        val topMargin = 20f
        val usableHeight = height - bottomMargin - topMargin
        val itemCount = trendData.size
        val stepX = width / itemCount.coerceAtLeast(1)

        when (chartType) {
            ChartType.BAR -> {
                val barWidth = (stepX * 0.5f).coerceAtMost(24.dp.toPx())
                trendData.forEachIndexed { index, item ->
                    val x = index * stepX + (stepX - barWidth) / 2

                    val selfH = (item.selfCount.toFloat() / maxVal) * usableHeight
                    val sharedH = (item.sharedCount.toFloat() / maxVal) * usableHeight
                    val recH = (item.receivedCount.toFloat() / maxVal) * usableHeight

                    var currentY = height - bottomMargin

                    // Draw Received bar (Green)
                    if (recH > 0) {
                        drawRect(
                            color = greenColor,
                            topLeft = Offset(x, currentY - recH),
                            size = Size(barWidth, recH)
                        )
                        currentY -= recH
                    }

                    // Draw Shared bar (Secondary)
                    if (sharedH > 0) {
                        drawRect(
                            color = secondaryColor,
                            topLeft = Offset(x, currentY - sharedH),
                            size = Size(barWidth, sharedH)
                        )
                        currentY -= sharedH
                    }

                    // Draw Self bar (Primary)
                    if (selfH > 0) {
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(x, currentY - selfH),
                            size = Size(barWidth, selfH)
                        )
                    }
                }
            }
            ChartType.LINE -> {
                val pathSelf = Path()
                val pathShared = Path()
                val pathRec = Path()

                trendData.forEachIndexed { index, item ->
                    val x = index * stepX + stepX / 2

                    val ySelf = height - bottomMargin - (item.selfCount.toFloat() / maxVal) * usableHeight
                    val yShared = height - bottomMargin - (item.sharedCount.toFloat() / maxVal) * usableHeight
                    val yRec = height - bottomMargin - (item.receivedCount.toFloat() / maxVal) * usableHeight

                    if (index == 0) {
                        pathSelf.moveTo(x, ySelf)
                        pathShared.moveTo(x, yShared)
                        pathRec.moveTo(x, yRec)
                    } else {
                        pathSelf.lineTo(x, ySelf)
                        pathShared.lineTo(x, yShared)
                        pathRec.lineTo(x, yRec)
                    }

                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(x, ySelf))
                    drawCircle(color = secondaryColor, radius = 4.dp.toPx(), center = Offset(x, yShared))
                    drawCircle(color = greenColor, radius = 4.dp.toPx(), center = Offset(x, yRec))
                }

                drawPath(pathSelf, color = primaryColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                drawPath(pathShared, color = secondaryColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                drawPath(pathRec, color = greenColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            }
            ChartType.SCATTER -> {
                trendData.forEachIndexed { index, item ->
                    val x = index * stepX + stepX / 2
                    val ySelf = height - bottomMargin - (item.selfCount.toFloat() / maxVal) * usableHeight
                    val yShared = height - bottomMargin - (item.sharedCount.toFloat() / maxVal) * usableHeight
                    val yRec = height - bottomMargin - (item.receivedCount.toFloat() / maxVal) * usableHeight

                    if (item.selfCount > 0) {
                        drawCircle(color = primaryColor, radius = 8.dp.toPx(), center = Offset(x, ySelf))
                    }
                    if (item.sharedCount > 0) {
                        drawCircle(color = secondaryColor, radius = 7.dp.toPx(), center = Offset(x, yShared))
                    }
                    if (item.receivedCount > 0) {
                        drawCircle(color = greenColor, radius = 6.dp.toPx(), center = Offset(x, yRec))
                    }
                }
            }
            ChartType.PIE -> {
                val totalSelf = trendData.sumOf { it.selfCount }.toFloat()
                val totalShared = trendData.sumOf { it.sharedCount }.toFloat()
                val totalRec = trendData.sumOf { it.receivedCount }.toFloat()
                val grandTotal = (totalSelf + totalShared + totalRec).coerceAtLeast(1f)

                val sweepSelf = (totalSelf / grandTotal) * 360f
                val sweepShared = (totalShared / grandTotal) * 360f
                val sweepRec = (totalRec / grandTotal) * 360f

                val diameter = minOf(width, height - bottomMargin) * 0.75f
                val topLeftX = (width - diameter) / 2
                val topLeftY = (height - bottomMargin - diameter) / 2

                drawArc(
                    color = primaryColor,
                    startAngle = 0f,
                    sweepAngle = sweepSelf,
                    useCenter = true,
                    topLeft = Offset(topLeftX, topLeftY),
                    size = Size(diameter, diameter)
                )
                drawArc(
                    color = secondaryColor,
                    startAngle = sweepSelf,
                    sweepAngle = sweepShared,
                    useCenter = true,
                    topLeft = Offset(topLeftX, topLeftY),
                    size = Size(diameter, diameter)
                )
                drawArc(
                    color = greenColor,
                    startAngle = sweepSelf + sweepShared,
                    sweepAngle = sweepRec,
                    useCenter = true,
                    topLeft = Offset(topLeftX, topLeftY),
                    size = Size(diameter, diameter)
                )
            }
        }
    }
}

@Composable
fun StoreScreen(viewModel: SmokingViewModel) {
    val cigarettes by viewModel.cigarettes.collectAsStateWithLifecycle()
    var showAddCigaretteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Fix header button squish issue
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "我的烟盒库 (Cigarette Box)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "管理常用香烟种类与零售价格规则",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = { showAddCigaretteDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("添加烟草", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (cigarettes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "烟盒为空，请点击右上角【添加烟草】", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cigarettes) { cig ->
                    CigaretteItemCard(
                        cigarette = cig,
                        onUpdate = { updatedCig -> viewModel.updateCigarette(updatedCig) },
                        onSetActive = { viewModel.setActiveCigarette(cig.id) },
                        onDelete = { viewModel.deleteCigarette(cig) }
                    )
                }
            }
        }
    }

    if (showAddCigaretteDialog) {
        AddCigaretteDialog(
            onDismiss = { showAddCigaretteDialog = false },
            onConfirm = { name, price, packSize, priceType, cartonPrice, packsPerCarton ->
                viewModel.addCigarette(name, price, packSize, priceType, cartonPrice, packsPerCarton)
                showAddCigaretteDialog = false
            }
        )
    }
}

// Expandable Card Item for Cigarette Box with inline editing & active toggle
@Composable
fun CigaretteItemCard(
    cigarette: Cigarette,
    onUpdate: (Cigarette) -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf(cigarette.name) }
    var editPriceType by remember { mutableStateOf(cigarette.priceType) }
    var editPrice by remember { mutableStateOf(cigarette.price.toString()) }
    var editCartonPrice by remember { mutableStateOf(cigarette.cartonPrice.toString()) }
    var editPackSize by remember { mutableStateOf(cigarette.packSize.toString()) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cigarette.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (cigarette.isActive) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "使用中",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = cigarette.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!cigarette.isActive) {
                        TextButton(onClick = onSetActive) {
                            Text("设为当前", fontSize = 12.sp)
                        }
                    }

                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = "展开编辑"
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Pricing Badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("单包: ¥${String.format(Locale.getDefault(), "%.2f", cigarette.price)}") }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("单条: ¥${String.format(Locale.getDefault(), "%.2f", cigarette.cartonPrice)}") }
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text("${cigarette.packSize}支/包") }
                )
            }

            // Expanded Edit Mode
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider()

                    Text("编辑香烟信息与规则:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("品牌名称") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = editPriceType == "PACK",
                            onClick = { editPriceType = "PACK" },
                            label = { Text("按单包计算") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = editPriceType == "CARTON",
                            onClick = { editPriceType = "CARTON" },
                            label = { Text("按单条(10包)计算") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (editPriceType == "PACK") {
                        OutlinedTextField(
                            value = editPrice,
                            onValueChange = { editPrice = it },
                            label = { Text("单包零售价 (元)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = editCartonPrice,
                            onValueChange = { editCartonPrice = it },
                            label = { Text("单条(10包)零售价 (元)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = {
                            val p = editPrice.toDoubleOrNull() ?: cigarette.price
                            val cp = editCartonPrice.toDoubleOrNull() ?: cigarette.cartonPrice
                            val ps = editPackSize.toIntOrNull() ?: cigarette.packSize
                            val calcPrice = if (editPriceType == "CARTON") cp / 10.0 else p

                            onUpdate(
                                cigarette.copy(
                                    name = editName,
                                    price = calcPrice,
                                    cartonPrice = if (editPriceType == "PACK") p * 10.0 else cp,
                                    priceType = editPriceType,
                                    packSize = ps
                                )
                            )
                            expanded = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存修改", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: SmokingViewModel, stats: SmokingStats) {
    val activeGoal by viewModel.activeGoal.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val isDemoMode by viewModel.isDemoMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var limitInput by remember { mutableStateOf("") }
    var budgetInput by remember { mutableStateOf("") }
    var quitDateTimestamp by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(activeGoal) {
        activeGoal?.let {
            limitInput = it.dailyLimit.toString()
            budgetInput = it.monthlyBudget?.toString() ?: ""
            quitDateTimestamp = it.targetQuitDate
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "系统设置与控制台",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Expandable Card 1: Goal Setting Card
        ExpandableSettingsCard(
            title = "控烟与预算目标 (Quit Goals)",
            icon = Icons.Rounded.Flag,
            initialExpanded = true
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("每日吸烟限制量 (支/天)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("每月烟草预算金额 (元, 可选)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                val dateLabel = if (quitDateTimestamp != null) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    "完全戒烟目标日: " + sdf.format(Date(quitDateTimestamp!!))
                } else {
                    "设置完全戒烟目标日 (可选)"
                }

                OutlinedButton(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selected = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                }
                                quitDateTimestamp = selected.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateLabel)
                }

                Button(
                    onClick = {
                        val limit = limitInput.toIntOrNull() ?: 10
                        val budget = budgetInput.toDoubleOrNull()
                        viewModel.updateGoal(limit, quitDateTimestamp, budget)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("保存目标与计划", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Expandable Card 2: Demo Data Toggle
        ExpandableSettingsCard(
            title = "Demo 演示体验数据模式",
            icon = Icons.Rounded.BugReport,
            initialExpanded = false
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("开启 Demo 模拟体验数据", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("开启后将自动注入近60天包含自抽、社交递烟与接烟的模拟记录，关闭后自动恢复干净真实数据。", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isDemoMode,
                        onCheckedChange = { viewModel.toggleDemoMode(it) }
                    )
                }
            }
        }

        // Expandable Card 3: Cloud Backup
        ExpandableSettingsCard(
            title = "云备份 & 同步 (Cloud Backup)",
            icon = Icons.Rounded.CloudSync,
            initialExpanded = false
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "一键将本地的吸烟历史、烟盒种类、戒烟目标同步上传至云端服务器，更换手机设备时可随时拉取还原。",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.backupData() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("备份到云端", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.restoreData() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("从云端恢复", fontSize = 13.sp)
                    }
                }
            }
        }

        // Expandable Card 4: About Section
        ExpandableSettingsCard(
            title = "关于软件 (About)",
            icon = Icons.Rounded.Info,
            initialExpanded = false
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("吸烟跟踪Guard · 科学控烟助手", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("版本号: v1.2.0 (Build 2026.07)", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "本软件专为理性控烟与社交社交场景打造，独创‘自购自抽’、‘社交递烟’与‘社交接烟’三维分类模型，精确计算戒烟健康与开销投入。",
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ExpandableSettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    initialExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initialExpanded) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    content()
                }
            }
        }
    }
}

// Dialog: Add Log Dialog with 3 scenes selector
@Composable
fun AddLogDialog(
    cigarettes: List<Cigarette>,
    onDismiss: () -> Unit,
    onConfirm: (cigaretteId: Int, quantity: Int, logType: String, note: String) -> Unit
) {
    var selectedCigarette by remember { mutableStateOf(cigarettes.firstOrNull { it.isActive } ?: cigarettes.firstOrNull()) }
    var quantity by remember { mutableStateOf("1") }
    var selectedLogType by remember { mutableStateOf("SELF") } // "SELF", "SHARED_OUT", "RECEIVED_IN"
    var note by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "记录一次吸烟打卡",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (cigarettes.isEmpty()) {
                    Text(
                        "提示：请先去【烟盒】栏添加常用的香烟种类与价格。",
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("知道了")
                    }
                } else {
                    // Cigarette Dropdown
                    Text("选择香烟品牌:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedCigarette?.name ?: "选择香烟种类")
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            cigarettes.forEach { cig ->
                                DropdownMenuItem(
                                    text = { Text(cig.name) },
                                    onClick = {
                                        selectedCigarette = cig
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Scene Selection Tabs (Requirement 1)
                    Text("选择打卡场景:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedLogType == "SELF",
                                onClick = { selectedLogType = "SELF" },
                                label = { Text("🚬 自购自抽", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedLogType == "SHARED_OUT",
                                onClick = { selectedLogType = "SHARED_OUT" },
                                label = { Text("🤝 社交递烟", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedLogType == "RECEIVED_IN",
                                onClick = { selectedLogType = "RECEIVED_IN" },
                                label = { Text("🎁 社交接烟", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val explanationText = when (selectedLogType) {
                            "SELF" -> "自己购买的香烟自己抽：计入个人健康抽烟量，并计入财务开销。"
                            "SHARED_OUT" -> "掏自己的烟递给朋友/同事：不计入个人健康超标量，但计入开销。"
                            "RECEIVED_IN" -> "接别人递过来的烟抽（他人买单）：计入个人健康抽烟量，但不花自己钱（免费）。"
                            else -> ""
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = explanationText,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Quantity Input
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("记录数量 (支)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Note input
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("备注标签 (例如: 饭后, 社交聚会等)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val cigId = selectedCigarette?.id ?: 1
                                val qty = quantity.toIntOrNull() ?: 1
                                onConfirm(cigId, qty, selectedLogType, note)
                            }
                        ) {
                            Text("确认打卡", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Dialog: Add Cigarette Dialog with Single Pack vs Single Carton options
@Composable
fun AddCigaretteDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, packSize: Int, priceType: String, cartonPrice: Double, packsPerCarton: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var priceType by remember { mutableStateOf("PACK") } // "PACK" or "CARTON"
    var priceInput by remember { mutableStateOf("") }
    var packSizeInput by remember { mutableStateOf("20") }
    var packsPerCartonInput by remember { mutableStateOf("10") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "添加新烟草品牌与规则",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("香烟名称/品牌 (如: 中华, 炫赫门)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("选择计价类型规则:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = priceType == "PACK",
                        onClick = { priceType = "PACK" },
                        label = { Text("📦 单包零售价规则", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = priceType == "CARTON",
                        onClick = { priceType = "CARTON" },
                        label = { Text("🧱 单条零售价规则", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (priceType == "PACK") {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("单包零售价格 (元)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text("单条(整条10包)零售价格 (元)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = packSizeInput,
                        onValueChange = { packSizeInput = it },
                        label = { Text("每包支数 (默认20)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = packsPerCartonInput,
                        onValueChange = { packsPerCartonInput = it },
                        label = { Text("每条包数 (默认10)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Dynamic Live Conversion Preview
                val rawVal = priceInput.toDoubleOrNull() ?: 0.0
                val pSize = packSizeInput.toIntOrNull() ?: 20
                val ppCarton = packsPerCartonInput.toIntOrNull() ?: 10

                val packPrice = if (priceType == "CARTON") rawVal / ppCarton.coerceAtLeast(1) else rawVal
                val cartonPrice = if (priceType == "PACK") rawVal * ppCarton else rawVal
                val unitPrice = packPrice / pSize.coerceAtLeast(1)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "💡 价格自动折算预览:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "折合单包: ¥${String.format(Locale.getDefault(), "%.2f", packPrice)} | 折合单条: ¥${String.format(Locale.getDefault(), "%.2f", cartonPrice)} | 单支成本: ¥${String.format(Locale.getDefault(), "%.2f", unitPrice)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && rawVal > 0) {
                                onConfirm(name, packPrice, pSize, priceType, cartonPrice, ppCarton)
                            }
                        }
                    ) {
                        Text("保存烟草", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
