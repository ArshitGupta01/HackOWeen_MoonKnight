package com.example.theekhai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.theekhai.presentation.report.ReportScreen
import com.example.theekhai.presentation.workspace.CaseDetailScreen
import com.example.theekhai.presentation.workspace.WorkspaceScreen
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiTheme
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val navigator = remember { AppNavigator() }
    val currentRoute by navigator.currentRoute.collectAsState()

    TheekHaiTheme {
        CompositionLocalProvider(LocalNavigator provides navigator) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth >= 850.dp

                if (isWideScreen) {
                    // ── Wide Desktop / Tablet Layout: Sidebar + Main Content ──
                    Row(modifier = Modifier.fillMaxSize()) {
                        AppDesktopSidebar(
                            currentRoute = currentRoute,
                            onNavigate = { navigator.navigateTo(it) },
                            modifier = Modifier.width(260.dp).fillMaxHeight()
                        )

                        TheekHaiDivider(modifier = Modifier.fillMaxHeight().width(1.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(LocalTheekHaiColors.current.paper)
                        ) {
                            AppContent(currentRoute)
                        }
                    }
                } else {
                    // ── Compact Mobile Layout: Top Bar + Content + Bottom Bar ──
                    val drawerState = rememberDrawerState(DrawerValue.Closed)
                    val coroutineScope = rememberCoroutineScope()

                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                modifier = Modifier.width(280.dp),
                                drawerContainerColor = LocalTheekHaiColors.current.sidebarBg
                            ) {
                                AppDesktopSidebar(
                                    currentRoute = currentRoute,
                                    onNavigate = {
                                        navigator.navigateTo(it)
                                        coroutineScope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    ) {
                        Scaffold(
                            topBar = {
                                MobileTopBar(
                                    currentRoute = currentRoute,
                                    onOpenMenu = { coroutineScope.launch { drawerState.open() } },
                                    onOpenProfile = { navigator.navigateTo(Route.Account) }
                                )
                            },
                            bottomBar = {
                                MobileBottomNav(
                                    currentRoute = currentRoute,
                                    onNavigate = { navigator.navigateTo(it) }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .background(LocalTheekHaiColors.current.paper)
                            ) {
                                AppContent(currentRoute)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppContent(route: Route) {
    when (route) {
        Route.Home -> HomeScreen()
        Route.Login -> LoginScreen()
        Route.Signup -> SignupScreen()
        Route.Report -> ReportScreen()
        Route.Workspace -> WorkspaceScreen()
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

@Composable
private fun AppDesktopSidebar(
    currentRoute: Route,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTheekHaiColors.current

    Column(
        modifier = modifier
            .background(ext.sidebarBg)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section: Brand & primary navigation
        Column {
            // Brand & status indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Route.Home) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "THEEKHAI",
                        style = MaterialTheme.typography.titleMedium,
                        color = ext.green,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Smart Cities Redressal",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.muted
                    )
                }

                Surface(
                    color = TheekHaiColors.GreenSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        DotIndicator()
                        Text("Live", style = MaterialTheme.typography.labelSmall, color = TheekHaiColors.Green)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Citizen Navigation
            Eyebrow("CITIZEN SERVICES", modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))

            SidebarNavItem(
                number = "01",
                label = "Overview",
                isActive = currentRoute == Route.Home,
                onClick = { onNavigate(Route.Home) }
            )
            SidebarNavItem(
                number = "02",
                label = "Report an issue",
                isActive = currentRoute == Route.Report,
                onClick = { onNavigate(Route.Report) }
            )
            SidebarNavItem(
                number = "03",
                label = "My workspace",
                isActive = currentRoute == Route.Workspace || currentRoute is Route.CaseDetail,
                onClick = { onNavigate(Route.Workspace) }
            )
            SidebarNavItem(
                number = "04",
                label = "Civic guidance",
                isActive = currentRoute == Route.Library,
                onClick = { onNavigate(Route.Library) }
            )
            SidebarNavItem(
                number = "05",
                label = "Citizen profile",
                isActive = currentRoute == Route.Account,
                onClick = { onNavigate(Route.Account) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            TheekHaiDivider()
            Spacer(modifier = Modifier.height(18.dp))

            // Operations Navigation
            Eyebrow("OPERATIONS", modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))

            SidebarNavItem(
                number = "🏢",
                label = "Command Center",
                isActive = currentRoute == Route.CommandCenter,
                onClick = { onNavigate(Route.CommandCenter) }
            )
            SidebarNavItem(
                number = "👷",
                label = "Field Inspection",
                isActive = currentRoute == Route.FieldWorker,
                onClick = { onNavigate(Route.FieldWorker) }
            )
        }

        // Bottom User Row
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { onNavigate(Route.Login) },
            color = ext.paper,
            shape = RoundedCornerShape(6.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvatarCircle(initials = "PS", size = 32.dp, bgColor = ext.green)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Priya Sharma", style = MaterialTheme.typography.labelMedium, color = ext.ink)
                    Text("Ward 4 Resident", style = MaterialTheme.typography.bodySmall, color = ext.muted, fontSize = 11.sp)
                }
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Switch account",
                    tint = ext.muted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileTopBar(
    currentRoute: Route,
    onOpenMenu: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val ext = LocalTheekHaiColors.current
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "THEEKHAI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ext.green
                )
                Text(
                    text = when (currentRoute) {
                        Route.Home -> "Overview"
                        Route.Report -> "Report an issue"
                        Route.Workspace, is Route.CaseDetail -> "My workspace"
                        Route.Library -> "Civic guidance"
                        Route.Account -> "Citizen profile"
                        Route.CommandCenter -> "Command Center"
                        Route.FieldWorker -> "Field Inspection"
                        Route.Login -> "Sign in"
                        Route.Signup -> "Create account"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.muted
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onOpenMenu) {
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Open navigation menu", tint = ext.ink)
            }
        },
        actions = {
            IconButton(onClick = onOpenProfile) {
                AvatarCircle(initials = "PS", size = 28.dp, bgColor = ext.green)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ext.sidebarBg
        )
    )
}

@Composable
private fun MobileBottomNav(
    currentRoute: Route,
    onNavigate: (Route) -> Unit
) {
    val ext = LocalTheekHaiColors.current
    NavigationBar(
        containerColor = ext.sidebarBg,
        tonalElevation = 2.dp
    ) {
        NavigationBarItem(
            selected = currentRoute == Route.Home,
            onClick = { onNavigate(Route.Home) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ext.green,
                selectedTextColor = ext.green,
                indicatorColor = TheekHaiColors.GreenSurface
            )
        )
        NavigationBarItem(
            selected = currentRoute == Route.Report,
            onClick = { onNavigate(Route.Report) },
            icon = { Icon(Icons.Default.AddCircle, contentDescription = "Report") },
            label = { Text("Report", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ext.green,
                selectedTextColor = ext.green,
                indicatorColor = TheekHaiColors.GreenSurface
            )
        )
        NavigationBarItem(
            selected = currentRoute == Route.Workspace || currentRoute is Route.CaseDetail,
            onClick = { onNavigate(Route.Workspace) },
            icon = { Icon(Icons.Default.Assignment, contentDescription = "Workspace") },
            label = { Text("Workspace", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ext.green,
                selectedTextColor = ext.green,
                indicatorColor = TheekHaiColors.GreenSurface
            )
        )
        NavigationBarItem(
            selected = currentRoute == Route.Library,
            onClick = { onNavigate(Route.Library) },
            icon = { Icon(Icons.Default.MenuBook, contentDescription = "Guidance") },
            label = { Text("Guidance", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = ext.green,
                selectedTextColor = ext.green,
                indicatorColor = TheekHaiColors.GreenSurface
            )
        )
    }
}
