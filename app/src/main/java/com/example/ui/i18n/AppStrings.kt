package com.example.ui.i18n

import androidx.compose.ui.graphics.Color

enum class AppLanguage(val code: String, val displayName: String) {
    ZH("zh", "简体中文"),
    EN("en", "English")
}

enum class AppThemeMode(val code: String, val labelZh: String, val labelEn: String) {
    SYSTEM("system", "跟随系统", "System Default"),
    LIGHT("light", "浅色模式", "Light Mode"),
    DARK("dark", "深色模式", "Dark Mode")
}

enum class AppColorPreset(val code: String, val labelZh: String, val labelEn: String, val primary: Color) {
    DEFAULT("default", "紫青典雅 (默认)", "Purple & Teal", Color(0xFF6750A4)),
    HEALTH_GREEN("green", "健康防护绿", "Emerald Green", Color(0xFF1B5E20)),
    DEEP_BLUE("blue", "深海科技蓝", "Ocean Blue", Color(0xFF0D47A1)),
    WARM_AMBER("amber", "暖阳火琥珀", "Warm Amber", Color(0xFFD84315))
}

object AppStrings {
    fun get(key: String, lang: AppLanguage, vararg args: Any): String {
        val map = if (lang == AppLanguage.EN) enMap else zhMap
        val template = map[key] ?: zhMap[key] ?: key
        return try {
            if (args.isNotEmpty()) String.format(template, *args) else template
        } catch (e: Exception) {
            template
        }
    }

    private val zhMap = mapOf(
        "app_name" to "吸烟跟踪Guard",
        "tab_home" to "首页",
        "tab_trends" to "趋势",
        "tab_store" to "烟盒",
        "tab_settings" to "设置",
        
        "title_home" to "吸烟跟踪Guard",
        "title_trends" to "趋势与统计",
        "title_store" to "我的烟盒",
        "title_settings" to "设置与外观",

        "over_limit_warning" to "今日吸烟（%d支）已超出目标限制（%d支），请注意健康并尽量少抽！",
        "active_brand_label" to "当前使用烟草品牌",
        "unset_default_brand" to "未设置 (默认中华)",
        "per_pack_price" to "¥%.2f / 包",
        "stat_today_self" to "今日自抽",
        "stat_today_shared" to "社交递烟",
        "stat_today_received" to "社交接烟",
        "stat_today_cost" to "今日吸烟开销",
        "stat_today_saved" to "接烟省下金额",
        "target_subtitle" to "目标 %d 支",
        "shared_subtitle" to "分享他人",
        "received_subtitle" to "免费蹭烟",
        "cost_subtitle" to "自购+递烟花费",
        "saved_subtitle" to "他请客省下的",

        "quick_record_title" to "快速打卡极速记录",
        "quick_self_btn" to "🚬 自购自抽 +1",
        "quick_shared_btn" to "🤝 社交递烟 +1",
        "quick_received_btn" to "🎁 社交接烟 +1",
        "custom_record_btn" to "自定义多支 / 备注记录",

        "ai_advice_title" to "AI 戒烟教练分析与建议",
        "ai_advice_loading" to "AI 戒烟教练正在根据您的最新吸烟与社交接烟数据分析中...",
        
        "today_logs_header" to "今日打卡记录明细",
        "log_count" to "共 %d 条",
        "no_logs_today" to "今日尚未打卡，点击上方按钮开始记录第一支吧！",
        "type_self" to "自购自抽",
        "type_shared" to "社交递烟",
        "type_received" to "社交接烟",
        "free_badge" to "¥0.00 (免费)",

        "time_range_title" to "选择统计分析时间范围",
        "chart_type_title" to "图表展示形式",
        "financial_analysis_title" to "吸烟开销与社交性价比分析",
        "self_trend_section_title" to "自我吸烟量与行为趋势分析",
        "stat_daily_avg_self" to "日均自抽量",
        "stat_avg_interval" to "平均吸烟间隔",
        "stat_peak_hours" to "集中高峰时段",
        "stat_total_period_self" to "区间自抽总量",
        "chart_click_hint" to "💡 点击图表任意柱条/节点可交互查看当日详情",
        "selected_detail_title" to "数据节点详情: %s",
        "detail_self_count" to "自购自抽: %d 支",
        "detail_shared_count" to "社交递烟: %d 支",
        "detail_received_count" to "社交接烟: %d 支",
        "detail_interval" to "当日平均间隔: %s",
        "detail_peak" to "当日高发时段: %s",
        "detail_cost" to "当日支出: ¥%.2f",
        "no_interval_data" to "数据不足",
        "interval_filter_card_title" to "平均间隔计算与过滤设置",
        "interval_threshold_label" to "排除大于 X 小时的间隔",
        "interval_threshold_desc" to "计算平均吸烟间隔时，自动排除超过设定小时数 (如夜间睡眠/长休) 的异常间距，默认为 6 小时。",
        "insight_title" to "行为模式与建议",
        "self_cost" to "自购自抽支出",
        "shared_cost" to "社交递烟支出",
        "received_saved" to "社交接烟节省",
        "total_spent" to "当前时段实际总支金:",
        "no_chart_data" to "暂无当前时段的数据记录",

        "range_today" to "今日",
        "range_7days" to "近7天",
        "range_30days" to "近30天",
        "range_this_month" to "本月",
        "range_spec_month" to "指定月份",
        "range_spec_year" to "指定年份",
        "range_custom" to "自定义范围",

        "chart_bar" to "柱状图",
        "chart_line" to "折线图",
        "chart_scatter" to "散点图",
        "chart_pie" to "饼图",

        "box_title" to "我的烟盒库 (Cigarette Box)",
        "box_subtitle" to "管理常用香烟种类与零售价格规则",
        "add_cigarette_btn" to "添加烟草",
        "empty_box" to "烟盒为空，请点击右上角【添加烟草】",
        "in_use" to "使用中",
        "set_as_active" to "设为当前",

        "settings_title" to "设置",
        "lang_theme_card_title" to "外观与语言",
        "select_language" to "语言 (Language)",
        "select_theme_mode" to "主题外观",
        "select_color_preset" to "配色方案",
        "select_currency" to "货币 (Currency)",
        "currency_option_label" to "货币设置",
        
        "goal_card_title" to "控烟目标与算法",
        "daily_limit_label" to "每日吸烟限制 (自抽+接烟, 支/天)",
        "monthly_budget_label" to "每月烟草预算金额 (%s, 可选)",
        "target_quit_date_label" to "完全戒烟目标日: %s",
        "set_target_quit_date" to "设置完全戒烟目标日 (可选)",
        "save_goal_btn" to "保存目标与计划",

        "demo_card_title" to "Demo 演示体验数据模式",
        "demo_toggle_label" to "开启 Demo 模拟体验数据",
        "demo_toggle_desc" to "开启后将自动注入近60天包含自抽、社交递烟与接烟的模拟记录，关闭后自动恢复干净真实数据。",

        "cloud_card_title" to "云备份 & 同步 (Cloud Backup)",
        "cloud_desc" to "一键将本地的吸烟历史、烟盒种类、戒烟目标同步上传至云端服务器，更换手机设备时可随时拉取还原。",
        "upload_backup" to "一键备份上传",
        "download_backup" to "一键拉取恢复",

        "cancel" to "取消",
        "confirm" to "确定",
        "add_log_title" to "添加打卡记录",
        "select_cigarette" to "选择烟草",
        "log_quantity" to "吸烟数量 (支)",
        "log_type" to "打卡类型",
        "log_note" to "备注/社交场景 (可选)",
        "add_cigarette_title" to "添加新香烟",
        "brand_name" to "香烟品牌名称",
        "price_type" to "计价方式",
        "price_type_pack" to "按单包计算",
        "price_type_carton" to "按单条(10包)计算",
        "pack_price" to "单包售价 (元)",
        "carton_price" to "单条售价 (元)",
        "pack_size" to "每包支数 (默认20)",
        "packs_per_carton" to "每条包数 (默认10)"
    )

