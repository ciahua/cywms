package com.chenyang.cywms.ui.produce

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chenyang.cywms.AppContainer
import com.chenyang.cywms.WmsApp
import com.chenyang.cywms.data.model.Shengchanlingliao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProduceIssueListUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val search: String = "",
    val showCompleted: Boolean = false,
    val orders: List<Shengchanlingliao> = emptyList(),
    val error: String? = null,
    val toast: String? = null
)

class ProduceIssueListViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(ProduceIssueListUiState())
    val ui: StateFlow<ProduceIssueListUiState> = _ui.asStateFlow()

    init {
        refresh(initial = true)
    }

    fun onSearchChange(v: String) = _ui.update { it.copy(search = v) }

    fun toggleShowCompleted() {
        _ui.update { it.copy(showCompleted = !it.showCompleted) }
    }

    fun consumeToast() = _ui.update { it.copy(toast = null) }

    fun search() = refresh(initial = false)

    fun refresh(initial: Boolean = false) {
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    loading = initial && it.orders.isEmpty(),
                    refreshing = !initial || it.orders.isNotEmpty(),
                    error = null
                )
            }
            val q = _ui.value.search.trim().ifBlank { null }
            container.produceIssueRepository.listOrders(q).fold(
                onSuccess = { list ->
                    val sorted = list.sortedWith(
                        compareBy<Shengchanlingliao> { it.status != "0" }
                            .thenByDescending { it.createdt.orEmpty() }
                    )
                    _ui.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            orders = sorted,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _ui.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = e.message ?: "加载失败"
                        )
                    }
                }
            )
        }
    }

    fun visibleOrders(): List<Shengchanlingliao> {
        val s = _ui.value
        return if (s.showCompleted) s.orders
        else s.orders.filter { it.status == "0" || it.status.isNullOrBlank() }
    }

    companion object {
        fun factory(app: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val wms = app as WmsApp
                    return ProduceIssueListViewModel(wms, wms.container) as T
                }
            }
    }
}
