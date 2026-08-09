package com.chenyang.cywms

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chenyang.cywms.nav.WmsNavHost
import com.chenyang.cywms.scanner.KeyboardWedge
import com.chenyang.cywms.scanner.ScanBus
import com.chenyang.cywms.scanner.ScannerHelper
import com.chenyang.cywms.ui.theme.CywmsTheme
import com.chenyang.cywms.ui.theme.Navy900

class MainActivity : ComponentActivity() {
    private var scannerHelper: ScannerHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scannerHelper = ScannerHelper(this) { barcode ->
            ScanBus.emit(barcode)
        }
        setContent {
            CywmsTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Navy900) {
                    WmsNavHost()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        scannerHelper?.register()
    }

    override fun onPause() {
        scannerHelper?.unregister()
        super.onPause()
    }

    /**
     * 在窗口最前端拦截扫码枪键盘楔入，不依赖当前焦点控件，避免漏码。
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (KeyboardWedge.onKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}
