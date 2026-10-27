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

    TheekHaiTheme(useDarkTheme = darkMode) {
        CompositionLocalProvider(LocalNavigator provides navigator) {
            PlatformAppScaffold(
                navigator = navigator,
                currentRoute = currentRoute,
                darkMode = darkMode,
                onToggleDarkMode = { darkOverride = !darkMode }
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
        Route.Workspace -> {
            val vm = koinViewModel<NativeConnectViewModel>()
            val st by vm.state.collectAsState()
            LaunchedEffect(vm) { vm.loadCases() }
            WorkspaceScreen(complaints = st.cases, isLoading = st.isLoading, error = st.error, onRetry = vm::loadCases)
        }
        Route.Map -> {
            val vm = koinViewModel<NativeConnectViewModel>()
            val st by vm.state.collectAsState()
            LaunchedEffect(vm) { vm.loadCases() }
            MapScreen(st.cases)
        }
        is Route.CaseDetail -> LiveCaseDetailScreen(caseId = route.caseId)
        Route.Account -> AccountScreen()
        Route.Library -> LibraryScreen()
        Route.CommandCenter, Route.FieldWorker -> {
            val vm = koinViewModel<NativeConnectViewModel>()
            val st by vm.state.collectAsState()
            LaunchedEffect(vm) { vm.loadCases() }
            WorkspaceScreen(
                complaints = st.cases,
                isLoading = st.isLoading,
                error = st.error,
                onRetry = vm::loadCases,
                heading = if (route == Route.CommandCenter) "Case operations" else "Field work",
                description = "Live cases and workflow actions are provided by your account permissions."
            )
        }
    }
}
