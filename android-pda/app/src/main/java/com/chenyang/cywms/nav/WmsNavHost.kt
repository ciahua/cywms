package com.chenyang.cywms.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.chenyang.cywms.WmsApp
import com.chenyang.cywms.data.prefs.SessionSnapshot
import com.chenyang.cywms.ui.common.ModuleSoftKeyboardGuard
import com.chenyang.cywms.ui.home.HomeScreen
import com.chenyang.cywms.ui.login.LoginRoute
import com.chenyang.cywms.ui.login.LoginViewModel
import com.chenyang.cywms.ui.scan.ProduceIssueScanScreen
import com.chenyang.cywms.ui.theme.Amber500
import com.chenyang.cywms.ui.theme.Navy900
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val PRODUCE_ISSUE_SCAN = "produce_issue_scan"
}

@Composable
fun WmsNavHost() {
    val context = LocalContext.current
    val app = context.applicationContext as WmsApp
    val container = app.container
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val session by container.prefs.snapshotFlow
        .collectAsStateWithLifecycle(initialValue = null as SessionSnapshot?)

    if (session == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy900),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Amber500)
        }
        return
    }

    val start = if (session!!.token.isNotBlank()) Routes.HOME else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = start
    ) {
        composable(Routes.LOGIN) {
            val vm: LoginViewModel = viewModel(factory = LoginViewModel.factory(app))
            LoginRoute(
                viewModel = vm,
                onLoggedIn = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.HOME) {
            ModuleSoftKeyboardGuard()
            val name = session?.realName?.ifBlank { null }
                ?: session?.username.orEmpty()
            HomeScreen(
                displayName = name,
                onLogout = {
                    scope.launch {
                        container.authRepository.logout()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onModuleClick = { module ->
                    when (module.id) {
                        "produce_issue" -> navController.navigate(Routes.PRODUCE_ISSUE_SCAN)
                    }
                }
            )
        }
        composable(Routes.PRODUCE_ISSUE_SCAN) {
            ModuleSoftKeyboardGuard()
            ProduceIssueScanScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
