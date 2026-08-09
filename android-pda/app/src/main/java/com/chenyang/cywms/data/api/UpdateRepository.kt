package com.chenyang.cywms.data.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.chenyang.cywms.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class GithubReleaseInfo(
    val tagName: String,
    val name: String?,
    val body: String?,
    val publishedAt: String?,
    val version: String,
    val apkName: String,
    val downloadUrl: String,
    val size: Long
)

sealed class UpdateCheckResult {
    data class UpToDate(val localVersion: String, val remoteVersion: String?) : UpdateCheckResult()
    data class Available(
        val localVersion: String,
        val remote: GithubReleaseInfo
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

    /**
     * 版本规则：v1.0.0 起，每次发版末位 +1（v1.0.1、v1.0.2…）
     * 比较键：MAJOR*1_000_000 + MINOR*1_000 + PATCH
     */
    fun sortKey(version: String): Long {
        val m = Regex("""v?(\d+)\.(\d+)\.(\d+)""").find(version.trim())
            ?: return -1L
        val major = m.groupValues[1].toLongOrNull() ?: 0L
        val minor = m.groupValues[2].toLongOrNull() ?: 0L
        val patch = m.groupValues[3].toLongOrNull() ?: 0L
        return major * 1_000_000L + minor * 1_000L + patch
    }

    fun normalizeTag(tagOrName: String): String {
        val raw = tagOrName.trim()
            .removePrefix("pda-")
            .removePrefix("android-pda-")
            .removePrefix("release-")
        val semver = Regex("""v?\d+\.\d+\.\d+""").find(raw)?.value ?: return raw
        return if (semver.startsWith("v")) semver else "v$semver"
    }

    fun isRemoteNewer(remote: String, local: String): Boolean {
        if (remote.isBlank()) return false
        if (local.isBlank()) return true
        val r = normalizeTag(remote)
        val l = normalizeTag(local)
        if (r.equals(l, ignoreCase = true)) return false
        val rk = sortKey(r)
        val lk = sortKey(l)
        if (rk < 0 || lk < 0) return r > l
        return rk > lk
    }
}

class UpdateRepository(
    private val appContext: Context
) {
    companion object {
        const val GITHUB_OWNER = "ciahua"
        const val GITHUB_REPO = "cywms"
        const val APK_ASSET_NAME = "cywms-pda-debug.apk"
        private const val RELEASES_URL =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=15"
    }

    private val gson = Gson()

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private data class GhAsset(
        val name: String? = null,
        @SerializedName("browser_download_url") val browserDownloadUrl: String? = null,
        val size: Long = 0
    )

    private data class GhRelease(
        @SerializedName("tag_name") val tagName: String? = null,
        val name: String? = null,
        val body: String? = null,
        @SerializedName("published_at") val publishedAt: String? = null,
        val draft: Boolean = false,
        val prerelease: Boolean = false,
        val assets: List<GhAsset> = emptyList()
    )

    suspend fun checkUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val release = fetchLatestApkRelease()
                ?: return@runCatching UpdateCheckResult.UpToDate(VersionCompare.localVersion(), null)

            val local = VersionCompare.localVersion()
            val remoteVer = release.version
            if (!VersionCompare.isRemoteNewer(remoteVer, local)) {
                UpdateCheckResult.UpToDate(local, remoteVer)
            } else {
                UpdateCheckResult.Available(localVersion = local, remote = release)
            }
        }.getOrElse { UpdateCheckResult.Failed(it.message ?: "检查更新异常") }
    }

    private fun fetchLatestApkRelease(): GithubReleaseInfo? {
        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "cywms-pda/${BuildConfig.VERSION_NAME}")
            .get()
            .build()

        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) {
                error("GitHub HTTP ${resp.code}")
            }
            val text = resp.body?.string().orEmpty()
            val type = object : TypeToken<List<GhRelease>>() {}.type
            val list: List<GhRelease> = gson.fromJson(text, type) ?: emptyList()

            // GitHub /releases 默认按 created_at 排序；本仓库多条 release 的 created_at 相同，
            // 不能取列表第一项。应在带 APK 的 release 中按语义版本选最高。
            var best: GithubReleaseInfo? = null
            for (rel in list) {
                if (rel.draft) continue
                val asset = rel.assets.firstOrNull {
                    val n = it.name.orEmpty()
                    n.equals(APK_ASSET_NAME, ignoreCase = true) || n.endsWith(".apk", ignoreCase = true)
                } ?: continue
                val url = asset.browserDownloadUrl ?: continue
                val version = resolveVersion(rel)
                val info = GithubReleaseInfo(
                    tagName = rel.tagName.orEmpty(),
                    name = rel.name,
                    body = rel.body,
                    publishedAt = rel.publishedAt,
                    version = version,
                    apkName = asset.name.orEmpty(),
                    downloadUrl = url,
                    size = asset.size
                )
                val cur = best
                if (cur == null || VersionCompare.isRemoteNewer(info.version, cur.version)) {
                    best = info
                }
            }
            return best
        }
    }

    /**
     * 版本优先取 tag（pda-v1.0.0 → v1.0.0），
     * 否则取 release name / body 中的 VERSION=。
     */
    private fun resolveVersion(rel: GhRelease): String {
        val fromTag = VersionCompare.normalizeTag(rel.tagName.orEmpty())
        if (VersionCompare.sortKey(fromTag) >= 0) return fromTag
        val fromName = VersionCompare.normalizeTag(rel.name.orEmpty())
        if (VersionCompare.sortKey(fromName) >= 0) return fromName
        rel.body?.lineSequence()?.forEach { line ->
            val m = Regex("""(?i)^VERSION\s*=\s*(.+)$""").find(line.trim())
            if (m != null) {
                val v = VersionCompare.normalizeTag(m.groupValues[1].trim())
                if (VersionCompare.sortKey(v) >= 0) return v
            }
        }
        return fromTag.ifBlank { "v0.0.0" }
    }

    fun downloadApk(downloadUrl: String, fileVersion: String): Flow<DownloadEvent> = flow {
        try {
            val dir = File(appContext.cacheDir, "apk_updates").apply { mkdirs() }
            val safeName = fileVersion.replace(Regex("""[^\w.\-]+"""), "_") + ".apk"
            val outFile = File(dir, safeName)
            if (outFile.exists()) outFile.delete()

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "cywms-pda/${BuildConfig.VERSION_NAME}")
                .header("Accept", "application/octet-stream")
                .get()
                .build()

            http.newCall(request).execute().use { resp ->
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
