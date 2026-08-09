package com.chenyang.cywms.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.chenyang.cywms.scanner.KeyboardWedge

/**
 * 进入扫码页时启用 Activity 级键盘楔入；离开时关闭。
 * 实际按键在 [com.chenyang.cywms.MainActivity.dispatchKeyEvent] 中处理，不依赖焦点。
 */
@Composable
fun ScanWedgeCatcher(enabled: Boolean = true) {
    val keyboard = LocalSoftwareKeyboardController.current
    DisposableEffect(enabled) {
        if (enabled) {
            KeyboardWedge.enabled = true
            KeyboardWedge.clear()
            keyboard?.hide()
        }
        onDispose {
            KeyboardWedge.enabled = false
            KeyboardWedge.clear()
        }
    }
}
