package com.example.theekhai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
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

/**
 * Desktop (JVM) platform target layout.
 * Totally identical to the web app (`web/src/main.ts`):
 * - Left persistent sidebar (Brand, 01-05 nav, Operations, Pilot dot, Account)
 * - Topbar (Location label, language toggle, user chip, Report CTA)
 * - Main content
 * - Civic footer (Privacy, Terms, Accountability note)
 */
@Composable
actual fun PlatformAppScaffold(
    navigator: AppNavigator,
    currentRoute: Route,
    content: @Composable () -> Unit
) {
    val ext = LocalTheekHaiColors.current
    var language by remember { mutableStateOf("en") }

    Row(modifier = Modifier.fillMaxSize()) {
        // ── 1. Web-Mirroring Desktop Sidebar ──
        DesktopSidebar(
            currentRoute = currentRoute,
            onNavigate = { navigator.navigateTo(it) },
            modifier = Modifier.width(260.dp).fillMaxHeight()
        )

        TheekHaiDivider(modifier = Modifier.fillMaxHeight().width(1.dp))

        // ── 2. Right Pane: Topbar + Main Body + Footer ──
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(ext.paper)
        ) {
            // Web-mirroring Topbar
            DesktopTopBar(
                language = language,
                onToggleLanguage = { language = if (language == "en") "hi" else "en" },
                onReportClick = { navigator.navigateTo(Route.Report) },
                onAccountClick = { navigator.navigateTo(Route.Account) },
                onSignOutClick = { navigator.navigateTo(Route.Login) }
            )

            TheekHaiDivider()

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()
            }

            TheekHaiDivider()

            // Web-mirroring Footer
            DesktopFooter(
                onNavigate = { navigator.navigateTo(it) }
            )
        }
    }
}

@Composable
private fun DesktopSidebar(
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
        Column {
            // Brand & Pilot status
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
                        text = "A BETTER EVERYDAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.muted,
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp
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

            Spacer(modifier = Modifier.height(28.dp))

            // Citizen Services Navigation (01 to 05)
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
                label = "Know your options",
                isActive = currentRoute == Route.Library,
                onClick = { onNavigate(Route.Library) }
            )
            SidebarNavItem(
                number = "05",
                label = "Resident profile",
                isActive = currentRoute == Route.Account,
                onClick = { onNavigate(Route.Account) }
            )

            Spacer(modifier = Modifier.height(22.dp))
            TheekHaiDivider()
            Spacer(modifier = Modifier.height(16.dp))

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

        // Side-Foot & User row
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Civic pilot note
            Column(modifier = Modifier.padding(start = 6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DotIndicator()
                    Text(
                        text = "Civic pilot · Supabase RLS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ext.ink
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "City-neutral. Community-focused.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = ext.muted
                )
            }

            // User card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onNavigate(Route.Account) },
                color = ext.paper,
                shape = RoundedCornerShape(6.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AvatarCircle(initials = "PS", size = 30.dp, bgColor = ext.green)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Priya Sharma", style = MaterialTheme.typography.labelMedium, color = ext.ink, fontWeight = FontWeight.Bold)
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
}

@Composable
private fun DesktopTopBar(
    language: String,
    onToggleLanguage: () -> Unit,
    onReportClick: () -> Unit,
    onAccountClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val ext = LocalTheekHaiColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(ext.sidebarBg)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "YOUR NEIGHBOURHOOD, BETTER TOGETHER",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            color = ext.muted,
            fontWeight = FontWeight.Bold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Language Button
            OutlinedButton(
                onClick = onToggleLanguage,
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = if (language == "en") "हिन्दी" else "English",
                    fontSize = 12.sp,
                    color = ext.ink
                )
            }

            // Account chip
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onAccountClick),
                color = ext.paper,
                shape = RoundedCornerShape(16.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AvatarCircle(initials = "PS", size = 20.dp, bgColor = ext.green)
                    Text("Priya Sharma", fontSize = 12.sp, color = ext.ink, fontWeight = FontWeight.Medium)
                    Surface(
                        color = Color(0xFFE2EADF),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "resident",
                            fontSize = 9.sp,
                            color = ext.green,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Quick Report CTA
            TheekHaiButton(
                text = "Report an issue  ↗",
                onClick = onReportClick,
                modifier = Modifier.height(34.dp)
            )
        }
    }
}

@Composable
private fun DesktopFooter(
    onNavigate: (Route) -> Unit
) {
    val ext = LocalTheekHaiColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(ext.sidebarBg)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "TheekHai • Civic pilot",
                fontSize = 12.sp,
                color = ext.muted
            )
            Text(
                text = "Privacy",
                fontSize = 12.sp,
                color = ext.green,
                modifier = Modifier.clickable { onNavigate(Route.Library) }
            )
            Text(
                text = "Terms",
                fontSize = 12.sp,
                color = ext.green,
                modifier = Modifier.clickable { onNavigate(Route.Library) }
            )
        }

        Text(
            text = "Built around accountability.",
            fontSize = 12.sp,
            color = ext.muted
        )
    }
}
