package com.chenyang.cywms.scanner

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

/**
 * 海康 / 旧 App 兼容扫码广播。
 *
 * 旧 APK 确认：
 * - Action = com.service.scanner.data
 * - Extra  = ScanCode
 *
 * 设备「扫描助手」需设为广播输出模式。
 */
class ScannerHelper(
    private val activity: Activity,
    private val onBarcode: (String) -> Unit = { ScanBus.emit(it) }
) {
    companion object {
        private const val TAG = "CywmsScan"

        /** 海康常见默认；可按扫描助手配置增补 */
        val ACTIONS = listOf(
            "com.service.scanner.data",
            "android.intent.ACTION_DECODE_DATA",
            "com.hikrobot.scanner.ACTION",
            "com.scanner.broadcast"
        )

        val EXTRA_KEYS = listOf(
            "ScanCode",
            "barcode",
            "barocode", // 个别固件拼写
            "data",
            "scannerdata",
            "decode_data",
            "DECODE_DATA",
            "scan_result",
            "SCAN_RESULT"
        )
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val code = extractBarcode(intent)
            if (code.isNotEmpty()) {
                Log.i(TAG, "broadcast action=${intent.action} code=$code")
                onBarcode(code)
            } else {
                Log.w(TAG, "empty barcode extras=${intent.extras?.keySet()}")
            }
        }
    }

    private var registered = false

    fun register() {
        if (registered) return
        val filter = IntentFilter().apply {
            ACTIONS.forEach { addAction(it) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            activity.registerReceiver(receiver, filter)
        }
        registered = true
        Log.i(TAG, "scanner receiver registered")
    }

    fun unregister() {
        if (!registered) return
        runCatching { activity.unregisterReceiver(receiver) }
        registered = false
        Log.i(TAG, "scanner receiver unregistered")
    }

    private fun extractBarcode(intent: Intent): String {
        for (key in EXTRA_KEYS) {
            val v = intent.getStringExtra(key)?.trim().orEmpty()
            if (v.isNotEmpty()) return v
        }
        // 个别设备用 byte[]
        for (key in EXTRA_KEYS) {
            val bytes = intent.getByteArrayExtra(key)
            if (bytes != null && bytes.isNotEmpty()) {
                return String(bytes).trim()
            }
        }
        return ""
    }
}
