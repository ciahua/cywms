package com.chenyang.cywms

import android.content.Context
import com.chenyang.cywms.data.api.ApiClient
import com.chenyang.cywms.data.api.AuthRepository
import com.chenyang.cywms.data.prefs.SessionPrefs

class AppContainer(context: Context) {
    val prefs = SessionPrefs(context.applicationContext)
    val apiClient = ApiClient(prefs)
    val authRepository = AuthRepository(apiClient, prefs)
}
