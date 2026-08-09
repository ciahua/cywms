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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chenyang.cywms.data.model.formatQty
import com.chenyang.cywms.ui.common.PdaScaffold
import com.chenyang.cywms.ui.theme.Amber500
import com.chenyang.cywms.ui.theme.Navy700
import com.chenyang.cywms.ui.theme.Slate200
import com.chenyang.cywms.ui.theme.Slate400
import com.chenyang.cywms.ui.theme.SuccessGreen

@Composable
fun ProduceIssueDetailRoute(
    viewModel: ProduceIssueDetailViewModel,
    onBack: () -> Unit,
    onScanLine: (
        detailId: String,
        matcode: String,
        matname: String,
        matspec: String,
        unit: String,
        needQty: String
    ) -> Unit
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    ProduceIssueDetailScreen(
        state = state,
        remainingOf = viewModel::remaining,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onScanLine = onScanLine
    )
}

@Composable
fun ProduceIssueDetailScreen(
    state: ProduceIssueDetailUiState,
    remainingOf: (DetailLineUi) -> Double,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onScanLine: (
        detailId: String,
        matcode: String,
        matname: String,
        matspec: String,
        unit: String,
        needQty: String
    ) -> Unit
) {
    PdaScaffold(
        title = state.orderNo.ifBlank { "领料明细" },
        subtitle = "共 ${state.lines.size} 行",
        onBack = onBack,
        actions = {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = "刷新", tint = Slate400)
            }
        }
    ) {
        when {
            state.loading -> FullScreenLoading()
            state.error != null && state.lines.isEmpty() -> FullScreenMessage(state.error ?: "")
            state.lines.isEmpty() -> FullScreenMessage("本单无明细（或当前账号无权限）")
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.lines, key = { it.detail.id.orEmpty() }) { line ->
                        val done = line.detail.status == "1" || remainingOf(line) <= 0
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Navy700)
                                .border(
                                    1.dp,
                                    if (done) Slate400.copy(0.2f) else Amber500.copy(0.35f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !done) {
                                    val d = line.detail
                                    onScanLine(
                                        d.id.orEmpty(),
                                        d.matcode.orEmpty(),
                                        d.matname.orEmpty(),
                                        d.matspec.orEmpty(),
                                        d.unit.orEmpty(),
                                        d.reqqty.orEmpty()
                                    )
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    line.detail.matcode ?: "—",
                                    color = Slate200,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    if (done) "已领完" else "去扫码",
                                    color = if (done) SuccessGreen else Amber500,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                line.detail.matname ?: "",
                                color = Slate400,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (!line.detail.matspec.isNullOrBlank()) {
                                Text(
                                    line.detail.matspec!!,
                                    color = Slate400.copy(0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "需领 ${line.detail.reqqty ?: "0"}  ·  已领 ${line.scannedQty}  ·  剩余 ${formatQty(remainingOf(line))} ${line.detail.unit.orEmpty()}",
                                color = Slate200,
                                fontSize = 12.sp
                            )
                            if (!line.detail.warehouse.isNullOrBlank()) {
                                Text(
                                    "仓 ${line.detail.warehouse}",
                                    color = Slate400,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
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
