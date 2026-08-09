package com.chenyang.cywms.ui.produce

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chenyang.cywms.AppContainer
import com.chenyang.cywms.WmsApp
import com.chenyang.cywms.data.model.ShengchanlingliaoDetail
import com.chenyang.cywms.data.model.asQty
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailLineUi(
    val detail: ShengchanlingliaoDetail,
    val scannedQty: String = "0"
)

data class ProduceIssueDetailUiState(
    val mainId: String = "",
    val orderNo: String = "",
    val loading: Boolean = true,
    val lines: List<DetailLineUi> = emptyList(),
    val error: String? = null,
    val toast: String? = null
)

class ProduceIssueDetailViewModel(
    application: Application,
    private val container: AppContainer,
    mainId: String,
    orderNo: String
) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(
        ProduceIssueDetailUiState(mainId = mainId, orderNo = orderNo)
    )
    val ui: StateFlow<ProduceIssueDetailUiState> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun consumeToast() = _ui.update { it.copy(toast = null) }

    fun refresh() {
        val mainId = _ui.value.mainId
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null) }
            container.produceIssueRepository.listDetails(mainId).fold(
                onSuccess = { details ->
                    val enriched = coroutineScope {
                        details.map { d ->
                            async {
                                val id = d.id.orEmpty()
                                val qty = if (id.isBlank()) "0"
                                else container.produceIssueRepository.getScanQty(id)
                                    .getOrDefault(d.actqty ?: "0")
                                DetailLineUi(detail = d, scannedQty = qty)
                            }
                        }.awaitAll()
                    }.sortedWith(
                        compareBy<DetailLineUi> { it.detail.status != "0" }
                            .thenBy { it.detail.matcode.orEmpty() }
                    )
                    _ui.update {
                        it.copy(loading = false, lines = enriched, error = null)
                    }
                },
                onFailure = { e ->
                    _ui.update {
                        it.copy(loading = false, error = e.message ?: "加载明细失败")
                    }
                }
            )
        }
    }

    fun remaining(line: DetailLineUi): Double {
        val need = line.detail.reqqty.asQty()
        val got = line.scannedQty.asQty()
        return (need - got).coerceAtLeast(0.0)
    }

    companion object {
        fun factory(
            app: Application,
            mainId: String,
            orderNo: String
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val wms = app as WmsApp
                    return ProduceIssueDetailViewModel(
                        wms, wms.container, mainId, orderNo
                    ) as T
                }
            }
    }
}
