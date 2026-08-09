package com.chenyang.cywms.scanner

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent

/**
 * Activity 级键盘楔入拦截：不依赖 Compose/EditText 焦点，避免「有按键音但没入账」。
 *
 * 扫码页开启 [enabled]；登录页关闭，以免吞掉账号密码按键。
 * 结束符：Enter / Tab；若无结束符，则在最后一键后 [idleCommitMs] 空闲自动提交。
 */
object KeyboardWedge {
    private const val TAG = "CywmsScan"
    /** 枪口字符间隔通常 < 30ms；略放宽以兼容慢速楔入 */
    private const val IDLE_COMMIT_MS = 80L
    private const val MAX_LEN = 256

    @Volatile
    var enabled: Boolean = false

    private val buffer = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())
    private var lastCharAt = 0L

    private val idleFlush = Runnable {
        if (!enabled) return@Runnable
        val idle = SystemClock.uptimeMillis() - lastCharAt
        if (idle >= IDLE_COMMIT_MS) {
            flush("idle")
        }
    }

    /**
     * @return true 表示已消费该按键（调用方勿再分发）
     */
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!enabled) return false

        // 部分固件一次塞入整串
        if (event.action == KeyEvent.ACTION_MULTIPLE) {
            val chars = event.characters
            if (!chars.isNullOrEmpty()) {
                appendRaw(chars)
                return true
            }
        }

        if (event.action != KeyEvent.ACTION_DOWN) return false

        when (event.keyCode) {
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_TAB -> {
                flush("terminator")
                return true
            }
            KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL -> {
                if (buffer.isNotEmpty()) {
                    buffer.deleteCharAt(buffer.lastIndex)
                    scheduleIdle()
                    return true
                }
                return false
            }
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_ALT_RIGHT,
            KeyEvent.KEYCODE_CTRL_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_CAPS_LOCK,
            KeyEvent.KEYCODE_UNKNOWN -> return false
        }

        val ch = event.unicodeChar
        if (ch != 0 && !Character.isISOControl(ch)) {
            appendChar(ch.toChar())
            return true
        }
        return false
    }

    fun clear() {
        handler.removeCallbacks(idleFlush)
        synchronized(buffer) { buffer.clear() }
    }

    private fun appendRaw(chars: String) {
        val cleaned = chars.replace("\r", "").replace("\n", "").replace("\t", "")
        if (cleaned.isEmpty()) {
            if (chars.contains('\n') || chars.contains('\r') || chars.contains('\t')) {
                flush("multi-term")
            }
            return
        }
        synchronized(buffer) {
            if (buffer.length + cleaned.length > MAX_LEN) buffer.clear()
            buffer.append(cleaned)
        }
        // 串里若自带结束符，上面已剥掉；整包到达后短空闲即提交
        scheduleIdle()
        if (chars.contains('\n') || chars.contains('\r') || chars.contains('\t')) {
            flush("multi-term")
        }
    }

    private fun appendChar(ch: Char) {
        if (ch == '\n' || ch == '\r' || ch == '\t') {
            flush("char-term")
            return
        }
        synchronized(buffer) {
            if (buffer.length >= MAX_LEN) buffer.clear()
            buffer.append(ch)
        }
        scheduleIdle()
    }

    private fun scheduleIdle() {
        lastCharAt = SystemClock.uptimeMillis()
        handler.removeCallbacks(idleFlush)
        handler.postDelayed(idleFlush, IDLE_COMMIT_MS)
    }

    private fun flush(reason: String) {
        handler.removeCallbacks(idleFlush)
        val code = synchronized(buffer) {
            val s = buffer.toString().trim()
            buffer.clear()
            s
        }
        if (code.isEmpty()) return
        Log.i(TAG, "wedge flush reason=$reason code=$code")
        ScanBus.emit(code)
    }
}
