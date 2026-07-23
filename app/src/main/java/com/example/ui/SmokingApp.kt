package com.example.ui

import android.app.DatePickerDialog
import kotlinx.coroutines.isActive
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.i18n.AppColorPreset
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppStrings
import com.example.ui.i18n.AppThemeMode
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmokingApp(viewModel: SmokingViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()

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
                                0 -> AppStrings.get("title_home", lang)
                                1 -> AppStrings.get("title_trends", lang)
                                2 -> AppStrings.get("title_store", lang)
                                else -> AppStrings.get("title_settings", lang)
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
                    icon = { Icon(Icons.Rounded.Home, contentDescription = AppStrings.get("tab_home", lang)) },
                    label = { Text(AppStrings.get("tab_home", lang)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.BarChart, contentDescription = AppStrings.get("tab_trends", lang)) },
                    label = { Text(AppStrings.get("tab_trends", lang)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.Inventory2, contentDescription = AppStrings.get("tab_store", lang)) },
                    label = { Text(AppStrings.get("tab_store", lang)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = AppStrings.get("tab_settings", lang)) },
                    label = { Text(AppStrings.get("tab_settings", lang)) }
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
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currency by viewModel.appCurrency.collectAsStateWithLifecycle()

    var showAddLogDialog by remember { mutableStateOf(false) }

    val activeCigarette = cigarettes.firstOrNull { it.isActive } ?: cigarettes.firstOrNull()

    // Dynamic timer calculating time elapsed since last smoking log
    val lastLog = remember(logs) { logs.maxByOrNull { it.timestamp } }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }
    val elapsedText = if (lastLog != null) {
        val diff = (nowMs - lastLog.timestamp).coerceAtLeast(0L)
        val secs = (diff / 1000) % 60
        val mins = (diff / (1000 * 60)) % 60
        val hours = diff / (1000 * 3600)
        if (lang == AppLanguage.EN) {
            String.format("%02dh %02dm %02ds", hours, mins, secs)
        } else {
            String.format("%02d小时%02d分%02d秒", hours, mins, secs)
        }
    } else {
        AppStrings.get("timer_no_logs", lang)
    }

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
                        text = AppStrings.get("over_limit_warning", lang, stats.todayTotalCount, stats.currentGoalLimit),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = AppStrings.get("active_brand_label", lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeCigarette?.name ?: AppStrings.get("unset_default_brand", lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "${currency.symbol}${String.format(Locale.getDefault(), "%.2f", activeCigarette?.price ?: 25.0)} ${AppStrings.get("pack_unit", lang)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        // Today Stats Grid (Merged 2/3 and 1/3 layout)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val stickUnit = AppStrings.get("stick_unit", lang).trim()
            val selfText = if (lang == AppLanguage.EN) "Self" else "自抽"
            val recText = if (lang == AppLanguage.EN) "Received" else "接烟"
            val targetText = if (lang == AppLanguage.EN) "Goal" else "目标"
            val overText = if (lang == AppLanguage.EN) "Over Limit" else "已超标"
            val onTrackText = if (lang == AppLanguage.EN) "On Track" else "符合目标"

            // Merged Card 1 (Self + Received) occupying 2/3 of content width
            StatCard(
                title = if (lang == AppLanguage.EN) "Today Actual Smoking (Self + Received)" else "今日实际吸烟 (自抽+接烟)",
                value = "${stats.todaySelfCount + stats.todayReceivedCount} $stickUnit",
                detailText = "$selfText ${stats.todaySelfCount}$stickUnit · $recText ${stats.todayReceivedCount}$stickUnit",
                subtitle = "$targetText ${stats.currentGoalLimit} $stickUnit/day (${if (stats.isOverLimit) overText else onTrackText})",
                icon = Icons.Rounded.SmokingRooms,
                iconTint = if (stats.isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(2f)
            )

            // Card 2 (Shared Out) occupying 1/3 of content width
            StatCard(
                title = AppStrings.get("stat_today_shared", lang),
                value = "${stats.todaySharedCount} $stickUnit",
                detailText = AppStrings.get("shared_subtitle", lang),
                subtitle = if (lang == AppLanguage.EN) "Shared Cost" else "递烟开销",
                icon = Icons.Rounded.CallMade,
                iconTint = MaterialTheme.colorScheme.secondary,
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
                title = AppStrings.get("stat_today_cost", lang),
                value = "${currency.symbol}${String.format(Locale.getDefault(), "%.2f", stats.todayCost)}",
                subtitle = AppStrings.get("cost_subtitle", lang),
                icon = Icons.Rounded.Payments,
                iconTint = MaterialTheme.colorScheme.primary,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = AppStrings.get("stat_today_saved", lang),
                value = "${currency.symbol}${String.format(Locale.getDefault(), "%.2f", stats.todaySavedFromReceived)}",
                subtitle = AppStrings.get("saved_subtitle", lang),
                icon = Icons.Rounded.CardGiftcard,
                iconTint = Color(0xFF2E7D32),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Action Buttons
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = AppStrings.get("quick_record_title", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        // color = MaterialTheme.colorScheme.primary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                // tint = MaterialTheme.colorScheme.onPrimary,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lang == AppLanguage.EN) "Since last: $elapsedText" else "距上次 $elapsedText",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                // color = MaterialTheme.colorScheme.onPrimary
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick 1: Self
                    Button(
                        onClick = {
                            val cigId = activeCigarette?.id ?: 1
                            viewModel.addSmokingLog(cigId, 1, "SELF", if (lang == AppLanguage.EN) "Quick Log" else "极速记录")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.SmokingRooms, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("quick_self_btn", lang), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // Quick 2: Shared Out
                    Button(
                        onClick = {
                            val cigId = activeCigarette?.id ?: 1
                            viewModel.addSmokingLog(cigId, 1, "SHARED_OUT", if (lang == AppLanguage.EN) "Shared Out" else "社交递烟")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.CallMade, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("quick_shared_btn", lang), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // Quick 3: Received In
                    Button(
                        onClick = {
                            val cigId = activeCigarette?.id ?: 1
                            viewModel.addSmokingLog(cigId, 1, "RECEIVED_IN", if (lang == AppLanguage.EN) "Received In" else "他人递烟")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.CallReceived, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("quick_received_btn", lang), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showAddLogDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(AppStrings.get("custom_record_btn", lang), fontSize = 13.sp)
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
                            text = AppStrings.get("ai_advice_title", lang),
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
                            contentDescription = if (lang == AppLanguage.EN) "Refresh AI Advice" else "刷新 AI 建议",
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
                            Text(AppStrings.get("ai_advice_loading", lang), fontSize = 13.sp)
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
                text = AppStrings.get("today_logs_header", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = AppStrings.get("log_count", lang, logs.size),
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
                    text = AppStrings.get("no_logs_today", lang),
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
                        "SHARED_OUT" -> AppStrings.get("type_shared", lang)
                        "RECEIVED_IN" -> AppStrings.get("type_received", lang)
                        else -> AppStrings.get("type_self", lang)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = logTypeName,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = badgeColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    val defaultCigName = if (lang == AppLanguage.EN) "Cigarette" else "香烟"
                                    val stickLabel = AppStrings.get("stick_unit", lang).trim()
                                    Text(
                                        text = "${cig?.name ?: defaultCigName} x${log.quantity}$stickLabel",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (log.note.isNotEmpty()) {
                                        Text(
                                            text = AppStrings.get("note_prefix", lang, log.note),
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = sdf.format(Date(log.timestamp)),
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = if (log.logType == "RECEIVED_IN") AppStrings.get("free_badge", lang) else "${currency.symbol}${String.format(Locale.getDefault(), "%.2f", log.cost)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (log.logType == "RECEIVED_IN") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                        maxLines = 1
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteSmokingLog(log) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = if (lang == AppLanguage.EN) "Delete Log" else "删除记录",
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
            lang = lang,
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
    color: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    detailText: String? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (icon != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (detailText != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = detailText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val goal by viewModel.activeGoal.collectAsStateWithLifecycle()
    val currency by viewModel.appCurrency.collectAsStateWithLifecycle()
    val maxIntervalHours by viewModel.maxIntervalThresholdHours.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var selectedChartIndex by remember { mutableIntStateOf(-1) }

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
            text = AppStrings.get("time_range_title", lang),
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
                    onClick = {
                        viewModel.selectedTimeRange.value = range
                        selectedChartIndex = -1
                    },
                    label = { Text(range.getLabel(lang), fontSize = 12.sp) },
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
                            SimpleDateFormat(if (lang == AppLanguage.EN) "MM/yyyy" else "yyyy年MM月", Locale.getDefault())
                        else
                            SimpleDateFormat(if (lang == AppLanguage.EN) "yyyy" else "yyyy年", Locale.getDefault())

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
                                    text = "${if (lang == AppLanguage.EN) "Target Date: " else "目标日期: "}${sdf.format(selectedCal.time)}",
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
                                Text(
                                    if (lang == AppLanguage.EN)
                                        "Select ${if (selectedTimeRange == TrendTimeRange.SPECIFIC_MONTH) "Month" else "Year"}"
                                    else
                                        "点击选择${if (selectedTimeRange == TrendTimeRange.SPECIFIC_MONTH) "月份" else "年份"}",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else if (selectedTimeRange == TrendTimeRange.CUSTOM_RANGE) {
                        val customStart by viewModel.customStartDate.collectAsStateWithLifecycle()
                        val customEnd by viewModel.customEndDate.collectAsStateWithLifecycle()
                        val dateSdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

                        val startText = customStart?.let { dateSdf.format(Date(it)) } ?: if (lang == AppLanguage.EN) "Select Start Date" else "选择开始日期"
                        val endText = customEnd?.let { dateSdf.format(Date(it)) } ?: if (lang == AppLanguage.EN) "Select End Date" else "选择结束日期"

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (lang == AppLanguage.EN) "Custom Date Range:" else "自定义时间范围:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

                            Text(if (lang == AppLanguage.EN) "to" else "至", fontSize = 12.sp, color = Color.Gray)

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

        // ================= SECTION 1: 自我吸烟量与行为趋势分析 (Self-Smoking Trend Analysis) =================
        val totalSelfCount = trendData.sumOf { it.selfCount }
        val periodDays = trendData.size.coerceAtLeast(1)
        val dailyAvgSelf = totalSelfCount.toFloat() / periodDays

        val validIntervals = trendData.filter { it.avgIntervalMinutes > 0 }.map { it.avgIntervalMinutes }
        val overallAvgInterval = if (validIntervals.isNotEmpty()) validIntervals.average().toInt() else 0

        val peakSlots = trendData.filter { it.peakHourSlot != "无打卡" && it.peakHourSlot != "No logs" }.groupBy { it.peakHourSlot }
        val overallPeakSlot = peakSlots.maxByOrNull { it.value.size }?.key ?: "14:00 - 16:00"

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = AppStrings.get("self_trend_section_title", lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (lang == AppLanguage.EN) "Focus on self-smoked, interval & peak hours" else "聚焦自抽量、打卡间隔与高发吸烟时段",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // 4 Stat Summary Cards Grid (文字卡片)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = AppStrings.get("stat_daily_avg_self", lang),
                        value = String.format(Locale.getDefault(), if (lang == AppLanguage.EN) "%.1f sticks" else "%.1f 支", dailyAvgSelf),
                        subtitle = "${AppStrings.get("stat_total_period_self", lang)}: ${totalSelfCount}${if (lang == AppLanguage.EN) " sticks" else "支"}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = AppStrings.get("stat_avg_interval", lang),
                        value = if (overallAvgInterval > 0) "${overallAvgInterval}${if (lang == AppLanguage.EN) " mins" else "分钟"}" else AppStrings.get("no_interval_data", lang),
                        subtitle = if (lang == AppLanguage.EN) "Excl. >${maxIntervalHours}h gaps" else "已排除>${maxIntervalHours}小时间隔",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = AppStrings.get("stat_peak_hours", lang),
                        value = overallPeakSlot,
                        subtitle = if (lang == AppLanguage.EN) "Peak smoking window" else "吸烟最密集窗口",
                        modifier = Modifier.weight(1f)
                    )
                    val dailyLimit = goal?.dailyLimit ?: 15
                    val statusText = if (dailyAvgSelf <= dailyLimit) {
                        if (lang == AppLanguage.EN) "🟢 On Target" else "🟢 控烟达标"
                    } else {
                        if (lang == AppLanguage.EN) "🟠 Warning" else "🟠 超标预警"
                    }
                    StatCard(
                        title = AppStrings.get("insight_title", lang),
                        value = statusText,
                        subtitle = if (lang == AppLanguage.EN) "Limit: ${dailyLimit} sticks/day" else "限制目标: ${dailyLimit}支/天",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Behavioral Insight Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (dailyAvgSelf > (goal?.dailyLimit ?: 15)) {
                                if (lang == AppLanguage.EN) "Recent self-smoked volume is high, peaking around [${overallPeakSlot}]. Consider sugar-free gum or deep breathing in this window to extend stick intervals!" else "近期自抽量偏高，主要集中在 [${overallPeakSlot}]。建议在该时段常备无糖口香糖或深呼吸分散注意力，并尝试拉长单支间隔！"
                            } else {
                                if (lang == AppLanguage.EN) "Your self-smoking habit is currently well-controlled! Average interval is ${if (overallAvgInterval > 0) "${overallAvgInterval} mins" else "steady"}. Keep up the steady reduction progress." else "当前自抽习惯控制良好！平均间隔为 ${if (overallAvgInterval > 0) "${overallAvgInterval}分钟" else "规律良好"}，请保持有节奏的减量进度。"
                            },
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Interactive Chart Card (图表卡片 + 交互)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${selectedTimeRange.getLabel(lang)} - ${selectedChartType.getLabel(lang)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    // Chart type options on their own row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ChartType.values().forEach { type ->
                            FilterChip(
                                selected = selectedChartType == type,
                                onClick = { viewModel.selectedChartType.value = type },
                                label = { Text(type.getLabel(lang), fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Dynamic Banner / Annotation Pill
                val bannerText = if (selectedChartIndex in trendData.indices) {
                    val sel = trendData[selectedChartIndex]
                    if (selectedChartType == ChartType.LINE) {
                        if (lang == AppLanguage.EN)
                            "[${sel.dateLabel}] Total Smoked: ${sel.selfCount + sel.receivedCount} sticks · Received: ${sel.receivedCount} sticks (Self: ${sel.selfCount})"
                        else
                            "【${sel.dateLabel}】自购自抽+社交接烟: ${sel.selfCount + sel.receivedCount}支 · 社交接烟: ${sel.receivedCount}支 (自抽:${sel.selfCount}支)"
                    } else {
                        if (lang == AppLanguage.EN)
                            "[${sel.dateLabel}] Smoked: ${sel.selfCount + sel.receivedCount} sticks (Self: ${sel.selfCount}, Rec: ${sel.receivedCount}) · Shared: ${sel.sharedCount} sticks · Spent: ${currency.symbol}${String.format(Locale.getDefault(), "%.2f", sel.selfCost + sel.sharedCost)}"
                        else
                            "【${sel.dateLabel}】实际吸烟:${sel.selfCount + sel.receivedCount}支 (自抽:${sel.selfCount} 接:${sel.receivedCount}) · 递烟:${sel.sharedCount}支 · 花费:${currency.symbol}${String.format(Locale.getDefault(), "%.2f", sel.selfCost + sel.sharedCost)}"
                    }
                } else {
                    if (selectedChartType == ChartType.LINE) {
                        if (lang == AppLanguage.EN)
                            "Tap line node to compare Total vs Received details"
                        else
                            "点击折线数据节点可对比【自购自抽+社交接烟】与【社交接烟】详情"
                    } else {
                        AppStrings.get("chart_click_hint", lang)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedChartIndex in trendData.indices) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bannerText,
                        fontSize = 12.sp,
                        color = if (selectedChartIndex in trendData.indices) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legend aligned with selected chart format
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedChartType == ChartType.LINE) {
                        LegendItem(color = MaterialTheme.colorScheme.primary, text = AppStrings.get("chart_legend_line_total", lang))
                        Spacer(modifier = Modifier.width(16.dp))
                        LegendItem(color = Color(0xFF2E7D32), text = AppStrings.get("chart_legend_received", lang))
                    } else {
                        LegendItem(color = MaterialTheme.colorScheme.primary, text = AppStrings.get("chart_legend_self", lang))
                        Spacer(modifier = Modifier.width(12.dp))
                        LegendItem(color = Color(0xFF2E7D32), text = AppStrings.get("chart_legend_received", lang))
                        Spacer(modifier = Modifier.width(12.dp))
                        LegendItem(color = MaterialTheme.colorScheme.secondary, text = AppStrings.get("chart_legend_shared", lang))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Chart Graphic Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    if (trendData.isEmpty()) {
                        Text(if (lang == AppLanguage.EN) "No data recorded for this period" else "暂无当前时段的数据记录", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    } else {
                        TrendChartComposable(
                            trendData = trendData,
                            chartType = selectedChartType,
                            selectedIndex = selectedChartIndex,
                            onSelectIndex = { selectedChartIndex = it },
                            lang = lang
                        )
                    }
                }

                // Interactive Click Popover Detail Card (点击数据节点交互详情)
                AnimatedVisibility(visible = selectedChartIndex in trendData.indices) {
                    if (selectedChartIndex in trendData.indices) {
                        val item = trendData[selectedChartIndex]
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format(AppStrings.get("selected_detail_title", lang), item.dateLabel),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { selectedChartIndex = -1 },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(if (lang == AppLanguage.EN) "Self: ${item.selfCount} sticks" else "自抽: ${item.selfCount} 支", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(if (lang == AppLanguage.EN) "Shared Out: ${item.sharedCount} sticks" else "递烟: ${item.sharedCount} 支", fontSize = 11.sp, color = Color.Gray)
                                        Text(if (lang == AppLanguage.EN) "Received In: ${item.receivedCount} sticks" else "接烟: ${item.receivedCount} 支", fontSize = 11.sp, color = Color.Gray)
                                        Text(if (lang == AppLanguage.EN) "Actual Smoked (Self+Rec): ${item.selfCount + item.receivedCount} sticks" else "实际吸烟(自抽+接烟): ${item.selfCount + item.receivedCount} 支", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(if (lang == AppLanguage.EN) "Avg Interval: ${if (item.avgIntervalMinutes > 0) "${item.avgIntervalMinutes} mins" else "Single log"}" else "平均间隔: ${if (item.avgIntervalMinutes > 0) "${item.avgIntervalMinutes}分钟" else "单次打卡"}", fontSize = 11.sp)
                                        Text(if (lang == AppLanguage.EN) "Peak Window: ${item.peakHourSlot}" else "高峰窗口: ${item.peakHourSlot}", fontSize = 11.sp)
                                        Text(if (lang == AppLanguage.EN) "Daily Spent: ${currency.symbol}${String.format(Locale.getDefault(), "%.2f", item.selfCost + item.sharedCost)}" else "当日开销: ${currency.symbol}${String.format(Locale.getDefault(), "%.2f", item.selfCost + item.sharedCost)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ================= SECTION 2: 吸烟开销与社交性价比分析 (Expenditure Analysis) =================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.get("financial_analysis_title", lang), fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
                        Text(AppStrings.get("self_cost", lang), fontSize = 11.sp, color = Color.Gray)
                        Text("${currency.symbol}${String.format(Locale.getDefault(), "%.2f", totalSelfCost)}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Column {
                        Text(AppStrings.get("shared_cost", lang), fontSize = 11.sp, color = Color.Gray)
                        Text("${currency.symbol}${String.format(Locale.getDefault(), "%.2f", totalSharedCost)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    Column {
                        Text(AppStrings.get("received_saved", lang), fontSize = 11.sp, color = Color.Gray)
                        Text("${currency.symbol}${String.format(Locale.getDefault(), "%.2f", totalReceivedSaved)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2E7D32))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(AppStrings.get("total_spent", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${currency.symbol}${String.format(Locale.getDefault(), "%.2f", totalSpent)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.error)
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
    chartType: ChartType,
    selectedIndex: Int = -1,
    onSelectIndex: (Int) -> Unit = {},
    lang: AppLanguage = AppLanguage.ZH
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val greenColor = Color(0xFF2E7D32)

    val maxVal = trendData.maxOfOrNull { it.selfCount + it.sharedCount + it.receivedCount }?.coerceAtLeast(5) ?: 5

    val leftMargin = 28.dp
    val bottomMargin = 28.dp

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(trendData, chartType) {
                detectTapGestures { offset ->
                    val leftPx = leftMargin.toPx()
                    val bottomPx = bottomMargin.toPx()
                    val usableW = size.width - leftPx
                    val stepX = usableW / trendData.size.coerceAtLeast(1)
                    val x = offset.x - leftPx
                    if (x >= 0 && stepX > 0) {
                        val clickedIdx = (x / stepX).toInt().coerceIn(0, trendData.size - 1)
                        onSelectIndex(if (selectedIndex == clickedIdx) -1 else clickedIdx)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val leftPx = leftMargin.toPx()
        val bottomPx = bottomMargin.toPx()
        val topPx = 16.dp.toPx()
        val usableHeight = height - bottomPx - topPx
        val usableWidth = width - leftPx
        val itemCount = trendData.size
        val stepX = usableWidth / itemCount.coerceAtLeast(1)

        // 1. Draw Y-Axis Gridlines & Scale Numbers
        val gridY0 = height - bottomPx
        val gridYMid = height - bottomPx - usableHeight / 2f
        val gridYMax = height - bottomPx - usableHeight

        drawLine(color = Color.LightGray.copy(alpha = 0.4f), start = Offset(leftPx, gridY0), end = Offset(width, gridY0), strokeWidth = 1.dp.toPx())
        drawLine(color = Color.LightGray.copy(alpha = 0.25f), start = Offset(leftPx, gridYMid), end = Offset(width, gridYMid), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        drawLine(color = Color.LightGray.copy(alpha = 0.25f), start = Offset(leftPx, gridYMax), end = Offset(width, gridYMax), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))

        val scalePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY
            textSize = 9.sp.toPx()
            isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText("0", 4f, gridY0 + 3f, scalePaint)
        drawContext.canvas.nativeCanvas.drawText("${maxVal / 2}", 4f, gridYMid + 3f, scalePaint)
        drawContext.canvas.nativeCanvas.drawText("$maxVal", 4f, gridYMax + 3f, scalePaint)

        // 2. Draw X-Axis Date Labels
        trendData.forEachIndexed { index, item ->
            val xCenter = leftPx + index * stepX + stepX / 2f
            val isSelected = index == selectedIndex
            val labelPaint = android.graphics.Paint().apply {
                color = if (isSelected) android.graphics.Color.parseColor("#1B5E20") else android.graphics.Color.GRAY
                textSize = if (isSelected) 10.sp.toPx() else 8.5.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = isSelected
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(item.dateLabel, xCenter, height - 4f, labelPaint)
        }

        // 3. Highlight Selected Crosshair Line
        if (selectedIndex in 0 until itemCount) {
            val selXCenter = leftPx + selectedIndex * stepX + stepX / 2f
            drawLine(
                color = primaryColor.copy(alpha = 0.5f),
                start = Offset(selXCenter, topPx),
                end = Offset(selXCenter, height - bottomPx),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )
        }

        // 4. Render Chart Format
        when (chartType) {
            ChartType.BAR -> {
                val groupWidth = stepX * 0.8f
                val singleBarWidth = (groupWidth / 2f - 2.dp.toPx()).coerceIn(4.dp.toPx(), 18.dp.toPx())
                val barGap = 2.dp.toPx()

                trendData.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val xCenter = leftPx + index * stepX + stepX / 2f
                    val xBar1 = xCenter - singleBarWidth - barGap / 2f // Bar 1: Self + Received stacked
                    val xBar2 = xCenter + barGap / 2f                  // Bar 2: Shared Out

                    val selfH = (item.selfCount.toFloat() / maxVal) * usableHeight
                    val recH = (item.receivedCount.toFloat() / maxVal) * usableHeight
                    val sharedH = (item.sharedCount.toFloat() / maxVal) * usableHeight

                    val baseY = height - bottomPx

                    // Draw Bar 1: Stacked Self (Primary) + Received In (Green)
                    var currentY = baseY
                    if (selfH > 0) {
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(xBar1, currentY - selfH),
                            size = Size(singleBarWidth, selfH)
                        )
                        currentY -= selfH
                    }
                    if (recH > 0) {
                        drawRect(
                            color = greenColor,
                            topLeft = Offset(xBar1, currentY - recH),
                            size = Size(singleBarWidth, recH)
                        )
                    }

                    // Draw Bar 2: Shared Out (Secondary)
                    if (sharedH > 0) {
                        drawRect(
                            color = secondaryColor,
                            topLeft = Offset(xBar2, baseY - sharedH),
                            size = Size(singleBarWidth, sharedH)
                        )
                    }

                    if (isSelected) {
                        val totalH1 = selfH + recH
                        val maxH = maxOf(totalH1, sharedH)
                        val barTopY = baseY - maxH
                        val totalWidth = singleBarWidth * 2 + barGap + 4.dp.toPx()
                        drawRect(
                            color = primaryColor,
                            topLeft = Offset(xBar1 - 2.dp.toPx(), barTopY - 2.dp.toPx()),
                            size = Size(totalWidth, maxH.coerceAtLeast(4.dp.toPx()) + 4.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }
            }
            ChartType.LINE -> {
                val pathCombined = Path() // Line 1: Self + Received In (自购自抽 + 社交接烟)
                val pathRec = Path()      // Line 2: Received In (社交接烟)

                trendData.forEachIndexed { index, item ->
                    val x = leftPx + index * stepX + stepX / 2f
                    val combinedCount = item.selfCount + item.receivedCount
                    val yCombined = height - bottomPx - (combinedCount.toFloat() / maxVal) * usableHeight
                    val yRec = height - bottomPx - (item.receivedCount.toFloat() / maxVal) * usableHeight

                    if (index == 0) {
                        pathCombined.moveTo(x, yCombined)
                        pathRec.moveTo(x, yRec)
                    } else {
                        pathCombined.lineTo(x, yCombined)
                        pathRec.lineTo(x, yRec)
                    }

                    val isSelected = index == selectedIndex
                    val radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx()

                    drawCircle(color = primaryColor, radius = radius, center = Offset(x, yCombined))
                    drawCircle(color = greenColor, radius = radius, center = Offset(x, yRec))

                    if (isSelected) {
                        drawCircle(color = primaryColor, radius = 9.dp.toPx(), center = Offset(x, yCombined), style = Stroke(width = 2.dp.toPx()))
                        drawCircle(color = greenColor, radius = 8.dp.toPx(), center = Offset(x, yRec), style = Stroke(width = 1.5.dp.toPx()))

                        val paintCombinedText = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#1B5E20")
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                        val paintRecText = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#2E7D32")
                            textSize = 9.5.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            isAntiAlias = true
                        }

                        val unitStr = if (lang == AppLanguage.EN) "" else "支"
                        drawContext.canvas.nativeCanvas.drawText("${combinedCount}$unitStr", x, (yCombined - 10.dp.toPx()).coerceAtLeast(12.dp.toPx()), paintCombinedText)
                        if (yRec != yCombined) {
                            drawContext.canvas.nativeCanvas.drawText("${item.receivedCount}$unitStr", x, (yRec - 10.dp.toPx()).coerceAtLeast(12.dp.toPx()), paintRecText)
                        }
                    }
                }

                drawPath(pathCombined, color = primaryColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                drawPath(pathRec, color = greenColor, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))))
            }
            ChartType.SCATTER -> {
                trendData.forEachIndexed { index, item ->
                    val x = leftPx + index * stepX + stepX / 2
                    val ySelf = height - bottomPx - (item.selfCount.toFloat() / maxVal) * usableHeight
                    val yShared = height - bottomPx - (item.sharedCount.toFloat() / maxVal) * usableHeight
                    val yRec = height - bottomPx - (item.receivedCount.toFloat() / maxVal) * usableHeight

                    val isSelected = index == selectedIndex
                    val baseR = if (isSelected) 8.dp.toPx() else 5.5.dp.toPx()

                    if (item.selfCount > 0) {
                        drawCircle(color = primaryColor, radius = baseR, center = Offset(x, ySelf))
                        if (isSelected) drawCircle(color = primaryColor.copy(alpha = 0.4f), radius = baseR + 4.dp.toPx(), center = Offset(x, ySelf))
                    }
                    if (item.sharedCount > 0) {
                        drawCircle(color = secondaryColor, radius = baseR - 1.dp.toPx(), center = Offset(x, yShared))
                    }
                    if (item.receivedCount > 0) {
                        drawCircle(color = greenColor, radius = baseR - 2.dp.toPx(), center = Offset(x, yRec))
                    }
                }
            }
            ChartType.PIE -> {
                val totalSelf = trendData.sumOf { it.selfCount }.toFloat()
                val totalRec = trendData.sumOf { it.receivedCount }.toFloat()
                val totalShared = trendData.sumOf { it.sharedCount }.toFloat()
                val grandTotal = (totalSelf + totalShared + totalRec).coerceAtLeast(1f)

                val sweepSelf = (totalSelf / grandTotal) * 360f
                val sweepRec = (totalRec / grandTotal) * 360f
                val sweepShared = (totalShared / grandTotal) * 360f

                val diameter = minOf(usableWidth, usableHeight) * 0.75f
                val topLeftX = leftPx + (usableWidth - diameter) / 2
                val topLeftY = topPx + (usableHeight - diameter) / 2

                val centerX = topLeftX + diameter / 2f
                val centerY = topLeftY + diameter / 2f
                val radius = diameter / 2f

                // Draw Arc 1: Self Smoked (Primary Color)
                drawArc(
                    color = primaryColor,
                    startAngle = 0f,
                    sweepAngle = sweepSelf,
                    useCenter = true,
                    topLeft = Offset(topLeftX, topLeftY),
                    size = Size(diameter, diameter)
                )
                // Draw Arc 2: Received In (Green Color)
                drawArc(
                    color = greenColor,
                    startAngle = sweepSelf,
                    sweepAngle = sweepRec,
                    useCenter = true,
                    topLeft = Offset(topLeftX, topLeftY),
                    size = Size(diameter, diameter)
                )
                // Draw Arc 3: Shared Out (Secondary Color)
                drawArc(
                    color = secondaryColor,
                    startAngle = sweepSelf + sweepRec,
                    sweepAngle = sweepShared,
                    useCenter = true,
                    topLeft = Offset(topLeftX, topLeftY),
                    size = Size(diameter, diameter)
                )

                // Draw percentage text on pie slices
                val paintPieText = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 12.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                val slices = listOf(
                    Triple(totalSelf, 0f, sweepSelf),
                    Triple(totalRec, sweepSelf, sweepRec),
                    Triple(totalShared, sweepSelf + sweepRec, sweepShared)
                )

                slices.forEach { (count, startAngle, sweepAngle) ->
                    if (sweepAngle > 10f && count > 0) { // Render label for slices with noticeable angle
                        val pct = (count / grandTotal) * 100f
                        val midAngleDeg = startAngle + sweepAngle / 2f
                        val midRad = Math.toRadians(midAngleDeg.toDouble())
                        val labelX = centerX + (radius * 0.62f) * Math.cos(midRad).toFloat()
                        val labelY = centerY + (radius * 0.62f) * Math.sin(midRad).toFloat() + 4.dp.toPx()

                        val pctText = if (pct % 1f == 0f) {
                            String.format(Locale.getDefault(), "%.0f%%", pct)
                        } else {
                            String.format(Locale.getDefault(), "%.1f%%", pct)
                        }
                        drawContext.canvas.nativeCanvas.drawText(pctText, labelX, labelY, paintPieText)
                    }
                }
            }
        }
    }
}

@Composable
fun StoreScreen(viewModel: SmokingViewModel) {
    val cigarettes by viewModel.cigarettes.collectAsStateWithLifecycle()
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val currency by viewModel.appCurrency.collectAsStateWithLifecycle()
    var showAddCigaretteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = AppStrings.get("box_title", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = AppStrings.get("box_subtitle", lang),
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
                Text(AppStrings.get("add_cigarette_btn", lang), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Swipe Gesture Hint Banner
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Swipe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.get("swipe_hint", lang),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (cigarettes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = AppStrings.get("empty_box", lang), color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(cigarettes, key = { it.id }) { cig ->
                    CigaretteItemCard(
                        cigarette = cig,
                        viewModel = viewModel,
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
            lang = lang,
            currencySymbol = currency.symbol,
            onDismiss = { showAddCigaretteDialog = false },
            onConfirm = { name, price, packSize, priceType, cartonPrice, packsPerCarton ->
                viewModel.addCigarette(name, price, packSize, priceType, cartonPrice, packsPerCarton)
                showAddCigaretteDialog = false
            }
        )
    }
}

// Expandable Card Item for Cigarette Box with swipe actions & inline editing
@Composable
fun CigaretteItemCard(
    cigarette: Cigarette,
    viewModel: SmokingViewModel,
    onUpdate: (Cigarette) -> Unit,
    onSetActive: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currency by viewModel.appCurrency.collectAsStateWithLifecycle()
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()

    var editName by remember { mutableStateOf(cigarette.name) }
    var editPriceType by remember { mutableStateOf(cigarette.priceType) }
    var editPrice by remember { mutableStateOf(cigarette.price.toString()) }
    var editCartonPrice by remember { mutableStateOf(cigarette.cartonPrice.toString()) }
    var editPackSize by remember { mutableStateOf(cigarette.packSize.toString()) }

    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipe_offset"
    )

    val unitPrice = cigarette.price / cigarette.packSize.coerceAtLeast(1)
    val packStr = "${currency.symbol}${String.format(Locale.getDefault(), "%.2f", cigarette.price)}${AppStrings.get("pack_unit", lang)}"
    val cartonStr = "${currency.symbol}${String.format(Locale.getDefault(), "%.2f", cigarette.cartonPrice)}${AppStrings.get("carton_unit", lang)}"
    val stickStr = "${currency.symbol}${String.format(Locale.getDefault(), "%.2f", unitPrice)}${AppStrings.get("stick_unit", lang)}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Swipe Background Action Indicators
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(
                    when {
                        offsetX > 20f -> Color(0xFF2E7D32) // Green for Set Active
                        offsetX < -20f -> MaterialTheme.colorScheme.error // Red for Delete
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(horizontal = 20.dp),
            horizontalArrangement = if (offsetX > 0) Arrangement.Start else Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (offsetX > 20f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = AppStrings.get("set_active_swipe", lang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else if (offsetX < -20f) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = AppStrings.get("delete_swipe", lang),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Foreground Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (cigarette.isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
            ),
            border = if (cigarette.isActive) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(cigarette.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX > 100f) {
                                if (!cigarette.isActive) onSetActive()
                            } else if (offsetX < -100f) {
                                onDelete()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-180f, 180f)
                        }
                    )
                }
                .animateContentSize()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header Row (Clean & Spacious - NO explicit buttons!)
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
                                    text = AppStrings.get("in_use", lang),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = cigarette.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (lang == AppLanguage.EN) "Toggle details" else "展开编辑",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pricing Specs Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = packStr,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                            maxLines = 1
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = cartonStr,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                            maxLines = 1
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stickStr,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
                            maxLines = 1
                        )
                    }
                }

                // Expanded Edit Section
                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Divider()

                        Text(
                            text = AppStrings.get("edit_rule_title", lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )

                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text(AppStrings.get("brand_name", lang)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = editPriceType == "PACK",
                                onClick = { editPriceType = "PACK" },
                                label = { Text(AppStrings.get("price_type_pack", lang)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = editPriceType == "CARTON",
                                onClick = { editPriceType = "CARTON" },
                                label = { Text(AppStrings.get("price_type_carton", lang)) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (editPriceType == "PACK") {
                            OutlinedTextField(
                                value = editPrice,
                                onValueChange = { editPrice = it },
                                label = { Text(AppStrings.get("pack_price", lang)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            OutlinedTextField(
                                value = editCartonPrice,
                                onValueChange = { editCartonPrice = it },
                                label = { Text(AppStrings.get("carton_price", lang)) },
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
                            Text(AppStrings.get("save_changes", lang), fontWeight = FontWeight.Bold)
                        }
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
    val lang by viewModel.appLanguage.collectAsStateWithLifecycle()
    val themeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val colorPreset by viewModel.appColorPreset.collectAsStateWithLifecycle()
    val fontFamilyState by viewModel.appFontFamily.collectAsStateWithLifecycle()
    val currency by viewModel.appCurrency.collectAsStateWithLifecycle()
    val maxIntervalHours by viewModel.maxIntervalThresholdHours.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = AppStrings.get("settings_title", lang),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // Group 1: 外观与语言 (Language, Theme Appearance, Font Family, Currency)
        WeChatSettingsGroup(title = AppStrings.get("lang_theme_card_title", lang)) {
            // 1. Language Item
            WeChatSettingsItem(
                title = AppStrings.get("select_language", lang),
                value = lang.displayName,
                icon = Icons.Rounded.Translate,
                iconBgColor = Color(0xFF1E88E5).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF1E88E5),
                onClick = { showLanguageDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 2. Theme Appearance Item
            val themeLabel = if (lang == AppLanguage.EN) themeMode.labelEn else themeMode.labelZh
            val colorLabel = if (lang == AppLanguage.EN) colorPreset.labelEn else colorPreset.labelZh
            WeChatSettingsItem(
                title = AppStrings.get("select_theme_mode", lang),
                value = "$themeLabel · $colorLabel",
                icon = Icons.Rounded.Palette,
                iconBgColor = Color(0xFF8E24AA).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF8E24AA),
                onClick = { showThemeDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 3. Font Family Item
            WeChatSettingsItem(
                title = if (lang == AppLanguage.EN) "Font Style" else "字体样式",
                value = fontFamilyState.getLabel(lang),
                icon = Icons.Rounded.FontDownload,
                iconBgColor = Color(0xFFD81B60).copy(alpha = 0.15f),
                iconTintColor = Color(0xFFD81B60),
                onClick = { showFontDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 4. Currency Item
            WeChatSettingsItem(
                title = AppStrings.get("select_currency", lang),
                subtitle = if (lang == AppLanguage.EN) "Link code & symbol" else "货币代码与符号联动",
                value = currency.getOptionLabel(lang),
                icon = Icons.Rounded.Payments,
                iconBgColor = Color(0xFF43A047).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF43A047),
                onClick = { showCurrencyDialog = true }
            )
        }

        // Group 2: 控烟目标与算法设置 (Goals & Calculation Rules)
        WeChatSettingsGroup(title = AppStrings.get("goal_card_title", lang)) {
            val dailyLimitStr = "${activeGoal?.dailyLimit ?: 10} ${if (lang == AppLanguage.EN) "sticks/day" else "支/天"}"
            WeChatSettingsItem(
                title = if (lang == AppLanguage.EN) "Daily Smoking Limit" else "每日吸烟限制",
                subtitle = if (lang == AppLanguage.EN) "Limit total" else "限制(自抽+接烟)的总和",
                value = dailyLimitStr,
                icon = Icons.Rounded.Flag,
                iconBgColor = Color(0xFFE53935).copy(alpha = 0.15f),
                iconTintColor = Color(0xFFE53935),
                onClick = { showGoalDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            val notSetText = if (lang == AppLanguage.EN) "Not set" else "未设置"
            val budgetStr = activeGoal?.monthlyBudget?.let { "${currency.symbol}$it" } ?: notSetText
            WeChatSettingsItem(
                title = if (lang == AppLanguage.EN) "Monthly Budget" else "每月烟草预算",
                value = budgetStr,
                icon = Icons.Rounded.AccountBalanceWallet,
                iconBgColor = Color(0xFFFB8C00).copy(alpha = 0.15f),
                iconTintColor = Color(0xFFFB8C00),
                onClick = { showGoalDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            val quitDateStr = activeGoal?.targetQuitDate?.let {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
            } ?: notSetText
            WeChatSettingsItem(
                title = if (lang == AppLanguage.EN) "Target Quit Date" else "完全戒烟目标日",
                value = quitDateStr,
                icon = Icons.Rounded.CalendarMonth,
                iconBgColor = Color(0xFF00ACC1).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF00ACC1),
                onClick = { showGoalDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            WeChatSettingsItem(
                title = if (lang == AppLanguage.EN) "Interval Filter Threshold" else "平均间隔过滤阈值",
                subtitle = if (lang == AppLanguage.EN) "Exclude gaps > ${maxIntervalHours}h" else "排除间隔>${maxIntervalHours}小时的数据",
                value = "${maxIntervalHours} ${if (lang == AppLanguage.EN) "hours" else "小时"}",
                icon = Icons.Rounded.Timer,
                iconBgColor = Color(0xFFD81B60).copy(alpha = 0.15f),
                iconTintColor = Color(0xFFD81B60),
                onClick = { showIntervalDialog = true }
            )
        }

        // Group 3: 数据与云端备份 (Demo Mode & Cloud Sync)
        WeChatSettingsGroup(title = if (lang == AppLanguage.EN) "Data & Sync" else "数据与同步") {
            WeChatSettingsItem(
                title = AppStrings.get("demo_card_title", lang),
                subtitle = if (lang == AppLanguage.EN) "60 days of realistic sample records" else "近60天模拟烟民真实行为数据",
                icon = Icons.Rounded.BugReport,
                iconBgColor = Color(0xFF3949AB).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF3949AB),
                showChevron = false,
                trailingContent = {
                    Switch(
                        checked = isDemoMode,
                        onCheckedChange = { viewModel.toggleDemoMode(it) }
                    )
                }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            WeChatSettingsItem(
                title = AppStrings.get("cloud_card_title", lang),
                value = if (syncState is SyncState.Success) (if (lang == AppLanguage.EN) "Synced" else "已同步") else (if (lang == AppLanguage.EN) "Tap to Backup" else "点击备份"),
                icon = Icons.Rounded.CloudSync,
                iconBgColor = Color(0xFF00897B).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF00897B),
                onClick = { showCloudSyncDialog = true }
            )
        }

        // Group 4: 关于 (About)
        WeChatSettingsGroup(title = if (lang == AppLanguage.EN) "About App" else "关于软件") {
            WeChatSettingsItem(
                title = if (lang == AppLanguage.EN) "Refresh AI Coach Advice" else "刷新 AI 戒烟教练建议",
                value = if (lang == AppLanguage.EN) "Tap to Refresh" else "点击刷新",
                icon = Icons.Rounded.AutoAwesome,
                iconBgColor = Color(0xFF7E57C2).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF7E57C2),
                onClick = { viewModel.fetchAiAdvice() }
            )

            HorizontalDivider(modifier = Modifier.padding(start = 58.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            WeChatSettingsItem(
                title = AppStrings.get("about_title", lang),
                subtitle = AppStrings.get("about_subtitle", lang),
                value = "v2.0",
                icon = Icons.Rounded.Info,
                iconBgColor = Color(0xFF757575).copy(alpha = 0.15f),
                iconTintColor = Color(0xFF757575),
                onClick = { showAboutDialog = true }
            )
        }
    }

    // ================= Dialogs =================
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.get("about_dialog_title", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = AppStrings.get("about_desc", lang),
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(AppStrings.get("software_version", lang), fontSize = 13.sp, color = Color.Gray)
                        Text("v2.0 (Build 2026.07)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(AppStrings.get("developer", lang), fontSize = 13.sp, color = Color.Gray)
                        Text("Nonion", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(AppStrings.get("contact_email", lang), fontSize = 13.sp, color = Color.Gray)
                        Text("nonion.pl@gmail.com", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(AppStrings.get("confirm", lang))
                }
            }
        )
    }
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(AppStrings.get("select_language", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.values().forEach { l ->
                        Surface(
                            onClick = {
                                viewModel.setAppLanguage(l)
                                showLanguageDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (lang == l) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = l.displayName,
                                    fontWeight = if (lang == l) FontWeight.Bold else FontWeight.Normal
                                )
                                if (lang == l) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(AppStrings.get("cancel", lang)) }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.EN) "Theme Appearance Settings" else "主题外观设置", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Theme Mode
                    Text(if (lang == AppLanguage.EN) "Theme Mode" else "主题模式", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppThemeMode.values().forEach { mode ->
                            val label = if (lang == AppLanguage.EN) mode.labelEn else mode.labelZh
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setAppThemeMode(mode) },
                                label = { Text(label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    HorizontalDivider()

                    // Color Preset
                    Text(if (lang == AppLanguage.EN) "Color Palette" else "配色方案", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppColorPreset.values().forEach { preset ->
                            val label = if (lang == AppLanguage.EN) preset.labelEn else preset.labelZh
                            Surface(
                                onClick = { viewModel.setAppColorPreset(preset) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (colorPreset == preset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(preset.primary, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(label, fontWeight = if (colorPreset == preset) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                    }
                                    if (colorPreset == preset) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showThemeDialog = false }) { Text(if (lang == AppLanguage.EN) "Done" else "完成") }
            }
        )
    }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FontDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.EN) "Font Style Settings" else "字体样式设置", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFontFamily.values().forEach { fontItem ->
                        Surface(
                            onClick = {
                                viewModel.setAppFontFamily(fontItem)
                                showFontDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (fontFamilyState == fontItem) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = fontItem.getLabel(lang),
                                        fontFamily = fontItem.fontFamily,
                                        fontWeight = if (fontFamilyState == fontItem) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (lang == AppLanguage.EN) "Preview: Smoke Tracker Guard 12345" else "预览文字: Guard控烟助手 12345",
                                        fontFamily = fontItem.fontFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (fontFamilyState == fontItem) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) { Text(AppStrings.get("cancel", lang)) }
            }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.EN) "Currency Settings" else "货币设置", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Select currency used for prices and expense analytics:" else "请选择用于界面全域价格与开销统计的货币：",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AppCurrency.values().forEach { curr ->
                        val isSelected = currency == curr
                        Surface(
                            onClick = {
                                viewModel.setAppCurrency(curr)
                                showCurrencyDialog = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = curr.getOptionLabel(lang),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text(AppStrings.get("cancel", lang)) }
            }
        )
    }

    if (showGoalDialog) {
        var limitInput by remember { mutableStateOf(activeGoal?.dailyLimit?.toString() ?: "10") }
        var budgetInput by remember { mutableStateOf(activeGoal?.monthlyBudget?.toString() ?: "") }
        var quitDateTs by remember { mutableStateOf(activeGoal?.targetQuitDate) }

        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.EN) "Smoking & Budget Goals" else "控烟与预算目标", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Note: Daily limit applies to total [Self Smoked + Received In] per day!" else "提示：每日吸烟限制量针对的是每天的【自购自抽 + 社交接烟】总和支数！",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = limitInput,
                        onValueChange = { limitInput = it },
                        label = { Text(AppStrings.get("daily_limit_label", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text(AppStrings.get("monthly_budget_label", lang, currency.symbol)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    val dateLabel = quitDateTs?.let {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                    } ?: (if (lang == AppLanguage.EN) "Tap to select target quit date" else "点击选择完全戒烟目标日")

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
                                    quitDateTs = selected.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(dateLabel, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = limitInput.toIntOrNull() ?: 10
                        val budget = budgetInput.toDoubleOrNull()
                        viewModel.updateGoal(limit, quitDateTs, budget)
                        showGoalDialog = false
                    }
                ) {
                    Text(if (lang == AppLanguage.EN) "Save" else "保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) { Text(AppStrings.get("cancel", lang)) }
            }
        )
    }

    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.EN) "Interval Threshold Settings" else "平均间隔过滤阈值设置", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = AppStrings.get("interval_threshold_desc", lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Text(if (lang == AppLanguage.EN) "Select threshold X (Default 6 hours):" else "选择阈值 X（默认 6 小时）:", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(4, 6, 8, 12).forEach { hours ->
                            FilterChip(
                                selected = maxIntervalHours == hours,
                                onClick = { viewModel.setMaxIntervalThresholdHours(hours) },
                                label = { Text("${hours}${if (lang == AppLanguage.EN) "h" else "小时"}", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    var customInput by remember(maxIntervalHours) { mutableStateOf(maxIntervalHours.toString()) }
                    OutlinedTextField(
                        value = customInput,
                        onValueChange = { input ->
                            customInput = input
                            input.toIntOrNull()?.let { h ->
                                if (h in 1..24) {
                                    viewModel.setMaxIntervalThresholdHours(h)
                                }
                            }
                        },
                        label = { Text(if (lang == AppLanguage.EN) "Custom threshold (1-24 hours)" else "自定义阈值 (1-24小时)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showIntervalDialog = false }) { Text(AppStrings.get("confirm", lang)) }
            }
        )
    }

    if (showCloudSyncDialog) {
        AlertDialog(
            onDismissRequest = { showCloudSyncDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (lang == AppLanguage.EN) "Cloud Backup & Restore" else "云端数据备份与恢复", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = AppStrings.get("cloud_desc", lang),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.backupData()
                                showCloudSyncDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("upload_backup", lang), fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.restoreData()
                                showCloudSyncDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("download_backup", lang), fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCloudSyncDialog = false }) { Text(if (lang == AppLanguage.EN) "Close" else "关闭") }
            }
        )
    }
}

@Composable
fun WeChatSettingsGroup(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp, top = 4.dp)
            )
        }
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
fun WeChatSettingsItem(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconBgColor: Color = MaterialTheme.colorScheme.primaryContainer,
    iconTintColor: Color = MaterialTheme.colorScheme.primary,
    showChevron: Boolean = true,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconBgColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTintColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (value != null) {
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            if (trailingContent != null) {
                trailingContent()
            } else if (showChevron) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
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
    lang: AppLanguage = AppLanguage.ZH,
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
                    text = AppStrings.get("add_log_title", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (cigarettes.isEmpty()) {
                    Text(
                        text = if (lang == AppLanguage.EN) "Notice: Please add cigarette brands in [Box] tab first." else "提示：请先去【烟盒】栏添加常用的香烟种类与价格。",
                        color = Color.Red,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(if (lang == AppLanguage.EN) "Got it" else "知道了")
                    }
                } else {
                    // Cigarette Dropdown
                    Text(if (lang == AppLanguage.EN) "Select Brand:" else "选择香烟品牌:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedCigarette?.name ?: AppStrings.get("select_cigarette", lang),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            cigarettes.forEach { cig ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = cig.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        selectedCigarette = cig
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Scene Selection Tabs (Requirement 1)
                    Text(if (lang == AppLanguage.EN) "Select Log Scene:" else "选择打卡场景:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedLogType == "SELF",
                                onClick = { selectedLogType = "SELF" },
                                label = { Text(if (lang == AppLanguage.EN) "Self Purchase" else "自购自抽", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedLogType == "SHARED_OUT",
                                onClick = { selectedLogType = "SHARED_OUT" },
                                label = { Text(if (lang == AppLanguage.EN) "Shared Out" else "社交递烟", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedLogType == "RECEIVED_IN",
                                onClick = { selectedLogType = "RECEIVED_IN" },
                                label = { Text(if (lang == AppLanguage.EN) "Received In" else "社交接烟", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val explanationText = when (selectedLogType) {
                            "SELF" -> if (lang == AppLanguage.EN) "Self purchased cigarette: Counted towards health volume and personal expense." else "自己购买的香烟自己抽：计入个人健康抽烟量，并计入财务开销。"
                            "SHARED_OUT" -> if (lang == AppLanguage.EN) "Shared your cigarette to others: Not counted in your smoking intake, but counted in expense." else "掏自己的烟递给朋友/同事：不计入个人健康超标量，但计入开销。"
                            "RECEIVED_IN" -> if (lang == AppLanguage.EN) "Accepted cigarette from others: Counted towards your smoking intake, but free." else "接别人递过来的烟抽（他人买单）：计入个人健康抽烟量，但不花自己钱（免费）。"
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
                        label = { Text(AppStrings.get("log_quantity", lang)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Note input
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(AppStrings.get("log_note", lang)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(AppStrings.get("cancel", lang))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val cigId = selectedCigarette?.id ?: 1
                                val qty = quantity.toIntOrNull() ?: 1
                                onConfirm(cigId, qty, selectedLogType, note)
                            }
                        ) {
                            Text(if (lang == AppLanguage.EN) "Confirm Log" else "确认打卡", fontWeight = FontWeight.Bold)
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
    lang: AppLanguage = AppLanguage.ZH,
    currencySymbol: String = "¥",
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
                    text = AppStrings.get("add_cigarette_title", lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (lang == AppLanguage.EN) "Brand Name (e.g. Marlboro, Chunghwa)" else "香烟名称/品牌 (如: 中华, 炫赫门)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(if (lang == AppLanguage.EN) "Pricing Rule Mode:" else "选择计价类型规则:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = priceType == "PACK",
                        onClick = { priceType = "PACK" },
                        label = { Text(if (lang == AppLanguage.EN) "📦 Per Pack Rule" else "📦 单包零售价规则", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = priceType == "CARTON",
                        onClick = { priceType = "CARTON" },
                        label = { Text(if (lang == AppLanguage.EN) "🧱 Per Carton Rule" else "🧱 单条零售价规则", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (priceType == "PACK") {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text(if (lang == AppLanguage.EN) "Pack Price ($currencySymbol)" else "单包零售价格 (元)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        label = { Text(if (lang == AppLanguage.EN) "Carton (10 Packs) Price ($currencySymbol)" else "单条(整条10包)零售价格 (元)") },
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
                        label = { Text(if (lang == AppLanguage.EN) "Sticks/Pack (Default 20)" else "每包支数 (默认20)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = packsPerCartonInput,
                        onValueChange = { packsPerCartonInput = it },
                        label = { Text(if (lang == AppLanguage.EN) "Packs/Carton (Default 10)" else "每条包数 (默认10)") },
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
                            text = if (lang == AppLanguage.EN) "💡 Live Price Breakdown:" else "💡 价格自动折算预览:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (lang == AppLanguage.EN)
                                "Per Pack: $currencySymbol${String.format(Locale.getDefault(), "%.2f", packPrice)} | Per Carton: $currencySymbol${String.format(Locale.getDefault(), "%.2f", cartonPrice)} | Per Stick: $currencySymbol${String.format(Locale.getDefault(), "%.2f", unitPrice)}"
                            else
                                "折合单包: $currencySymbol${String.format(Locale.getDefault(), "%.2f", packPrice)} | 折合单条: $currencySymbol${String.format(Locale.getDefault(), "%.2f", cartonPrice)} | 单支成本: $currencySymbol${String.format(Locale.getDefault(), "%.2f", unitPrice)}",
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
                        Text(AppStrings.get("cancel", lang))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && rawVal > 0) {
                                onConfirm(name, packPrice, pSize, priceType, cartonPrice, ppCarton)
                            }
                        }
                    ) {
                        Text(if (lang == AppLanguage.EN) "Save Brand" else "保存烟草", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
