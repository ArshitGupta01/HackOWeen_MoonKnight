package com.example.theekhai

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.isSystemInDarkTheme
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
        Route.Workspace -> { val vm = koinViewModel<CommandCenterViewModel>(); val st by vm.uiState.collectAsState(); WorkspaceScreen(complaints = st.filteredComplaints) }
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
