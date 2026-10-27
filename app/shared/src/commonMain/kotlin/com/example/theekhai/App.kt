package com.example.theekhai

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.theekhai.navigation.AppNavigator
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.presentation.account.AccountScreen
import com.example.theekhai.presentation.auth.LoginScreen
import com.example.theekhai.presentation.auth.SignupScreen
import com.example.theekhai.presentation.home.HomeScreen
import com.example.theekhai.presentation.leaderboard.LeaderboardScreen
import com.example.theekhai.presentation.library.LibraryScreen
import com.example.theekhai.presentation.map.MapScreen
import com.example.theekhai.presentation.report.ReportScreen
import com.example.theekhai.presentation.workspace.CaseDetailScreen
import com.example.theekhai.presentation.workspace.LiveCaseDetailScreen
import com.example.theekhai.presentation.workspace.WorkspaceScreen
import com.example.theekhai.presentation.nativeconnect.NativeConnectViewModel
import com.example.theekhai.ui.theme.TheekHaiTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val navigator = remember { AppNavigator() }
    val currentRoute by navigator.currentRoute.collectAsState()
    val systemDark = isSystemInDarkTheme()
    var darkOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val darkMode = darkOverride ?: systemDark

    CompositionLocalProvider(LocalNavigator provides navigator) {
        TheekHaiTheme(useDarkTheme = darkMode) {
            PlatformAppScaffold(
                navigator = navigator,
                currentRoute = currentRoute,
                darkMode = darkMode,
                onToggleDarkMode = { darkOverride = !darkMode }
            ) {
                AppNavHost(currentRoute = currentRoute)
            }
        }
    }
}

@Composable
private fun AppNavHost(currentRoute: Route) {
    when (val route = currentRoute) {
        Route.Home -> HomeScreen()
        Route.Login -> LoginScreen()
        Route.Signup -> SignupScreen()
        Route.Report -> ReportScreen()
        Route.Workspace, Route.Map, Route.CommandCenter, Route.FieldWorker -> {
            val vm = koinViewModel<NativeConnectViewModel>()
            val st by vm.state.collectAsState()
            LaunchedEffect(vm) { vm.loadCases() }
            if (route == Route.Map) {
                MapScreen(st.cases, st.isLoading, st.error, vm::loadCases)
            } else {
                WorkspaceScreen(
                    complaints = st.cases,
                    isLoading = st.isLoading,
                    error = st.error,
                    onRetry = vm::loadCases,
                    heading = when (route) {
                        Route.CommandCenter -> "Case operations"
                        Route.FieldWorker -> "Field work"
                        else -> "My workspace"
                    },
                    description = "Live cases and workflow actions are provided by your account permissions."
                )
            }
        }
        Route.Leaderboard -> LeaderboardScreen()
        is Route.CaseDetail -> LiveCaseDetailScreen(caseId = route.caseId)
        Route.Account -> AccountScreen()
        Route.Library -> LibraryScreen()
    }
}
