package com.chenyang.cywms.data.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.chenyang.cywms.BuildConfig
import com.chenyang.cywms.data.model.ApiResult
import com.chenyang.cywms.data.model.FileDownloadRecord
import com.chenyang.cywms.data.model.PageResult
import com.chenyang.cywms.data.prefs.SessionPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

interface UpdateApi {
    @GET("jeecg-boot/filedownload/filedownload/list1")
    suspend fun listVersions(
        @Query("pageNo") pageNo: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("column") column: String = "createTime",
        @Query("order") order: String = "desc"
    ): ApiResult<PageResult<FileDownloadRecord>>
}

sealed class UpdateCheckResult {
    data class UpToDate(val localVersion: String, val remoteVersion: String?) : UpdateCheckResult()
    data class Available(
        val localVersion: String,
        val remote: FileDownloadRecord,
        val downloadUrl: String
    ) : UpdateCheckResult()
    data class Failed(val message: String) : UpdateCheckResult()
}

sealed class DownloadEvent {
    data class Progress(val percent: Int, val downloaded: Long, val total: Long) : DownloadEvent()
    data class Success(val apkFile: File) : DownloadEvent()
    data class Failed(val message: String) : DownloadEvent()
}

object VersionCompare {
    fun localVersion(): String = BuildConfig.VERSION_NAME

    /** 从 v1.0.0_20251117.1 提取可比较键：YYYYMMDD + 序号 */
    fun sortKey(version: String): String {
        val m = Regex("""(\d{8})(?:\.(\d+))?""").findAll(version).lastOrNull()
        return if (m != null) {
            val day = m.groupValues[1]
            val rev = m.groupValues.getOrNull(2)?.ifBlank { null } ?: "0"
            "$day.${rev.padStart(4, '0')}"
        } else {
            version.trim().lowercase()
        }
    }

    fun isRemoteNewer(remote: String, local: String): Boolean {
        if (remote.isBlank()) return false
        if (local.isBlank()) return true
        if (remote.trim().equals(local.trim(), ignoreCase = true)) return false
        return sortKey(remote) > sortKey(local)
    }
}

class UpdateRepository(
    private val apiClient: ApiClient,
    private val prefs: SessionPrefs,
    private val appContext: Context
) {
    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun checkUpdate(
        host: String,
        port: String,
        useHttps: Boolean,
        proxyUrl: String
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            prefs.saveConnection(host, port, useHttps, proxyUrl)
            apiClient.applySession(prefs.current())
            val api = apiClient.updateApi()
            val body = api.listVersions()
            if (!body.success) {
                return@runCatching UpdateCheckResult.Failed(body.message ?: "检查更新失败")
            }
            val latest = body.result?.records
                ?.firstOrNull { !it.fileurl.isNullOrBlank() && !it.fileversion.isNullOrBlank() }
                ?: return@runCatching UpdateCheckResult.UpToDate(VersionCompare.localVersion(), null)

            val local = VersionCompare.localVersion()
            val remoteVer = latest.fileversion.orEmpty()
            if (!VersionCompare.isRemoteNewer(remoteVer, local)) {
                UpdateCheckResult.UpToDate(local, remoteVer)
            } else {
                UpdateCheckResult.Available(
                    localVersion = local,
                    remote = latest,
                    downloadUrl = buildDownloadUrl(latest.fileurl!!)
                )
            }
        }.getOrElse { UpdateCheckResult.Failed(it.message ?: "检查更新异常") }
    }

    private suspend fun buildDownloadUrl(fileUrl: String): String {
        val base = prefs.current().resolveBaseUrl().trimEnd('/')
        val path = fileUrl.trim().removePrefix("/")
        return "$base/jeecg-boot/sys/common/static/$path"
    }

    fun downloadApk(downloadUrl: String, fileVersion: String): Flow<DownloadEvent> = flow {
        try {
            val snap = prefs.current()
            val dir = File(appContext.cacheDir, "apk_updates").apply { mkdirs() }
            val safeName = fileVersion.replace(Regex("""[^\w.\-]+"""), "_") + ".apk"
            val outFile = File(dir, safeName)
            if (outFile.exists()) outFile.delete()

            val request = Request.Builder()
                .url(downloadUrl)
                .get()
                .apply {
                    if (snap.isNgrokProxy()) header("ngrok-skip-browser-warning", "true")
                    val token = snap.token
                    if (token.isNotBlank()) header("X-Access-Token", token)
                }
                .build()

            downloadClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    emit(DownloadEvent.Failed("下载失败 HTTP ${resp.code}"))
                    return@flow
                }
                val body = resp.body ?: run {
                    emit(DownloadEvent.Failed("下载内容为空"))
                    return@flow
                }
                val total = body.contentLength()
                var downloaded = 0L
                var lastEmit = -1
                body.byteStream().use { input ->
                    outFile.outputStream().use { output ->
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buf)
                            if (read <= 0) break
                            output.write(buf, 0, read)
                            downloaded += read
                            val percent = if (total > 0) {
                                ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }
                            if (percent != lastEmit) {
                                lastEmit = percent
                                emit(DownloadEvent.Progress(percent, downloaded, total))
                            }
                        }
                        output.flush()
                    }
                }
                if (outFile.length() < 1024) {
                    outFile.delete()
                    emit(DownloadEvent.Failed("下载文件过小，可能失败"))
                } else {
                    emit(DownloadEvent.Success(outFile))
                }
            }
        } catch (e: Exception) {
            emit(DownloadEvent.Failed(e.message ?: "下载异常"))
        }
    }.flowOn(Dispatchers.IO)

    fun installApk(context: Context, apkFile: File): Result<Unit> = runCatching {
        val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val settings = Intent(
                    android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settings)
                error("请先允许安装未知应用，然后再次点击安装")
            }
        }
        context.startActivity(intent)
    }
}
