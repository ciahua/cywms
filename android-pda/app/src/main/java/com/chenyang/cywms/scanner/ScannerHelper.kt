package com.chenyang.cywms.scanner

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

/**
 * 兼容旧 APK 扫码广播：
 * Action = com.service.scanner.data
 * Extra  = ScanCode
 */
class ScannerHelper(
    private val activity: Activity,
    private val onBarcode: (String) -> Unit
) {
    private val action = "com.service.scanner.data"
    private val extraKey = "ScanCode"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val code = intent?.getStringExtra(extraKey)?.trim().orEmpty()
            if (code.isNotEmpty()) onBarcode(code)
        }
    }

    private var registered = false

    fun register() {
        if (registered) return
        val filter = IntentFilter(action)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            activity.registerReceiver(receiver, filter)
        }
        registered = true
    }

    fun unregister() {
        if (!registered) return
        runCatching { activity.unregisterReceiver(receiver) }
        registered = false
    }
}
