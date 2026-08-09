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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chenyang.cywms.WmsApp
import com.chenyang.cywms.data.prefs.SessionSnapshot
import com.chenyang.cywms.ui.common.ModuleSoftKeyboardGuard
import com.chenyang.cywms.ui.home.HomeScreen
import com.chenyang.cywms.ui.login.LoginRoute
import com.chenyang.cywms.ui.login.LoginViewModel
import com.chenyang.cywms.ui.produce.ProduceIssueDetailRoute
import com.chenyang.cywms.ui.produce.ProduceIssueDetailViewModel
import com.chenyang.cywms.ui.produce.ProduceIssueListRoute
import com.chenyang.cywms.ui.produce.ProduceIssueListViewModel
import com.chenyang.cywms.ui.produce.ProduceIssueScanRoute
import com.chenyang.cywms.ui.produce.ProduceIssueScanViewModel
import com.chenyang.cywms.ui.theme.Amber500
import com.chenyang.cywms.ui.theme.Navy900
import kotlinx.coroutines.launch

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val PRODUCE_ISSUE_LIST = "produce_issue_list"
    const val PRODUCE_ISSUE_DETAIL = "produce_issue_detail/{mainId}"
    const val PRODUCE_ISSUE_SCAN = "produce_issue_scan"

    fun detail(mainId: String) = "produce_issue_detail/$mainId"
}

/** 扫码页参数（含中文规格，避免路由编码问题） */
object ProduceIssueScanArgs {
    var mainId: String = ""
    var detailId: String = ""
    var orderNo: String = ""
    var matcode: String = ""
    var matname: String = ""
    var matspec: String = ""
    var unit: String = ""
    var needQty: String = ""
    var detailOrderNo: String = ""
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
                        "produce_issue" -> navController.navigate(Routes.PRODUCE_ISSUE_LIST)
                    }
                }
            )
        }
        composable(Routes.PRODUCE_ISSUE_LIST) {
            ModuleSoftKeyboardGuard()
            val vm: ProduceIssueListViewModel = viewModel(
                factory = ProduceIssueListViewModel.factory(app)
            )
            ProduceIssueListRoute(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenOrder = { mainId, orderNo ->
                    ProduceIssueScanArgs.detailOrderNo = orderNo
                    navController.navigate(Routes.detail(mainId))
                }
            )
        }
        composable(
            route = Routes.PRODUCE_ISSUE_DETAIL,
            arguments = listOf(navArgument("mainId") { type = NavType.StringType })
        ) { entry ->
            ModuleSoftKeyboardGuard()
            val mainId = entry.arguments?.getString("mainId").orEmpty()
            val orderNo = ProduceIssueScanArgs.detailOrderNo
            val vm: ProduceIssueDetailViewModel = viewModel(
                factory = ProduceIssueDetailViewModel.factory(app, mainId, orderNo)
            )
            ProduceIssueDetailRoute(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onScanLine = { detailId, matcode, matname, matspec, unit, needQty ->
                    ProduceIssueScanArgs.mainId = mainId
                    ProduceIssueScanArgs.detailId = detailId
                    ProduceIssueScanArgs.orderNo = orderNo
                    ProduceIssueScanArgs.matcode = matcode
                    ProduceIssueScanArgs.matname = matname
                    ProduceIssueScanArgs.matspec = matspec
                    ProduceIssueScanArgs.unit = unit
                    ProduceIssueScanArgs.needQty = needQty
                    navController.navigate(Routes.PRODUCE_ISSUE_SCAN)
                }
            )
        }
        composable(Routes.PRODUCE_ISSUE_SCAN) {
            ModuleSoftKeyboardGuard()
            val args = ProduceIssueScanArgs
            val vm: ProduceIssueScanViewModel = viewModel(
                factory = ProduceIssueScanViewModel.factory(
                    app = app,
                    mainId = args.mainId,
                    detailId = args.detailId,
                    orderNo = args.orderNo,
                    matcode = args.matcode,
                    matname = args.matname,
                    matspec = args.matspec,
                    unit = args.unit,
                    needQty = args.needQty
                )
            )
            ProduceIssueScanRoute(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSubmittedAndDone = {
                    navController.popBackStack()
                }
            )
        }
    }
}
