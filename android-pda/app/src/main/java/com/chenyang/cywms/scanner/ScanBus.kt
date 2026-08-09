package com.chenyang.cywms.scanner

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局扫码事件总线：PDA 广播 / 键盘楔入 统一投递到当前页面。
 *
 * 海康等设备常在「广播」模式下仍附带键盘楔入，一次扫描会同时走两条通路；
 * 这里对相同条码做短时去重，避免一枪出两条记录。
 */
object ScanBus {
    private const val TAG = "CywmsScan"
    /** 同一条码在此窗口内只投递一次（广播+键盘几乎同时到达） */
    private const val DEDUP_WINDOW_MS = 500L

    private val _events = MutableSharedFlow<String>(
        extraBufferCapacity = 64,
        replay = 0
    )
    val events: SharedFlow<String> = _events.asSharedFlow()

    @Volatile
    private var lastCode: String = ""
    @Volatile
    private var lastAtMs: Long = 0L

    fun emit(raw: String): Boolean {
        val code = raw.trim()
        if (code.isEmpty()) return false
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (code == lastCode && now - lastAtMs < DEDUP_WINDOW_MS) {
                Log.i(TAG, "dedup skip code=$code within ${now - lastAtMs}ms")
                return false
            }
            lastCode = code
            lastAtMs = now
        }
        return _events.tryEmit(code)
    }

    /** 供页面在本地键盘路径调用：与广播共用同一去重窗口 */
    fun tryAccept(raw: String): Boolean = emit(raw)
}
