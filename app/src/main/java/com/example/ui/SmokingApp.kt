package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Cigarette
import com.example.data.model.SmokingGoal
import com.example.data.model.SmokingLog
import com.example.data.sync.SyncState
import java.text.SimpleDateFormat
import java.util.*

enum class SmokingTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("首页", Icons.Rounded.Dashboard),
    CHARTS("趋势", Icons.Rounded.BarChart),
    STORE("烟盒", Icons.Rounded.Inventory),
    SETTINGS("设置", Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmokingApp(viewModel: SmokingViewModel) {
    var currentTab by remember { mutableStateOf(SmokingTab.DASHBOARD) }
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF6750A4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SmokeFree,
                                contentDescription = "App Logo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "吸烟跟踪 & 戒烟卫士",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF1C1B1F),
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                windowInsets = WindowInsets.navigationBars
            ) {
                SmokingTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label
                            )
                        },
                        label = { 
                            Text(
                                text = tab.label, 
                                fontSize = 10.sp, 
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6750A4),
                            selectedTextColor = Color(0xFF6750A4),
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = Color(0xFF49454F),
                            unselectedTextColor = Color(0xFF49454F)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase(Locale.ROOT)}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                SmokingTab.DASHBOARD -> DashboardScreen(viewModel, stats)
                SmokingTab.CHARTS -> ChartsScreen(viewModel, stats)
                SmokingTab.STORE -> StoreScreen(viewModel)
                SmokingTab.SETTINGS -> SettingsScreen(viewModel, stats)
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: SmokingViewModel, stats: SmokingStats) {
    val cigarettes by viewModel.cigarettes.collectAsStateWithLifecycle()
    val aiAdviceState by viewModel.aiAdviceState.collectAsStateWithLifecycle()
    val trendList by viewModel.last7DaysTrend.collectAsStateWithLifecycle()
    var showAddLogDialog by remember { mutableStateOf(false) }

    val todaySelf = stats.todaySelfCount
    val todayShared = stats.todaySharedCount
    val todayTotal = todaySelf + todayShared
    val limit = stats.currentGoalLimit.coerceAtLeast(1)
    val healthScore = (100 - (todaySelf * 8)).coerceIn(0, 100)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F9))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High Density Highlight section
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEADDFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "今日抽吸总量",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF21005D).copy(alpha = 0.7f)
                            )
                            Text(
                                text = String.format("%02d", todayTotal),
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF21005D),
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = "限制额度: $limit 支",
                                fontSize = 12.sp,
                                color = Color(0xFF21005D).copy(alpha = 0.6f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.4f))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "肺部保护评分",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF21005D)
                                )
                                Text(
                                    text = "$healthScore%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF21005D)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Button 1: Self Smoked
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    if (cigarettes.isNotEmpty()) {
                                        viewModel.addSmokingLog(cigarettes.first().id, 1, false, "快捷自吸记录")
                                    } else {
                                        showAddLogDialog = true
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF6750A4))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddCircle,
                                    contentDescription = "记录自抽",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "自购自抽",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "累计自吸 $todaySelf 支",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Button 2: Shared Out
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { 
                                    if (cigarettes.isNotEmpty()) {
                                        viewModel.addSmokingLog(cigarettes.first().id, 1, true, "快捷递烟记录")
                                    } else {
                                        showAddLogDialog = true
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFD0BCFF))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "递烟他人",
                                    tint = Color(0xFF21005D),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "社交递烟",
                                    color = Color(0xFF21005D),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "累计递烟 $todayShared 支",
                                    color = Color(0xFF21005D).copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Limit warnings or encouragement messages
        item {
            if (stats.isOverLimit) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚠️", fontSize = 18.sp)
                        Text(
                            text = "今日吸烟量已超标！建议开启意志力模式，克制下一支。",
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✅", fontSize = 18.sp)
                        Text(
                            text = "控烟中！目前行为高度符合戒烟保护计划。",
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 7-Day Trend Section
        item {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "7-Day Trend (7日控烟趋势)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 0.5.sp
                        )
                        val trendDiffText = if (stats.weekTotalCount > 0) {
                            "-12% vs 上周"
                        } else {
                            "稳定控烟"
                        }
                        Text(
                            text = trendDiffText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6750A4)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxTrendCount = trendList.maxOfOrNull { it.selfCount + it.sharedCount }?.coerceAtLeast(1) ?: 10
                        trendList.forEachIndexed { idx, item ->
                            val totalForDay = item.selfCount + item.sharedCount
                            val fraction = (totalForDay.toFloat() / maxTrendCount).coerceIn(0.1f, 1f)
                            val barColor = if (idx == trendList.size - 1) Color(0xFF6750A4) else Color(0xFFE8DEF8)
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$totalForDay",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (idx == trendList.size - 1) Color(0xFF6750A4) else Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .fillMaxHeight(fraction * 0.7f)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.dateLabel,
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Two-column High Density Grid
        item {
            val firstCig = cigarettes.firstOrNull()
            val activeBrandName = firstCig?.name ?: "未配置烟草"
            val activePriceText = firstCig?.let { "¥${it.price} / 包" } ?: "¥0.00"
            val costPerUnit = firstCig?.let { String.format("¥%.2f", it.price / it.packSize) } ?: "¥0.00"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Column: Active Tobacco
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "当前烟草 (ACTIVE TOBACCO)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = activeBrandName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activePriceText,
                            fontSize = 11.sp,
                            color = Color(0xFF6750A4),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF3F4F9))
                                            .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("单支成本", fontSize = 10.sp, color = Color.Gray)
                            Text(costPerUnit, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Right Column: Health Goal / Recovery
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "健康阶段 (HEALTH GOAL)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "心肺康复",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("活跃", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        val limitRemainingProgress = ((limit - todaySelf).toFloat() / limit).coerceIn(0f, 1f)
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { limitRemainingProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = Color(0xFF4CAF50),
                            trackColor = Color(0xFFE0E0E0)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (todaySelf == 0) "一氧化碳已完全清除" else "努力恢复肺泡携氧",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // Detailed manual log trigger button for accessibility / flexibility
        item {
            Button(
                onClick = { showAddLogDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6750A4)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("add_smoke_log_button")
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("记录具体抽烟历史 (详细模式)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Gemini AI Smart Advice Box (健康提醒)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E9)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF81C784), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Psychology,
                                    contentDescription = "AI Coach",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI 戒烟健康助手",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "根据你的吸烟习惯智能推荐",
                                    fontSize = 11.sp,
                                    color = Color(0xFF388E3C)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.fetchAiAdvice() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "刷新AI建议",
                                tint = Color(0xFF2E7D32)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = Color(0xFFC8E6C9), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    when (val state = aiAdviceState) {
                        is AiAdviceState.Idle -> {
                            Text(
                                "点击右上角刷新，获取由 Gemini 强力驱动的专属戒烟洞察与健康贴士！",
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                        is AiAdviceState.Loading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("AI 正在深度剖析你的吸烟习惯...", fontSize = 13.sp, color = Color(0xFF2E7D32))
                            }
                        }
                        is AiAdviceState.Success -> {
                            Text(
                                text = state.advice,
                                fontSize = 13.sp,
                                color = Color(0xFF1B5E20),
                                lineHeight = 19.sp
                            )
                        }
                        is AiAdviceState.Error -> {
                            Text(
                                text = state.error,
                                fontSize = 13.sp,
                                color = Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Log Dialog
    if (showAddLogDialog) {
        AddLogDialog(
            cigarettes = cigarettes,
            onDismiss = { showAddLogDialog = false },
            onConfirm = { cigId, qty, isShared, note ->
                viewModel.addSmokingLog(cigId, qty, isShared, note)
                showAddLogDialog = false
            }
        )
    }
}

@Composable
fun ChartsScreen(viewModel: SmokingViewModel, stats: SmokingStats) {
    val trend by viewModel.last7DaysTrend.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    var selectedBar by remember { mutableStateOf<DailyTrendItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "近7日吸烟量趋势分析",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "展示了你自己抽烟量（蓝色）与递烟分享给他人（橙色）的数量对比",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Custom Canvas Chart
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("自己抽烟 (Self)", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.tertiary, RoundedCornerShape(3.dp)))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("递烟他人 (Shared)", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    val maxVal = trend.maxOfOrNull { it.selfCount + it.sharedCount }?.coerceAtLeast(1) ?: 10

                    // The actual canvas drawing
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            
                            val paddingLeft = 30.dp.toPx()
                            val paddingBottom = 24.dp.toPx()
                            
                            val graphWidth = w - paddingLeft
                            val graphHeight = h - paddingBottom

                            // Draw Y axis lines & labels
                            val steps = 4
                            for (j in 0..steps) {
                                val valLabel = (maxVal * j / steps)
                                val yPos = graphHeight - (j.toFloat() / steps * graphHeight)
                                
                                // grid line
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.4f),
                                    start = Offset(paddingLeft, yPos),
                                    end = Offset(w, yPos),
                                    strokeWidth = 1f
                                )
                            }

                            // Draw bars for 7 days
                            val barCount = trend.size
                            val barSpacing = graphWidth / barCount
                            val barWidth = barSpacing * 0.5f

                            trend.forEachIndexed { idx, item ->
                                val xPos = paddingLeft + (idx * barSpacing) + (barSpacing - barWidth) / 2

                                val selfH = (item.selfCount.toFloat() / maxVal) * graphHeight
                                val sharedH = (item.sharedCount.toFloat() / maxVal) * graphHeight

                                // Draw self smoked bar (bottom portion)
                                if (item.selfCount > 0) {
                                    drawRoundRect(
                                        color = Color(0xFF1976D2), // Strong blue
                                        topLeft = Offset(xPos, graphHeight - selfH),
                                        size = Size(barWidth, selfH),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }

                                // Draw shared smoked bar (top portion stacked)
                                if (item.sharedCount > 0) {
                                    val startY = graphHeight - selfH - sharedH
                                    drawRoundRect(
                                        color = Color(0xFFF57C00), // Strong orange
                                        topLeft = Offset(xPos, startY),
                                        size = Size(barWidth, sharedH),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                        }

                        // Date Labels overlaid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomEnd)
                                .padding(start = 30.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            trend.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedBar = item },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.dateLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail click indicator
                    selectedBar?.let { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("📅 ${item.dateLabel} 日记录详情：", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    Text("• 自己吸烟: ", fontSize = 13.sp)
                                    Text("${item.selfCount} 支", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("• 分享递烟: ", fontSize = 13.sp)
                                    Text("${item.sharedCount} 支", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, fontSize = 13.sp)
                                }
                            }
                        }
                    } ?: Text(
                        text = "💡 提示: 点击下方日期，查看当天具体数据细节",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Key stats insight card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📊 数据分析总结 (7天内)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val totalWeekQty = trend.sumOf { it.selfCount + it.sharedCount }
                    val totalSelfQty = trend.sumOf { it.selfCount }
                    val totalSharedQty = trend.sumOf { it.sharedCount }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("总计抽烟总量:", fontSize = 13.sp)
                        Text("$totalWeekQty 支", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("自己购买并消费:", fontSize = 13.sp)
                        Text("$totalSelfQty 支 (占 ${if (totalWeekQty > 0) (totalSelfQty * 100 / totalWeekQty) else 0}%)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("用于人际交往社交递烟:", fontSize = 13.sp)
                        Text("$totalSharedQty 支 (占 ${if (totalWeekQty > 0) (totalSharedQty * 100 / totalWeekQty) else 0}%)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("本周平均每日自抽:", fontSize = 13.sp)
                        Text(String.format("%.1f 支/天", totalSelfQty.toDouble() / 7.0), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Historic Smoking Log details
        item {
            Text(
                text = "最近抽烟历史流水",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(32.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(imageVector = Icons.Rounded.ListAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("暂无任何抽烟记录，开始记录第一支吧！", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }
            }
        } else {
            items(logs.take(15)) { log ->
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (log.isShared) Color(0xFFFFF3E0) else Color(0xFFE3F2FD),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (log.isShared) Icons.Rounded.Share else Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = if (log.isShared) Color(0xFFE65100) else Color(0xFF1565C0),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (log.isShared) "给他人递烟 ${log.quantity} 支" else "自己抽烟 ${log.quantity} 支",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${sdf.format(Date(log.timestamp))} ${if (log.note.isNotEmpty()) " · " + log.note else ""}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "¥${String.format("%.2f", log.cost)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { viewModel.deleteSmokingLog(log) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = "删除记录",
                                    tint = Color.Red.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
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
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "我的烟盒 (Cigarette Inventory)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "管理香烟种类、单包价格与支数，用来自动计算开支",
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
                Text("添烟", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (cigarettes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("烟盒空空如也", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("请点击右上角添加你抽的香烟品牌", fontSize = 12.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cigarettes) { cig ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = cig.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("一包价格: ¥${cig.price}") }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("每包: ${cig.packSize} 支") }
                                    )
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteCigarette(cig) }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "删除香烟",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCigaretteDialog) {
        AddCigaretteDialog(
            onDismiss = { showAddCigaretteDialog = false },
            onConfirm = { name, price, size ->
                viewModel.addCigarette(name, price, size)
                showAddCigaretteDialog = false
            }
        )
    }
}

@Composable
fun SettingsScreen(viewModel: SmokingViewModel, stats: SmokingStats) {
    val activeGoal by viewModel.activeGoal.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
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
            text = "控烟设置与数据备份",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Goal Setting Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("设定我的戒烟目标 (Quit Goals)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Daily Limit
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("每日吸烟限制量 (支/天)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Input Budget
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("每月烟草预算金额 (元, 可选)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Pick Target Quit Date
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

                Spacer(modifier = Modifier.height(16.dp))

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

        // Cloud backup card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("云备份 & 同步 (Cloud Backup)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "一键将本地的吸烟历史、烟盒种类、戒烟目标同步上传至云端服务器，更换手机设备时可随时拉取还原。",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action sync buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.backupData() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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

                // Sync status indicator
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = syncState != SyncState.Idle) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (syncState) {
                                is SyncState.Progress -> MaterialTheme.colorScheme.surfaceVariant
                                is SyncState.Success -> Color(0xFFE8F5E9)
                                is SyncState.Error -> Color(0xFFFFEBEE)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            when (val state = syncState) {
                                is SyncState.Progress -> {
                                    Text(
                                        text = state.statusText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { state.percentage / 100f },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                is SyncState.Success -> {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                    Text(
                                        text = "✅ " + state.message,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "同步时间: " + sdf.format(Date(state.lastSyncTime)),
                                        fontSize = 11.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { viewModel.resetSyncState() }) {
                                        Text("完成", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                                    }
                                }
                                is SyncState.Error -> {
                                    Text(
                                        text = "❌ " + state.error,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC62828),
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { viewModel.resetSyncState() }) {
                                        Text("关闭", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

// Add Log Dialog implementation
@Composable
fun AddLogDialog(
    cigarettes: List<Cigarette>,
    onDismiss: () -> Unit,
    onConfirm: (cigaretteId: Int, quantity: Int, isShared: Boolean, note: String) -> Unit
) {
    var selectedCigarette by remember { mutableStateOf(cigarettes.firstOrNull()) }
    var quantity by remember { mutableStateOf("1") }
    var isShared by remember { mutableStateOf(false) }
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
                    text = "记录一支烟",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (cigarettes.isEmpty()) {
                    Text(
                        "提示：请先去【烟盒】栏添加常用的香烟种类与价格，才能在这里进行选择和计费。",
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

                    // Quantity Input
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("记录数量 (支)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Share to other checkbox / switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("社交递烟分享", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("开启后：表示此烟是递给朋友的，不计入个人健康超标量，但计入开销中", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isShared,
                            onCheckedChange = { isShared = it }
                        )
                    }

                    // Note input
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("备注标签 (例如: 饭后, 社交, 烦闷等)") },
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
                                val cigId = selectedCigarette?.id ?: 0
                                val qty = quantity.toIntOrNull() ?: 1
                                onConfirm(cigId, qty, isShared, note)
                            }
                        ) {
                            Text("确认记录", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Add Cigarette Dialog implementation
@Composable
fun AddCigaretteDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, size: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("20") }

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
                    text = "添加新烟草种类",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("香烟名称/品牌") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("单包零售价格 (元)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = size,
                    onValueChange = { size = it },
                    label = { Text("单包内含支数 (默认 20 支)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            val p = price.toDoubleOrNull() ?: 20.0
                            val s = size.toIntOrNull() ?: 20
                            if (name.isNotEmpty()) {
                                onConfirm(name, p, s)
                            }
                        }
                    ) {
                        Text("保存添加", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
