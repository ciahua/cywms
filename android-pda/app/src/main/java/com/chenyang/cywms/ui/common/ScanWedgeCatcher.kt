package com.chenyang.cywms.ui.common

import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.chenyang.cywms.scanner.ScanBus

/**
 * 扫码「键盘楔入」捕获器：原生 EditText + showSoftInputOnFocus=false，
 * 可聚焦收枪口按键，但不会弹出软键盘（Compose BasicTextField 做不到这一点）。
 */
@Composable
fun ScanWedgeCatcher(
    modifier: Modifier = Modifier,
    requestFocusOnStart: Boolean = true
) {
    var host by remember { mutableStateOf<EditText?>(null) }

    AndroidView(
        modifier = modifier.size(1.dp),
        factory = { ctx ->
            val buffer = StringBuilder()
            EditText(ctx).apply {
                showSoftInputOnFocus = false
                isCursorVisible = false
                isFocusable = true
                isFocusableInTouchMode = true
                inputType = InputType.TYPE_CLASS_TEXT
                imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                    EditorInfo.IME_FLAG_NO_FULLSCREEN or
                    EditorInfo.IME_ACTION_DONE
                setTextIsSelectable(false)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                alpha = 0f
                setBackgroundColor(0)
                setTextColor(0)
                textSize = 1f
                maxLines = 1
                isSingleLine = true
                // 禁止长按弹出系统编辑菜单
                isLongClickable = false

                fun hideIme() {
                    showSoftInputOnFocus = false
                    val imm = ctx.getSystemService(InputMethodManager::class.java)
                    imm?.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                }

                fun commit() {
                    // 优先用按键缓冲；若枪把字符写进了文本框也一并吃掉
                    val fromText = text?.toString().orEmpty()
                        .replace("\r", "")
                        .replace("\n", "")
                        .trim()
                    val fromKeys = buffer.toString().trim()
                    buffer.clear()
                    setText("")
                    val code = fromText.ifBlank { fromKeys }
                    if (code.isNotEmpty()) {
                        ScanBus.emit(code)
                    }
                    hideIme()
                }

                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) hideIme()
                }

                setOnClickListener { hideIme() }

                setOnEditorActionListener { _, _, _ ->
                    commit()
                    true
                }

                setOnKeyListener { _, keyCode, event ->
                    if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                    when (keyCode) {
                        KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                            commit()
                            true
                        }
                        KeyEvent.KEYCODE_DEL -> {
                            if (buffer.isNotEmpty()) buffer.deleteCharAt(buffer.lastIndex)
                            false
                        }
                        else -> {
                            val ch = event.unicodeChar
                            if (ch != 0 && !Character.isISOControl(ch)) {
                                buffer.append(ch.toChar())
                            }
                            // 不消费，让系统也可写入 EditText，commit 时会清空
                            false
                        }
                    }
                }

                host = this
                post { hideIme() }
            }
        },
        update = { et ->
            et.showSoftInputOnFocus = false
            host = et
        }
    )

    DisposableEffect(requestFocusOnStart, host) {
        val et = host
        if (requestFocusOnStart && et != null) {
            et.post {
                et.showSoftInputOnFocus = false
                et.requestFocus()
                val imm = et.context.getSystemService(InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(et.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
            // 再延迟收一次，防止窗口焦点切换后系统又拉起 IME
            val again = Runnable {
                et.showSoftInputOnFocus = false
                val imm = et.context.getSystemService(InputMethodManager::class.java)
                imm?.hideSoftInputFromWindow(et.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
            }
            et.postDelayed(again, 120)
            et.postDelayed(again, 350)
            onDispose {
                et.removeCallbacks(again)
            }
        } else {
            onDispose { }
        }
    }
}
