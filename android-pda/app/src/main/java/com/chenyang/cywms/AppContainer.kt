package com.chenyang.cywms

import android.content.Context
import com.chenyang.cywms.data.api.ApiClient
import com.chenyang.cywms.data.api.AuthRepository
import com.chenyang.cywms.data.api.UpdateRepository
import com.chenyang.cywms.data.prefs.SessionPrefs

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val prefs = SessionPrefs(appContext)
    val apiClient = ApiClient(prefs)
    val authRepository = AuthRepository(apiClient, prefs)
    val updateRepository = UpdateRepository(appContext)
}
