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
import androidx.compose.ui.graphics.vector.ImageVector

data class HomeModule(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

data class HomeGroup(
    val title: String,
    val modules: List<HomeModule>
)

/** 覆盖旧版首页 14 模块，按作业类型分组展示 */
val HomeGroups: List<HomeGroup> = listOf(
    HomeGroup(
        title = "领料作业",
        modules = listOf(
            HomeModule("produce_issue", "生产领料", "工单发料扫码", Icons.Outlined.Outbox),
            HomeModule("outsource_issue", "委外领料", "委外发料扫码", Icons.Outlined.LocalShipping),
            HomeModule("over_issue", "物料超领", "超领单扫码", Icons.Outlined.MoveToInbox)
        )
    ),
    HomeGroup(
        title = "入库作业",
        modules = listOf(
            HomeModule("quick_stockin", "快捷入库", "储位上架", Icons.Outlined.Inventory2),
            HomeModule("idle_stockin", "呆料入库", "呆料管理入库", Icons.Outlined.Warehouse)
        )
    ),
    HomeGroup(
        title = "成品作业",
        modules = listOf(
            HomeModule("product_stockin", "成品入库", "成品扫码入库", Icons.Outlined.MoveToInbox),
            HomeModule("product_stockout", "成品出库", "发货出库扫码", Icons.Outlined.LocalShipping)
        )
    ),
    HomeGroup(
        title = "退料作业",
        modules = listOf(
            HomeModule("produce_return", "生产退料", "生产退料扫码", Icons.Outlined.Replay),
            HomeModule("return_outbound", "退料出库", "退料出库扫码", Icons.AutoMirrored.Outlined.AssignmentReturn)
        )
    ),
    HomeGroup(
        title = "盘点作业",
        modules = listOf(
            HomeModule("stock_taking", "仓库盘点", "物料盘点", Icons.AutoMirrored.Outlined.FactCheck),
            HomeModule("product_taking", "成品盘点", "成品盘点", Icons.Outlined.QrCodeScanner)
        )
    ),
    HomeGroup(
        title = "库内作业",
        modules = listOf(
            HomeModule("stock_moving", "移库操作", "储位转移", Icons.Outlined.SwapHoriz),
            HomeModule("change_barcode", "更换标签", "条码更换", Icons.AutoMirrored.Outlined.CompareArrows),
            HomeModule("double_check", "物料重检", "效期/重检", Icons.Outlined.Science)
        )
    )
)
