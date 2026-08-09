package com.chenyang.cywms.ui.produce

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chenyang.cywms.scanner.ScanBus
import com.chenyang.cywms.ui.common.PdaScaffold
import com.chenyang.cywms.ui.common.ScanWedgeCatcher
import com.chenyang.cywms.ui.theme.Amber500
import com.chenyang.cywms.ui.theme.DangerRed
import com.chenyang.cywms.ui.theme.Navy700
import com.chenyang.cywms.ui.theme.Navy800
import com.chenyang.cywms.ui.theme.Navy900
import com.chenyang.cywms.ui.theme.Slate200
import com.chenyang.cywms.ui.theme.Slate400
import com.chenyang.cywms.ui.theme.SuccessGreen
import kotlinx.coroutines.delay

@Composable
fun ProduceIssueScanRoute(
    viewModel: ProduceIssueScanViewModel,
    onBack: () -> Unit,
    onSubmittedAndDone: () -> Unit
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()
    var toastText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        ScanBus.events.collect { code ->
            viewModel.onBarcode(code)
        }
    }
    LaunchedEffect(state.toast) {
        val t = state.toast
        if (!t.isNullOrBlank()) {
            toastText = t
            delay(2000)
            viewModel.consumeToast()
        }
    }
    LaunchedEffect(state.submitted) {
        if (state.submitted) {
            delay(400)
            onSubmittedAndDone()
        }
    }

    ProduceIssueScanScreen(
        state = state,
        toastText = if (state.toast != null) toastText else "",
        onBack = onBack,
        onUndo = viewModel::removeLast,
        onClear = viewModel::clearLines,
        onSubmitContinue = { viewModel.submit(markLineDone = false) },
        onSubmitDone = { viewModel.submit(markLineDone = true) }
    )
}

@Composable
fun ProduceIssueScanScreen(
    state: ProduceIssueScanUiState,
    toastText: String,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSubmitContinue: () -> Unit,
    onSubmitDone: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.lines.size) {
        if (state.lines.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    ScanWedgeCatcher()

    PdaScaffold(
        title = state.matcode.ifBlank { "扫码领料" },
        subtitle = state.orderNo,
        onBack = onBack,
        actions = {
            IconButton(onClick = onUndo, enabled = state.lines.isNotEmpty() && !state.submitting) {
                Icon(Icons.Outlined.Undo, contentDescription = "撤销", tint = Slate400)
            }
            IconButton(onClick = onClear, enabled = state.lines.isNotEmpty() && !state.submitting) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = "清空", tint = Slate400)
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Navy800)
                .border(1.dp, Amber500.copy(0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Text(
                state.matname.ifBlank { "—" },
                color = Slate200,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state.matspec.isNotBlank()) {
                Text(
                    state.matspec,
                    color = Slate400,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "需领 ${state.needQty}  ·  已领 ${state.alreadyQty}  ·  本场 ${formatSession(state)}  ·  剩余 ${state.remaining}",
                color = Slate200,
                fontSize = 12.sp
            )
            val status = when {
                state.validating -> "校验中…"
                state.submitting -> "提交中…"
                state.error != null -> state.error
                state.tip != null -> state.tip
                else -> "请按侧键扫码"
            }
            Text(
                status.orEmpty(),
                color = when {
                    state.error != null -> DangerRed
                    state.validating || state.submitting -> Amber500
                    else -> Slate400
                },
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Text(
            "本场扫码（${state.lines.size}）",
            color = Slate400,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            if (state.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Amber500,
                    strokeWidth = 2.dp
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    reverseLayout = false
                ) {
                    if (state.lines.isEmpty()) {
                        item {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("等待扫码…", color = Slate400, fontSize = 14.sp)
                            }
                        }
                    }
                    itemsIndexed(state.lines.asReversed()) { index, line ->
                        val no = state.lines.size - index
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Navy700)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "#$no  +${line.scanQty}  余${line.restQty}",
                                color = SuccessGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                line.barcode,
                                color = Slate200,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            if (toastText.isNotBlank()) {
                Text(
                    toastText,
                    color = Amber500,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Navy900.copy(alpha = 0.92f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onSubmitContinue,
                enabled = state.lines.isNotEmpty() && !state.submitting && !state.validating,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200)
            ) {
                Text("提交继续", fontSize = 13.sp)
            }
            Button(
                onClick = onSubmitDone,
                enabled = state.lines.isNotEmpty() && !state.submitting && !state.validating,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Amber500,
                    contentColor = Navy900
                )
            ) {
                if (state.submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Navy900
                    )
                } else {
                    Text("提交完成", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

private fun formatSession(state: ProduceIssueScanUiState): String {
    val q = state.sessionQty
    return if (q % 1.0 == 0.0) q.toLong().toString() else q.toString()
}
