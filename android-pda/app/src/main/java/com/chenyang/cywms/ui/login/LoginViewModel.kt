package com.chenyang.cywms.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chenyang.cywms.AppContainer
import com.chenyang.cywms.data.api.LoginOutcome
import com.chenyang.cywms.data.prefs.SessionPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val host: String = "172.16.5.7",
    val port: String = "8080",
    val useHttps: Boolean = false,
    val proxyUrl: String = SessionPrefs.DEFAULT_PROXY_URL,
    val username: String = "",
    val password: String = "",
    val rememberAccount: Boolean = true,
    val advancedOpen: Boolean = false,
    val loading: Boolean = false,
    val testing: Boolean = false,
    val error: String? = null,
    val testMessage: String? = null,
    val loggedIn: Boolean = false,
    val displayName: String = "",
    val ready: Boolean = false
)

class LoginViewModel(
    private val container: AppContainer
) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val snap = container.prefs.snapshotFlow.first()
            _ui.update {
                it.copy(
                    host = snap.host,
                    port = snap.port,
                    useHttps = snap.useHttps,
                    proxyUrl = snap.proxyUrl,
                    username = snap.username,
                    password = if (snap.rememberAccount) snap.password else "",
                    rememberAccount = snap.rememberAccount,
                    loggedIn = snap.token.isNotBlank(),
                    displayName = snap.realName.ifBlank { snap.username },
                    ready = true
                )
            }
        }
    }

    fun onHost(v: String) = _ui.update { it.copy(host = v, error = null) }
    fun onPort(v: String) = _ui.update { it.copy(port = v, error = null) }
    fun onUseHttps(v: Boolean) = _ui.update { it.copy(useHttps = v) }
    fun onProxy(v: String) = _ui.update { it.copy(proxyUrl = v, error = null) }
    fun onUsername(v: String) = _ui.update { it.copy(username = v, error = null) }
    fun onPassword(v: String) = _ui.update { it.copy(password = v, error = null) }
    fun onRemember(v: Boolean) = _ui.update { it.copy(rememberAccount = v) }
    fun toggleAdvanced() = _ui.update { it.copy(advancedOpen = !it.advancedOpen) }

    fun testConnection() {
        val s = _ui.value
        viewModelScope.launch {
            _ui.update { it.copy(testing = true, testMessage = null, error = null) }
            container.prefs.saveConnection(s.host, s.port, s.useHttps, s.proxyUrl)
            val result = container.apiClient.testConnection()
            _ui.update {
                it.copy(
                    testing = false,
                    testMessage = result.getOrElse { e -> "失败：${e.message}" }
                )
            }
        }
    }

    fun login() {
        val s = _ui.value
        if (s.username.isBlank() || s.password.isBlank()) {
            _ui.update { it.copy(error = "请输入账号和密码") }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(loading = true, error = null, testMessage = null) }
            when (
                val outcome = container.authRepository.login(
                    username = s.username,
                    password = s.password,
                    host = s.host,
                    port = s.port,
                    useHttps = s.useHttps,
                    proxyUrl = s.proxyUrl,
                    rememberAccount = s.rememberAccount
                )
            ) {
                is LoginOutcome.Success -> _ui.update {
                    it.copy(
                        loading = false,
                        loggedIn = true,
                        displayName = outcome.realName,
                        error = null
                    )
                }
                is LoginOutcome.Failure -> _ui.update {
                    it.copy(loading = false, error = outcome.message)
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(container) as T
            }
        }
    }
}
