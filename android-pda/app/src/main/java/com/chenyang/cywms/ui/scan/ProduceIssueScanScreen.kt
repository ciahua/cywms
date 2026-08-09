package com.chenyang.cywms.ui.scan

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chenyang.cywms.scanner.ScanBus
import com.chenyang.cywms.ui.common.scanInputNoIme
import com.chenyang.cywms.ui.theme.Amber500
import com.chenyang.cywms.ui.theme.Navy700
import com.chenyang.cywms.ui.theme.Navy800
import com.chenyang.cywms.ui.theme.Navy900
import com.chenyang.cywms.ui.theme.Slate200
import com.chenyang.cywms.ui.theme.Slate400
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScanRecord(
    val code: String,
    val time: String,
    val source: String
)

/**
 * 生产领料 · 扫码测试页
 * 验证 PDA 扫码头广播 / 键盘楔入，显示扫码内容。
 */
@Composable
fun ProduceIssueScanScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val records = remember { mutableStateListOf<ScanRecord>() }
    var latest by remember { mutableStateOf<String?>(null) }
    var wedgeBuffer by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    fun appendRecord(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return
        latest = trimmed
        wedgeBuffer = ""
        records.add(
            0,
            ScanRecord(
                code = trimmed,
                time = timeFmt.format(Date()),
                source = "扫码"
            )
        )
        vibrateShort(context)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.hide()
        ScanBus.events.collect { code ->
            appendRecord(code)
        }
    }

    LaunchedEffect(records.size) {
        if (records.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "返回",
                    tint = Slate200
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "生产领料 · 扫码测试",
                    color = Slate200,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "按侧键扫码，结果将显示在下方",
                    color = Slate400,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = {
                    records.clear()
                    latest = null
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = "清空",
                    tint = Slate400
                )
            }
        }

        // 兼容「模拟键盘」：回车结束一枪
        BasicTextField(
            value = wedgeBuffer,
            onValueChange = { wedgeBuffer = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .alpha(0.01f)
                .focusRequester(focusRequester)
                .scanInputNoIme()
                .onPreviewKeyEvent { event ->
                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                        (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER ||
                            event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
                    ) {
                        val buf = wedgeBuffer
                        wedgeBuffer = ""
                        if (buf.isNotBlank()) {
                            ScanBus.emit(buf) // 与广播共用去重，避免一枪两条
                        }
                        true
                    } else {
                        false
                    }
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val buf = wedgeBuffer
                    wedgeBuffer = ""
                    if (buf.isNotBlank()) ScanBus.emit(buf)
                }
            ),
            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
            cursorBrush = SolidColor(Color.Transparent)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Navy800)
                .border(1.dp, Amber500.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = Amber500,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "  最近一次",
                        color = Slate400,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = latest ?: "等待扫码…",
                    color = if (latest == null) Slate400 else Slate200,
                    fontSize = if (latest == null) 16.sp else 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Text(
            text = "扫码记录（${records.size}）",
            color = Slate400,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (records.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("尚未扫到条码", color = Slate400, fontSize = 14.sp)
                    }
                }
            }
            itemsIndexed(records) { index, item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Navy700)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = item.code,
                        color = Slate200,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "#${records.size - index} · ${item.source}",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                        Text(item.time, color = Slate400, fontSize = 11.sp)
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0x22FFFFFF))
        Text(
            text = "广播：com.service.scanner.data / ScanCode\n建议扫描助手仅开广播；广播+键盘同时开时已自动去重",
            color = Slate400.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Suppress("DEPRECATION")
private fun vibrateShort(context: Context) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                v.vibrate(40)
            }
        }
    }
}
