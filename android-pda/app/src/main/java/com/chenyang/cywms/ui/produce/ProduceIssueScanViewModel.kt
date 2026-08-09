package com.chenyang.cywms.ui.produce

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chenyang.cywms.AppContainer
import com.chenyang.cywms.WmsApp
import com.chenyang.cywms.data.model.BarcodeParse
import com.chenyang.cywms.data.model.ProduceMaterialBarcodePda
import com.chenyang.cywms.data.model.ScannedBarcodeLine
import com.chenyang.cywms.data.model.asQty
import com.chenyang.cywms.data.model.formatQty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ProduceIssueScanUiState(
    val mainId: String = "",
    val detailId: String = "",
    val orderNo: String = "",
    val matcode: String = "",
    val matname: String = "",
    val matspec: String = "",
    val unit: String = "",
    val needQty: String = "0",
    val alreadyQty: String = "0",
    val loading: Boolean = true,
    val validating: Boolean = false,
    val submitting: Boolean = false,
    val lines: List<ScannedBarcodeLine> = emptyList(),
    val tip: String? = null,
    val error: String? = null,
    val submitted: Boolean = false,
    val toast: String? = null
) {
    val sessionQty: Double get() = lines.sumOf { it.scanQty.asQty() }
    val remaining: Double
        get() = (needQty.asQty() - alreadyQty.asQty() - sessionQty).coerceAtLeast(0.0)
}

