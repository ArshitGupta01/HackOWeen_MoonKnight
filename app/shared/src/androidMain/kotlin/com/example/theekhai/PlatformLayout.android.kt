package com.example.theekhai

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
import com.example.theekhai.navigation.Route
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors
import kotlinx.coroutines.launch

@Composable
actual fun PlatformAppScaffold(
    navigator: AppNavigator,
    currentRoute: Route,
    content: @Composable () -> Unit
) {
    val ext = LocalTheekHaiColors.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(290.dp),
                drawerContainerColor = ext.sidebarBg
            ) {
                AndroidDrawerContent(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navigator.navigateTo(route)
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                AndroidTopBar(
                    currentRoute = currentRoute,
                    onOpenMenu = { coroutineScope.launch { drawerState.open() } }
                )
            },
            bottomBar = {
                AndroidBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { navigator.navigateTo(it) }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(ext.paper)
            ) {
                content()
            }
        }
    }
}

/**
 * Top App Bar matching the user's wireframe sketches:
 * - Left: "Theek Hai" with secondary breadcrumb when in sub-screens ("→ Report an issue", "→ Civic Map", etc.)
 * - Right: Hamburger icon (≡)
 * - Bottom: Clean divider line
 */
@Composable
private fun AndroidTopBar(
    currentRoute: Route,
    onOpenMenu: () -> Unit
) {
    val ext = LocalTheekHaiColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ext.sidebarBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Theek Hai",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = ext.ink
                    )

                    if (currentRoute != Route.Home) {
                        val subtitle = when (currentRoute) {
                            Route.Report -> "→ Report an issue"
                            Route.Map -> "→ Civic Map"
                            Route.Workspace -> "→ My workspace"
                            Route.Account -> "→ Resident profile"
                            Route.Library -> "→ Civic guidance"
                            Route.CommandCenter -> "→ Command Center"
                            Route.FieldWorker -> "→ Field Inspection"
                            Route.Login -> "→ Sign in"
                            Route.Signup -> "→ Create account"
                            is Route.CaseDetail -> "→ Case #${currentRoute.caseId.take(8)}"
                            else -> ""
                        }
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = ext.muted
                            )
                        }
                    }
                }

                // Hamburger Menu Icon on Top Right
                IconButton(
                    onClick = onOpenMenu,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open navigation menu",
                        tint = ext.ink,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            TheekHaiDivider()
        }
    }
}

/**
 * Android 5-Tab Bottom Navigation Bar matching the user's wireframe sketches:
 * 1. HOME (Route.Home)
 * 2. MAP (Route.Map)
 * 3. Report (Route.Report) -> Centered, Circle with '+', highlighted pill background when active
 * 4. WORKSPACE (Route.Workspace)
 * 5. PROFILE (Route.Account)
 */
@Composable
private fun AndroidBottomNavBar(
    currentRoute: Route,
    onNavigate: (Route) -> Unit
) {
    val ext = LocalTheekHaiColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ext.sidebarBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            TheekHaiDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // 1. HOME
                AndroidNavTabItem(
                    label = "HOME",
                    icon = Icons.Default.Home,
                    isSelected = currentRoute == Route.Home,
                    onClick = { onNavigate(Route.Home) },
                    modifier = Modifier.weight(1f)
                )

                // 2. MAP
                AndroidNavTabItem(
                    label = "MAP",
                    icon = Icons.Default.Map,
                    isSelected = currentRoute == Route.Map,
                    onClick = { onNavigate(Route.Map) },
                    modifier = Modifier.weight(1f)
                )

                // 3. Report (Center Action with Pill Highlight)
                AndroidReportCenterTab(
                    isSelected = currentRoute == Route.Report,
                    onClick = { onNavigate(Route.Report) },
                    modifier = Modifier.weight(1.15f)
                )

                // 4. WORKSPACE
                AndroidNavTabItem(
                    label = "WORKSPACE",
                    icon = Icons.Default.Description,
                    isSelected = currentRoute == Route.Workspace || currentRoute is Route.CaseDetail,
                    onClick = { onNavigate(Route.Workspace) },
                    modifier = Modifier.weight(1f)
                )

                // 5. PROFILE
                AndroidNavTabItem(
                    label = "PROFILE",
                    icon = Icons.Default.AccountCircle,
                    isSelected = currentRoute == Route.Account || currentRoute == Route.Login || currentRoute == Route.Signup,
                    onClick = { onNavigate(Route.Account) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AndroidNavTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTheekHaiColors.current
    val itemColor = if (isSelected) ext.green else ext.muted

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = itemColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = itemColor,
            letterSpacing = 0.4.sp
        )
    }
}

/**
 * Highlighted center pill container for the "Report (+)" tab, matching Image 2 sketch!
 */
@Composable
private fun AndroidReportCenterTab(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTheekHaiColors.current
    val pillBg = if (isSelected) Color(0xFFDCE2D6) else Color(0xFFE8ECE3)
    val pillBorder = if (isSelected) ext.green else Color(0xFFCCD4C6)

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        color = pillBg,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, pillBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = "Report issue",
                tint = ext.green,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Report",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ext.green
            )
        }
    }
}

/**
 * Slide-out navigation drawer opened by the hamburger icon (≡) on Android.
 */
@Composable
private fun AndroidDrawerContent(
    currentRoute: Route,
    onNavigate: (Route) -> Unit
) {
    val ext = LocalTheekHaiColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Route.Home) }
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Theek Hai",
                        style = MaterialTheme.typography.titleLarge,
                        color = ext.green,
                        fontWeight = FontWeight.Bold
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
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DotIndicator()
                        Text("Live", style = MaterialTheme.typography.labelSmall, color = TheekHaiColors.Green)
                    }
                }
            }

            TheekHaiDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Citizen Services Section
            Eyebrow("CITIZEN SERVICES", modifier = Modifier.padding(start = 10.dp, bottom = 6.dp))

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
                label = "Civic Map radar",
                isActive = currentRoute == Route.Map,
                onClick = { onNavigate(Route.Map) }
            )
            SidebarNavItem(
                number = "04",
                label = "My workspace",
                isActive = currentRoute == Route.Workspace || currentRoute is Route.CaseDetail,
                onClick = { onNavigate(Route.Workspace) }
            )
            SidebarNavItem(
                number = "05",
                label = "Civic guidance",
                isActive = currentRoute == Route.Library,
                onClick = { onNavigate(Route.Library) }
            )
            SidebarNavItem(
                number = "06",
                label = "Resident profile",
                isActive = currentRoute == Route.Account,
                onClick = { onNavigate(Route.Account) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            TheekHaiDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Operations Section
            Eyebrow("OPERATIONS", modifier = Modifier.padding(start = 10.dp, bottom = 6.dp))

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

        // Drawer Bottom: User profile & Switch
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onNavigate(Route.Account) },
            color = ext.paper,
            shape = RoundedCornerShape(8.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AvatarCircle(initials = "PS", size = 32.dp, bgColor = ext.green)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Priya Sharma", style = MaterialTheme.typography.labelMedium, color = ext.ink, fontWeight = FontWeight.Bold)
                    Text("Ward 4 Resident", style = MaterialTheme.typography.bodySmall, color = ext.muted, fontSize = 11.sp)
                }
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Account",
                    tint = ext.muted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
