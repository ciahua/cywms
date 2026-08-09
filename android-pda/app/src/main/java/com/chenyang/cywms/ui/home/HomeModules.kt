package com.chenyang.cywms.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.AssignmentReturn
import androidx.compose.material.icons.automirrored.outlined.CompareArrows
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.Outbox
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class HomeModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

/** 分组色：低饱和工业色，同组同色，避免花哨 */
data class GroupAccent(
    val accent: Color,
    /** 方格底色：深蓝上叠一抹分组色 */
    val tileBg: Color,
    val iconWell: Color,
    val border: Color
)

data class HomeGroup(
    val title: String,
    val accent: GroupAccent,
    val modules: List<HomeModule>
)

private fun accentOf(accent: Color): GroupAccent = GroupAccent(
    accent = accent,
    tileBg = Color(
        red = accent.red * 0.22f + 0.08f,
        green = accent.green * 0.22f + 0.10f,
        blue = accent.blue * 0.22f + 0.16f,
        alpha = 1f
    ),
    iconWell = accent.copy(alpha = 0.22f),
    border = accent.copy(alpha = 0.35f)
)

// 六组六色：琥珀 / 青瓷 / 雾蓝 / 陶土 / 藤紫 / 鼠尾草 —— 皆偏暗偏灰
private val AccentIssue = accentOf(Color(0xFFE0A04A))      // 领料 · 琥珀
private val AccentInbound = accentOf(Color(0xFF5FA8A0))    // 入库 · 青瓷
private val AccentProduct = accentOf(Color(0xFF6B8FCE))    // 成品 · 雾蓝
private val AccentReturn = accentOf(Color(0xFFC4846A))     // 退料 · 陶土
private val AccentStocktake = accentOf(Color(0xFF9A86C8))  // 盘点 · 藤紫
private val AccentInternal = accentOf(Color(0xFF7A9E78))   // 库内 · 鼠尾草

/** 覆盖旧版首页 14 模块，按作业类型分组展示 */
val HomeGroups: List<HomeGroup> = listOf(
    HomeGroup(
        title = "领料作业",
        accent = AccentIssue,
        modules = listOf(
            HomeModule("produce_issue", "生产领料", "工单发料扫码", Icons.Outlined.Outbox),
            HomeModule("outsource_issue", "委外领料", "委外发料扫码", Icons.Outlined.LocalShipping),
            HomeModule("over_issue", "物料超领", "超领单扫码", Icons.Outlined.MoveToInbox)
        )
    ),
    HomeGroup(
        title = "入库作业",
        accent = AccentInbound,
        modules = listOf(
            HomeModule("quick_stockin", "快捷入库", "储位上架", Icons.Outlined.Inventory2),
            HomeModule("idle_stockin", "呆料入库", "呆料管理入库", Icons.Outlined.Warehouse)
        )
    ),
    HomeGroup(
        title = "成品作业",
        accent = AccentProduct,
        modules = listOf(
            HomeModule("product_stockin", "成品入库", "成品扫码入库", Icons.Outlined.MoveToInbox),
            HomeModule("product_stockout", "成品出库", "发货出库扫码", Icons.Outlined.LocalShipping)
        )
    ),
    HomeGroup(
        title = "退料作业",
        accent = AccentReturn,
        modules = listOf(
            HomeModule("produce_return", "生产退料", "生产退料扫码", Icons.Outlined.Replay),
            HomeModule("return_outbound", "退料出库", "退料出库扫码", Icons.AutoMirrored.Outlined.AssignmentReturn)
        )
    ),
    HomeGroup(
        title = "盘点作业",
        accent = AccentStocktake,
        modules = listOf(
            HomeModule("stock_taking", "仓库盘点", "物料盘点", Icons.AutoMirrored.Outlined.FactCheck),
            HomeModule("product_taking", "成品盘点", "成品盘点", Icons.Outlined.QrCodeScanner)
        )
    ),
    HomeGroup(
        title = "库内作业",
        accent = AccentInternal,
        modules = listOf(
            HomeModule("stock_moving", "移库操作", "储位转移", Icons.Outlined.SwapHoriz),
            HomeModule("change_barcode", "更换标签", "条码更换", Icons.AutoMirrored.Outlined.CompareArrows),
            HomeModule("double_check", "物料重检", "效期/重检", Icons.Outlined.Science)
        )
    )
)