class ProduceIssueScanViewModel(
    application: Application,
    private val container: AppContainer,
    mainId: String,
    detailId: String,
    orderNo: String,
    matcode: String,
    matname: String,
    matspec: String,
    unit: String,
    needQty: String
) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(
        ProduceIssueScanUiState(
            mainId = mainId,
            detailId = detailId,
            orderNo = orderNo,
            matcode = matcode,
            matname = matname,
            matspec = matspec,
            unit = unit,
            needQty = needQty
        )
    )
    val ui: StateFlow<ProduceIssueScanUiState> = _ui.asStateFlow()

    private val scanMutex = Mutex()

    init {
        viewModelScope.launch {
            val already = container.produceIssueRepository.getScanQty(detailId)
                .getOrDefault("0")
            _ui.update {
                it.copy(loading = false, alreadyQty = already)
            }
        }
    }

    fun consumeToast() = _ui.update { it.copy(toast = null) }

    fun clearTip() = _ui.update { it.copy(tip = null, error = null) }

    fun removeLast() {
        _ui.update {
            if (it.lines.isEmpty()) it
            else it.copy(lines = it.lines.dropLast(1), tip = "已移除最后一枪")
        }
    }

    fun clearLines() {
        _ui.update { it.copy(lines = emptyList(), tip = "已清空本场扫码") }
    }

    fun onBarcode(raw: String) {
        viewModelScope.launch {
            scanMutex.withLock {
                handleBarcode(raw)
            }
        }
    }

    private suspend fun handleBarcode(raw: String) {
        val s = _ui.value
        if (s.loading || s.submitting || s.submitted) return
        val parsed = BarcodeParse.parse(raw) ?: return
        val code = parsed.raw

        if (s.lines.any { it.barcode.equals(code, ignoreCase = true) }) {
            reject("重复扫码：$code")
            return
        }
        if (parsed.matcode.isNotBlank() &&
            s.matcode.isNotBlank() &&
            !parsed.matcode.equals(s.matcode, ignoreCase = true)
        ) {
            // 仍以服务端 verify 为准，先本地提示加速反馈
            // 不直接 return
        }

        val remain = s.remaining
        if (remain <= 0) {
            reject("本行已领满，无需继续扫码")
            return
        }

        _ui.update { it.copy(validating = true, tip = "校验中…", error = null) }

        val verified = container.produceIssueRepository.verify(code, s.detailId)
            .getOrElse { e ->
                reject(e.message ?: "校验失败")
                return
            }
        if (!verified) {
            reject("条码与物料不匹配或无效")
            return
        }

        val restStr = container.produceIssueRepository.getRestQty(code)
            .getOrElse { e ->
                reject(e.message ?: "余量查询失败")
                return
            }
        val rest = restStr.asQty()
        if (rest <= 0) {
            reject("条码余量不足（$restStr）")
            return
        }

        var scanQty = parsed.qty.asQty()
        if (scanQty <= 0) scanQty = rest
        if (scanQty > rest) scanQty = rest
        if (scanQty > remain) scanQty = remain

        val fifoOk = container.produceIssueRepository.checkFifo(
            barcode = code,
            matcode = s.matcode.ifBlank { parsed.matcode },
            period = parsed.period,
            requestQty = formatQty(scanQty)
        ).getOrDefault(true)
        if (!fifoOk) {
            reject("FIFO 校验未通过，请先扫较早批次")
            return
        }

        val line = ScannedBarcodeLine(
            barcode = code,
            scanQty = formatQty(scanQty),
            restQty = restStr,
            period = parsed.period,
            matcodeFromBarcode = parsed.matcode
        )
        _ui.update {
            it.copy(
                validating = false,
                lines = it.lines + line,
                tip = "已扫 +${line.scanQty}（余 ${it.remaining - scanQty}）",
                error = null
            )
        }
        vibrateShort(getApplication())
    }

    private fun reject(msg: String) {
        _ui.update {
            it.copy(validating = false, error = msg, tip = null, toast = msg)
        }
    }

    fun submit(markLineDone: Boolean) {
        viewModelScope.launch {
            val s = _ui.value
            if (s.submitting || s.submitted) return@launch
            if (s.lines.isEmpty()) {
                _ui.update { it.copy(toast = "请先扫码") }
                return@launch
            }
            _ui.update { it.copy(submitting = true, error = null, tip = "提交中…") }
            val username = container.produceIssueRepository.currentUsername()
            val body = ProduceMaterialBarcodePda(
                fkOutsourcedetail = s.detailId,
                deliveryno = s.mainId,
                scanner = username,
                needqty = s.needQty,
                barcodelist = s.lines.map { it.barcode },
                scanqtyList = s.lines.map { it.scanQty },
                restqtyList = s.lines.map { it.restQty }
            )
            val submitResult = container.produceIssueRepository.submitPda(body)
            if (submitResult.isFailure) {
                _ui.update {
                    it.copy(
                        submitting = false,
                        tip = null,
                        error = submitResult.exceptionOrNull()?.message ?: "提交失败",
                        toast = submitResult.exceptionOrNull()?.message ?: "提交失败"
                    )
                }
                return@launch
            }

            if (markLineDone) {
                container.produceIssueRepository.changeLineStatus(s.detailId)
                // 主单是否全部完成：尽力触发，失败不阻断
                container.produceIssueRepository.changeMainStatus(s.mainId)
            } else {
                // 刷新已扫数量
                val already = container.produceIssueRepository.getScanQty(s.detailId)
                    .getOrDefault(s.alreadyQty)
                _ui.update {
                    it.copy(
                        submitting = false,
                        submitted = false,
                        lines = emptyList(),
                        alreadyQty = already,
                        tip = "提交成功，可继续扫码",
                        toast = "提交成功"
                    )
                }
                return@launch
            }

            _ui.update {
                it.copy(
                    submitting = false,
                    submitted = true,
                    tip = "提交成功",
                    toast = "提交成功",
                    lines = emptyList()
                )
            }
        }
    }

    companion object {
        fun factory(
            app: Application,
            mainId: String,
            detailId: String,
            orderNo: String,
            matcode: String,
            matname: String,
            matspec: String,
            unit: String,
            needQty: String
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val wms = app as WmsApp
                    return ProduceIssueScanViewModel(
                        wms, wms.container,
                        mainId, detailId, orderNo,
                        matcode, matname, matspec, unit, needQty
                    ) as T
                }
            }
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
