package com.chenyang.cywms.scanner

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局扫码事件总线：PDA 广播 / 键盘楔入 统一投递到当前页面。
 */
object ScanBus {
    private val _events = MutableSharedFlow<String>(
        extraBufferCapacity = 16,
        replay = 0
    )
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun emit(raw: String) {
        val code = raw.trim()
        if (code.isNotEmpty()) {
            _events.tryEmit(code)
        }
    }
}
