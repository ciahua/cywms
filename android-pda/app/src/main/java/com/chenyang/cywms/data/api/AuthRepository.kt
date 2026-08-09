package com.chenyang.cywms.data.api

import com.chenyang.cywms.data.model.LoginRequest
import com.chenyang.cywms.data.prefs.SessionPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class LoginOutcome {
    data class Success(val realName: String, val username: String) : LoginOutcome()
    data class Failure(val message: String) : LoginOutcome()
}

class AuthRepository(
    private val apiClient: ApiClient,
    private val prefs: SessionPrefs
) {
    suspend fun login(
        username: String,
        password: String,
        host: String,
        port: String,
        useHttps: Boolean,
        proxyUrl: String,
        rememberAccount: Boolean
    ): LoginOutcome = withContext(Dispatchers.IO) {
        prefs.saveConnection(host, port, useHttps, proxyUrl)
        val snapshot = prefs.current()
        apiClient.applySession(snapshot)

        runCatching {
            apiClient.authApi().login(LoginRequest(username.trim(), password))
        }.fold(
            onSuccess = { body ->
                if (body.success && !body.result?.token.isNullOrBlank()) {
                    val token = body.result!!.token!!
                    val realName = body.result.userInfo?.realname
                        ?: body.result.userInfo?.username
                        ?: username
                    prefs.saveLoginSuccess(
                        username = username.trim(),
                        password = password,
                        rememberAccount = rememberAccount,
                        token = token,
                        realName = realName
                    )
                    apiClient.applySession(prefs.current())
                    LoginOutcome.Success(realName = realName, username = username.trim())
                } else {
                    LoginOutcome.Failure(body.message?.ifBlank { null } ?: "登录失败")
                }
            },
            onFailure = { e ->
                LoginOutcome.Failure(e.message ?: "网络异常，请检查服务器/代理地址")
            }
        )
    }

    suspend fun logout() {
        prefs.clearToken()
    }
}
