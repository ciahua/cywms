package com.chenyang.cywms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chenyang.cywms.nav.WmsNavHost
import com.chenyang.cywms.scanner.ScannerHelper
import com.chenyang.cywms.ui.theme.CywmsTheme
import com.chenyang.cywms.ui.theme.Navy900

class MainActivity : ComponentActivity() {
    private var scannerHelper: ScannerHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scannerHelper = ScannerHelper(this) { barcode ->
            // 骨架阶段：后续业务页订阅扫码事件；此处先打系统日志
            android.util.Log.i("CywmsScan", barcode)
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
}
