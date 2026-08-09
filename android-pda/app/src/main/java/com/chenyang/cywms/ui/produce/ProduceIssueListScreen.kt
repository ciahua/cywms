package com.chenyang.cywms.ui.produce

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chenyang.cywms.data.model.Shengchanlingliao
import com.chenyang.cywms.ui.common.PdaScaffold
import com.chenyang.cywms.ui.common.imeOnDoubleTap
import com.chenyang.cywms.ui.theme.Amber500
import com.chenyang.cywms.ui.theme.Navy700
import com.chenyang.cywms.ui.theme.Slate200
import com.chenyang.cywms.ui.theme.Slate400
import com.chenyang.cywms.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

@Composable
fun ProduceIssueListRoute(
    viewModel: ProduceIssueListViewModel,
    onBack: () -> Unit,
    onOpenOrder: (mainId: String, orderNo: String) -> Unit
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    var toastText by remember { mutableStateOf("") }
    LaunchedEffect(state.toast) {
        val t = state.toast
        if (!t.isNullOrBlank()) {
            toastText = t
            delay(1800)
            viewModel.consumeToast()
        }
    }
    ProduceIssueListScreen(
        state = state,
        visibleOrders = viewModel.visibleOrders(),
        toastText = if (state.toast != null) toastText else "",
        onBack = onBack,
        onSearchChange = viewModel::onSearchChange,
        onSearch = viewModel::search,
        onRefresh = { viewModel.refresh() },
        onToggleCompleted = viewModel::toggleShowCompleted,
        onOpenOrder = onOpenOrder
    )
}

@Composable
fun ProduceIssueListScreen(
    state: ProduceIssueListUiState,
    visibleOrders: List<Shengchanlingliao>,
    toastText: String,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onToggleCompleted: () -> Unit,
    onOpenOrder: (mainId: String, orderNo: String) -> Unit
) {
    PdaScaffold(
        title = "生产领料",
        subtitle = if (state.showCompleted) "全部单据" else "待领单据",
        onBack = onBack,
        actions = {
            IconButton(onClick = onToggleCompleted) {
                Icon(Icons.Outlined.FilterList, contentDescription = "筛选", tint = Slate400)
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = "刷新", tint = Slate400)
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Navy700)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            BasicTextField(
                value = state.search,
                onValueChange = onSearchChange,
                singleLine = true,
                textStyle = TextStyle(color = Slate200, fontSize = 14.sp),
                cursorBrush = SolidColor(Amber500),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier
                    .weight(1f)
                    .imeOnDoubleTap(),
                decorationBox = { inner ->
                    Box {
                        if (state.search.isEmpty()) {
                            Text("单号/指令，双击再弹键盘", color = Slate400, fontSize = 13.sp)
                        }
                        inner()
                    }
                }
            )
            Text(
                text = "搜索",
                color = Amber500,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable(onClick = onSearch)
                    .padding(start = 8.dp)
            )
        }

        when {
            state.loading -> FullScreenLoading()
            state.error != null && visibleOrders.isEmpty() -> FullScreenMessage(state.error ?: "")
            visibleOrders.isEmpty() -> FullScreenMessage(
                if (state.showCompleted) "暂无领料单" else "暂无待领单据（可点筛选看全部）"
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleOrders, key = { it.id.orEmpty() + it.lingliaodanhao.orEmpty() }) { order ->
                        OrderCard(order = order) {
                            val id = order.id.orEmpty()
                            if (id.isNotBlank()) {
                                onOpenOrder(id, order.lingliaodanhao.orEmpty())
                            }
                        }
                    }
                }
            }
        }

        if (toastText.isNotBlank()) {
            Text(
                text = toastText,
                color = Amber500,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OrderCard(order: Shengchanlingliao, onClick: () -> Unit) {
    val done = order.status == "1"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Navy700)
            .border(
                1.dp,
                if (done) Slate400.copy(alpha = 0.2f) else Amber500.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = order.lingliaodanhao ?: "—",
                color = Slate200,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            Text(
                text = if (done) "已完成" else "待领",
                color = if (done) SuccessGreen else Amber500,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "指令 ${order.shengchanzhiling ?: "—"}",
            color = Slate400,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = listOfNotNull(
                order.lingliaobumen?.takeIf { it.isNotBlank() },
                order.createdt?.takeIf { it.isNotBlank() }
            ).joinToString(" · ").ifBlank { "—" },
            color = Slate400.copy(alpha = 0.85f),
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}


@Composable
private fun FullScreenLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Amber500, strokeWidth = 2.dp)
    }
}

@Composable
private fun FullScreenMessage(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Slate400, fontSize = 14.sp)
    }
}
