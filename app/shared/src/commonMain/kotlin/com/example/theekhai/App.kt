package com.example.theekhai

import androidx.compose.runtime.*
import com.example.theekhai.navigation.AppNavigator
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.presentation.account.AccountScreen
import com.example.theekhai.presentation.admin.CommandCenterRoute
import com.example.theekhai.presentation.admin.CommandCenterViewModel
import com.example.theekhai.presentation.auth.LoginScreen
import com.example.theekhai.presentation.auth.SignupScreen
import com.example.theekhai.presentation.fieldworker.FieldWorkerRoute
import com.example.theekhai.presentation.fieldworker.FieldWorkerViewModel
import com.example.theekhai.presentation.home.HomeScreen
import com.example.theekhai.presentation.library.LibraryScreen
import com.example.theekhai.presentation.map.MapScreen
import com.example.theekhai.presentation.report.ReportScreen
import com.example.theekhai.presentation.workspace.CaseDetailScreen
import com.example.theekhai.presentation.workspace.WorkspaceScreen
import com.example.theekhai.ui.theme.TheekHaiTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val navigator = remember { AppNavigator() }
    val currentRoute by navigator.currentRoute.collectAsState()

    TheekHaiTheme {
        CompositionLocalProvider(LocalNavigator provides navigator) {
            PlatformAppScaffold(
                navigator = navigator,
                currentRoute = currentRoute
            ) {
                AppContent(currentRoute)
            }
        }
    }
}

@Composable
fun AppContent(route: Route) {
    when (route) {
        Route.Home -> HomeScreen()
        Route.Login -> LoginScreen()
        Route.Signup -> SignupScreen()
        Route.Report -> ReportScreen()
        Route.Workspace -> WorkspaceScreen()
        Route.Map -> MapScreen()
        is Route.CaseDetail -> CaseDetailScreen(caseId = route.caseId)
        Route.Account -> AccountScreen()
        Route.Library -> LibraryScreen()
        Route.CommandCenter -> {
            val vm = koinViewModel<CommandCenterViewModel>()
            CommandCenterRoute(vm)
        }
        Route.FieldWorker -> {
            val vm = koinViewModel<FieldWorkerViewModel>()
            FieldWorkerRoute(vm)
        }
    }
}