    private val enMap = mapOf(
        "app_name" to "Smoke Tracker Guard",
        "tab_home" to "Home",
        "tab_trends" to "Trends",
        "tab_store" to "Box",
        "tab_settings" to "Settings",

        "title_home" to "Smoke Tracker Guard",
        "title_trends" to "Trends & Analytics",
        "title_store" to "My Cigarette Box",
        "title_settings" to "Settings & Theme",

        "over_limit_warning" to "Today's smoking (%d sticks) exceeds target limit (%d sticks). Please watch your health!",
        "active_brand_label" to "Active Cigarette Brand",
        "unset_default_brand" to "Not set (Default Chunghwa)",
        "per_pack_price" to "¥%.2f / pack",
        "stat_today_self" to "Self Smoked",
        "stat_today_shared" to "Shared Out",
        "stat_today_received" to "Received In",
        "stat_today_cost" to "Today Cost",
        "stat_today_saved" to "Money Saved",
        "target_subtitle" to "Goal %d sticks",
        "shared_subtitle" to "Given away",
        "received_subtitle" to "Free cigarettes",
        "cost_subtitle" to "Self + Shared cost",
        "saved_subtitle" to "Saved by free sticks",

        "quick_record_title" to "Quick Log Actions",
        "quick_self_btn" to "🚬 Self Smoked +1",
        "quick_shared_btn" to "🤝 Shared Out +1",
        "quick_received_btn" to "🎁 Received In +1",
        "custom_record_btn" to "Custom Log / Add Notes",

        "ai_advice_title" to "AI Coach Insights & Advice",
        "ai_advice_loading" to "AI Coach is analyzing your smoking & social records...",

        "today_logs_header" to "Today's Log Entries",
        "log_count" to "Total %d entries",
        "no_logs_today" to "No logs recorded today. Tap quick actions above to record your first stick!",
        "type_self" to "Self Smoked",
        "type_shared" to "Shared Out",
        "type_received" to "Received In",
        "free_badge" to "¥0.00 (Free)",

        "time_range_title" to "Select Analytics Period",
        "chart_type_title" to "Chart Format",
        "financial_analysis_title" to "Expense & Social Savings Analysis",
        "self_trend_section_title" to "Self Consumption & Behavior Trend",
        "stat_daily_avg_self" to "Daily Avg (Self)",
        "stat_avg_interval" to "Avg Interval",
        "stat_peak_hours" to "Peak Hours",
        "stat_total_period_self" to "Period Self Total",
        "chart_click_hint" to "💡 Tap any bar or point on the chart for daily details",
        "selected_detail_title" to "Selected Node Detail: %s",
        "detail_self_count" to "Self Smoked: %d sticks",
        "detail_shared_count" to "Shared Out: %d sticks",
        "detail_received_count" to "Received In: %d sticks",
        "detail_interval" to "Daily Avg Interval: %s",
        "detail_peak" to "Daily Peak Slot: %s",
        "detail_cost" to "Daily Expense: ¥%.2f",
        "no_interval_data" to "N/A",
        "interval_filter_card_title" to "Average Interval Calculation Settings",
        "interval_threshold_label" to "Exclude intervals > X hours",
        "interval_threshold_desc" to "Excludes long gaps (such as overnight sleep) greater than the threshold hours when calculating average smoking interval (Default: 6h).",
        "insight_title" to "Behavioral Pattern & Advice",
        "self_cost" to "Self Smoked Cost",
        "shared_cost" to "Shared Out Cost",
        "received_saved" to "Received In Saved",
        "total_spent" to "Actual Period Total Spent:",
        "no_chart_data" to "No data recorded for selected period",

        "range_today" to "Today",
        "range_7days" to "Last 7 Days",
        "range_30days" to "Last 30 Days",
        "range_this_month" to "This Month",
        "range_spec_month" to "Select Month",
        "range_spec_year" to "Select Year",
        "range_custom" to "Custom Range",

        "chart_bar" to "Bar",
        "chart_line" to "Line",
        "chart_scatter" to "Scatter",
        "chart_pie" to "Pie",

        "box_title" to "Cigarette Box Library",
        "box_subtitle" to "Manage cigarette brands & price rules",
        "add_cigarette_btn" to "Add Brand",
        "empty_box" to "Cigarette box is empty. Tap [Add Brand] above.",
        "in_use" to "Active",
        "set_as_active" to "Set Active",

        "settings_title" to "Settings",
        "lang_theme_card_title" to "Appearance & Language",
        "select_language" to "Language",
        "select_theme_mode" to "Theme Mode",
        "select_color_preset" to "Color Palette",
        "select_currency" to "Currency",
        "currency_option_label" to "Currency Options",

        "goal_card_title" to "Goals & Algorithm",
        "daily_limit_label" to "Daily Limit (Self + Received, sticks/day)",
        "monthly_budget_label" to "Monthly Budget (%s, optional)",
        "target_quit_date_label" to "Target Quit Date: %s",
        "set_target_quit_date" to "Set Target Quit Date (optional)",
        "save_goal_btn" to "Save Goals & Plan",

        "demo_card_title" to "Demo Simulation Data Mode",
        "demo_toggle_label" to "Enable Demo Simulated Records",
        "demo_toggle_desc" to "Injects 60 days of sample smoking & social records. Turn off to restore real data.",

        "cloud_card_title" to "Cloud Sync & Backup",
        "cloud_desc" to "Backup your smoking logs, cigarette box, and goals to the cloud to restore across devices.",
        "upload_backup" to "Upload Backup",
        "download_backup" to "Restore Data",

        "cancel" to "Cancel",
        "confirm" to "Confirm",
        "add_log_title" to "Add Smoking Log",
        "select_cigarette" to "Select Brand",
        "log_quantity" to "Quantity (Sticks)",
        "log_type" to "Log Type",
        "log_note" to "Note / Social Context (optional)",
        "add_cigarette_title" to "Add New Cigarette Brand",
        "brand_name" to "Brand Name",
        "price_type" to "Pricing Mode",
        "price_type_pack" to "Per Pack",
        "price_type_carton" to "Per Carton (10 Packs)",
        "pack_price" to "Pack Price (¥)",
        "carton_price" to "Carton Price (¥)",
        "pack_size" to "Sticks per Pack (default 20)",
        "packs_per_carton" to "Packs per Carton (default 10)"
    )
}
