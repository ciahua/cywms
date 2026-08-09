package com.chenyang.cywms.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chenyang.cywms.AppContainer
import com.chenyang.cywms.WmsApp
import com.chenyang.cywms.data.api.DownloadEvent
import com.chenyang.cywms.data.api.LoginOutcome
import com.chenyang.cywms.data.api.UpdateCheckResult
import com.chenyang.cywms.data.api.VersionCompare
import com.chenyang.cywms.data.prefs.SessionPrefs
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

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
    val ready: Boolean = false,
    val localVersion: String = VersionCompare.localVersion(),
    val checkingUpdate: Boolean = false,
    val updateMessage: String? = null,
    val updateAvailable: Boolean = false,
    val remoteVersion: String? = null,
    val remoteRemark: String? = null,
    val downloadUrl: String? = null,
    val downloading: Boolean = false,
    val downloadPercent: Int = -1,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val apkReady: File? = null,
    val installMessage: String? = null,
    /** 发现新版本后弹出确认下载对话框 */
    val showUpdateDialog: Boolean = false,
    /** 点击更新后「已是最新 / 检查失败」等短暂提示（自动消失） */
    val showToast: Boolean = false,
    val toastMessage: String = ""
)

class LoginViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(LoginUiState())
    val ui: StateFlow<LoginUiState> = _ui.asStateFlow()

    private var downloadJob: Job? = null

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
                    ready = true,
                    localVersion = VersionCompare.localVersion()
                )
            }
            // 进入登录页静默检查（仅角标，不弹窗、不下载）
            checkUpdate(promptUser = false)
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

    /** 点击底部更新图标：有新版本则进入下载；无新版本短暂提示 */
    fun onUpdateIconClick() {
        val s = _ui.value
        when {
            s.downloading -> return
            s.checkingUpdate -> {
                // 检查中再点：给轻提示，避免「没反应」
                _ui.update {
                    it.copy(showToast = true, toastMessage = "正在检查更新…")
                }
            }
            s.apkReady != null -> installDownloaded()
            // 静默检查已发现新版本：直接进入下载
            s.updateAvailable && !s.downloadUrl.isNullOrBlank() -> startDownload()
            else -> checkUpdate(promptUser = true)
        }
    }

    fun dismissUpdateDialog() {
        _ui.update {
            it.copy(
                showUpdateDialog = false,
                updateMessage = "已取消下载，继续使用 ${it.localVersion}"
            )
        }
    }

    fun confirmDownload() {
        _ui.update { it.copy(showUpdateDialog = false) }
        startDownload()
    }

    fun dismissToast() {
        _ui.update { it.copy(showToast = false, toastMessage = "") }
    }

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

    /**
     * @param promptUser true=用户点击：无新版本提示；有新版本直接开始下载。
     *                   false=启动静默检查：仅角标，不提示、不下载。
     */
    fun checkUpdate(promptUser: Boolean = false) {
        if (_ui.value.downloading) return
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    // 静默检查不占用按钮 busy，避免启动期间点击被吞掉
                    checkingUpdate = promptUser,
                    updateMessage = null,
                    installMessage = null,
                    error = null,
                    showUpdateDialog = false,
                    showToast = false,
                    toastMessage = ""
                )
            }
            when (val result = container.updateRepository.checkUpdate()) {
                is UpdateCheckResult.UpToDate -> {
                    val msg = "当前没有新版本（${result.localVersion}）"
                    _ui.update {
                        it.copy(
                            checkingUpdate = false,
                            updateAvailable = false,
                            remoteVersion = result.remoteVersion,
                            downloadUrl = null,
                            apkReady = null,
                            remoteRemark = null,
                            updateMessage = msg,
                            showToast = promptUser,
                            toastMessage = if (promptUser) msg else ""
                        )
                    }
                }
                is UpdateCheckResult.Available -> {
                    val remark = listOfNotNull(
                        result.remote.name,
                        result.remote.body?.lineSequence()?.firstOrNull {
                            it.isNotBlank() && !it.startsWith("VERSION=", ignoreCase = true)
                        }
                    ).distinct().joinToString("\n").ifBlank { null }
                    _ui.update {
                        it.copy(
                            checkingUpdate = false,
                            updateAvailable = true,
                            remoteVersion = result.remote.version,
                            remoteRemark = remark,
                            downloadUrl = result.remote.downloadUrl,
                            apkReady = null,
                            updateMessage = "发现新版本 ${result.remote.version}",
                            showUpdateDialog = false,
                            showToast = false
                        )
                    }
                    // 用户主动点击：有新版本直接进入下载状态
                    if (promptUser) startDownload()
                }
                is UpdateCheckResult.Failed -> {
                    val msg = "检查更新失败：${result.message}"
                    _ui.update {
                        it.copy(
                            checkingUpdate = false,
                            updateAvailable = false,
                            updateMessage = msg,
                            showToast = promptUser,
                            toastMessage = if (promptUser) msg else ""
                        )
                    }
                }
            }
        }
    }

    fun startDownload() {
        val url = _ui.value.downloadUrl ?: return
        val ver = _ui.value.remoteVersion ?: "update"
        if (_ui.value.downloading) return
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _ui.update {
                it.copy(
                    downloading = true,
                    downloadPercent = 0,
                    downloadedBytes = 0,
                    totalBytes = 0,
                    apkReady = null,
                    installMessage = null,
                    updateMessage = "正在下载 $ver …"
                )
            }
            container.updateRepository.downloadApk(url, ver).collect { event ->
                when (event) {
                    is DownloadEvent.Progress -> _ui.update {
                        it.copy(
                            downloadPercent = event.percent,
                            downloadedBytes = event.downloaded,
                            totalBytes = event.total,
                            updateMessage = if (event.percent >= 0) {
                                "下载中 ${event.percent}%"
                            } else {
                                "下载中 ${event.downloaded / (1024 * 1024)} MB"
                            }
                        )
                    }
                    is DownloadEvent.Success -> _ui.update {
                        it.copy(
                            downloading = false,
                            downloadPercent = 100,
                            apkReady = event.apkFile,
                            updateMessage = "下载完成，正在安装…"
                        )
                    }
                    is DownloadEvent.Failed -> _ui.update {
                        it.copy(
                            downloading = false,
                            updateMessage = "下载失败：${event.message}",
                            showToast = true,
                            toastMessage = "下载失败：${event.message}"
                        )
                    }
                }
            }
        }
    }

    fun installDownloaded() {
        val file = _ui.value.apkReady ?: return
        val outcome = container.updateRepository.installApk(getApplication(), file)
        _ui.update {
            it.copy(
                installMessage = outcome.fold(
                    onSuccess = { "已调起安装界面" },
                    onFailure = { e -> e.message ?: "无法安装" }
                )
            )
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
        fun factory(app: WmsApp) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LoginViewModel(app, app.container) as T
            }
        }
    }
}
