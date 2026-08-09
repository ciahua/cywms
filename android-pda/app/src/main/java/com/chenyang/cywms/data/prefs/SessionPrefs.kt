package com.chenyang.cywms.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cywms_session")

data class SessionSnapshot(
    val host: String = "172.16.5.7",
    val port: String = "8080",
    val useHttps: Boolean = false,
    val proxyUrl: String = "",
    val username: String = "",
    val password: String = "",
    val rememberAccount: Boolean = true,
    val token: String = "",
    val realName: String = ""
) {
    fun resolveBaseUrl(): String {
        val proxy = proxyUrl.trim().trimEnd('/')
        if (proxy.isNotEmpty()) return proxy
        val scheme = if (useHttps) "https" else "http"
        val h = host.trim().ifEmpty { "172.16.5.7" }
        val p = port.trim().ifEmpty { "8080" }
        return "$scheme://$h:$p"
    }

    fun isNgrokProxy(): Boolean {
        val p = proxyUrl.lowercase()
        return p.contains("ngrok")
    }
}

class SessionPrefs(private val context: Context) {
    private object Keys {
        val host = stringPreferencesKey("host")
        val port = stringPreferencesKey("port")
        val useHttps = booleanPreferencesKey("use_https")
        val proxyUrl = stringPreferencesKey("proxy_url")
        val username = stringPreferencesKey("username")
        val password = stringPreferencesKey("password")
        val remember = booleanPreferencesKey("remember")
        val token = stringPreferencesKey("token")
        val realName = stringPreferencesKey("real_name")
    }

    val snapshotFlow: Flow<SessionSnapshot> = context.dataStore.data.map { prefs ->
        SessionSnapshot(
            host = prefs[Keys.host] ?: "172.16.5.7",
            port = prefs[Keys.port] ?: "8080",
            useHttps = prefs[Keys.useHttps] ?: false,
            proxyUrl = prefs[Keys.proxyUrl] ?: "",
            username = prefs[Keys.username] ?: "",
            password = prefs[Keys.password] ?: "",
            rememberAccount = prefs[Keys.remember] ?: true,
            token = prefs[Keys.token] ?: "",
            realName = prefs[Keys.realName] ?: ""
        )
    }

    suspend fun current(): SessionSnapshot = snapshotFlow.first()

    suspend fun saveConnection(
        host: String,
        port: String,
        useHttps: Boolean,
        proxyUrl: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.host] = host.trim()
            prefs[Keys.port] = port.trim()
            prefs[Keys.useHttps] = useHttps
            prefs[Keys.proxyUrl] = proxyUrl.trim()
        }
    }

    suspend fun saveLoginSuccess(
        username: String,
        password: String,
        rememberAccount: Boolean,
        token: String,
        realName: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.username] = username
            prefs[Keys.remember] = rememberAccount
            prefs[Keys.password] = if (rememberAccount) password else ""
            prefs[Keys.token] = token
            prefs[Keys.realName] = realName
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs[Keys.token] = ""
        }
    }
}
