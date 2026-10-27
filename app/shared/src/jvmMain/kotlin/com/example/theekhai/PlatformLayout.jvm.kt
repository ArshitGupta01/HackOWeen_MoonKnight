package com.example.theekhai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.theekhai.ui.localization.AppLanguage
import com.example.theekhai.ui.localization.LocalAppLanguage
import com.example.theekhai.ui.localization.t
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors

/**
 * Desktop (JVM) platform target layout.
 * Exactly mirrors the web app (`web/src/main.ts` & `web/src/style.css`):
 * - Left persistent sidebar (230px, #eceee5, brand, 01-05 nav, operations, side-foot)
 * - Topbar (72px, #f5f3ec, location-label, language button, account-chip, report CTA)
 * - Reactive Hindi/English toggle matching web app's `switchLanguage()`
 * - Main content (#f5f3ec)
 * - Civic footer (#f5f3ec, privacy, terms, built around accountability)
 */
@Composable
actual fun PlatformAppScaffold(
    navigator: AppNavigator,
    currentRoute: Route,
    content: @Composable () -> Unit
) {
    val ext = LocalTheekHaiColors.current
    var currentLanguage by remember { mutableStateOf(AppLanguage.EN) }

    CompositionLocalProvider(LocalAppLanguage provides currentLanguage) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(TheekHaiColors.Background)
        ) {
            // ── 1. Web-Mirroring Persistent Sidebar (230px) ──
            DesktopSidebar(
                currentRoute = currentRoute,
                onNavigate = { navigator.navigateTo(it) },
                modifier = Modifier
                    .width(230.dp)
                    .fillMaxHeight()
            )

            TheekHaiDivider(modifier = Modifier.fillMaxHeight().width(1.dp))

            // ── 2. Right Pane: Topbar (72dp) + Main Body + Footer ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(TheekHaiColors.Background)
            ) {
                // Web-mirroring Topbar with functional Hindi-English toggle
                DesktopTopBar(
                    language = currentLanguage,
                    onToggleLanguage = {
                        currentLanguage = if (currentLanguage == AppLanguage.EN) AppLanguage.HI else AppLanguage.EN
                    },
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
}

@Composable
private fun DesktopSidebar(
    currentRoute: Route,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTheekHaiColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .background(TheekHaiColors.SidebarBg)
            .verticalScroll(scrollState)
            .padding(top = 36.dp, start = 24.dp, end = 24.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Brand: TheekHai + A BETTER EVERYDAY (matching web/src/style.css .brand & .brand-caption)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(Route.Home) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = TheekHaiColors.Green
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "TH",
                                color = Color(0xFFFCFBF6),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Text(
                        text = "TheekHai",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = ext.ink
                    )
                }

                Text(
                    text = t("A BETTER EVERYDAY", "बेहतर हर दिन"),
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.muted,
                    fontSize = 9.sp,
                    letterSpacing = 1.8.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 36.dp, start = 2.dp)
                )
            }

            // Citizen Services Navigation
            Eyebrow(t("CITIZEN SERVICES", "नागरिक सेवाएँ"), modifier = Modifier.padding(start = 10.dp, bottom = 6.dp))

            WebSidebarNavItem(
                number = "01",
                label = t("Overview", "परिचय"),
                isActive = currentRoute == Route.Home,
                onClick = { onNavigate(Route.Home) }
            )
            WebSidebarNavItem(
                number = "02",
                label = t("Report an issue", "समस्या दर्ज करें"),
                isActive = currentRoute == Route.Report,
                onClick = { onNavigate(Route.Report) }
            )
            WebSidebarNavItem(
                number = "03",
                label = t("My workspace", "मेरा कार्यक्षेत्र"),
                isActive = currentRoute == Route.Workspace || currentRoute is Route.CaseDetail,
                onClick = { onNavigate(Route.Workspace) }
            )
            WebSidebarNavItem(
                number = "04",
                label = t("Civic Map", "नागरिक मानचित्र"),
                isActive = currentRoute == Route.Map,
                onClick = { onNavigate(Route.Map) }
            )
            WebSidebarNavItem(
                number = "05",
                label = t("Know your options", "अपने विकल्प जानें"),
                isActive = currentRoute == Route.Library,
                onClick = { onNavigate(Route.Library) }
            )
            WebSidebarNavItem(
                number = "06",
                label = t("My account", "मेरा खाता"),
                isActive = currentRoute == Route.Account,
                onClick = { onNavigate(Route.Account) }
            )

            Spacer(modifier = Modifier.height(20.dp))
            TheekHaiDivider()
            Spacer(modifier = Modifier.height(14.dp))

            // Operations Navigation
            Eyebrow(t("OPERATIONS", "संचालन"), modifier = Modifier.padding(start = 10.dp, bottom = 6.dp))

            WebSidebarNavItem(
                number = "🏢",
                label = t("Command Center", "कमांड सेंटर"),
                isActive = currentRoute == Route.CommandCenter,
                onClick = { onNavigate(Route.CommandCenter) }
            )
            WebSidebarNavItem(
                number = "👷",
                label = t("Field Inspection", "फील्ड निरीक्षण"),
                isActive = currentRoute == Route.FieldWorker,
                onClick = { onNavigate(Route.FieldWorker) }
            )
        }

        // Side-Foot (matching web/src/style.css .side-foot)
        Column(modifier = Modifier.padding(top = 32.dp)) {
            TheekHaiDivider()
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                DotIndicator()
                Text(
                    text = t("Civic pilot · Supabase RLS", "नागरिक पायलट · सुपाबेस RLS"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ext.ink,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = t("City-neutral. Community-focused.", "हर शहर के लिए। समुदाय के साथ।"),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = ext.muted
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = t("Help & permissions", "सहायता और अनुमतियाँ"),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = ext.green,
                modifier = Modifier.clickable { onNavigate(Route.Library) }
            )
        }
    }
}

@Composable
private fun WebSidebarNavItem(
    number: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTheekHaiColors.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clickable(onClick = onClick),
        color = if (isActive) TheekHaiColors.Green else Color.Transparent,
        shape = RoundedCornerShape(5.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = number,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) Color(0xFFE8EDE2).copy(alpha = 0.7f) else ext.muted
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isActive) Color(0xFFFCFBF6) else Color(0xFF5B6B61)
            )
        }
    }
}

@Composable
private fun DesktopTopBar(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onReportClick: () -> Unit,
    onAccountClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    val ext = LocalTheekHaiColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(TheekHaiColors.Background)
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Location Label (matching .location-label in web/src/style.css)
        Text(
            text = t("YOUR NEIGHBOURHOOD, BETTER TOGETHER", "आपका पड़ोस, मिलकर बेहतर"),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFF6C796D)
        )

        // Header Tools (matching .header-tools in web/src/style.css: gap 24px)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Language Button (borderless quiet button matching web .language)
            TextButton(
                onClick = onToggleLanguage,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.EN) "हिन्दी" else "English",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TheekHaiColors.Green
                )
            }

            // Account chip (matching .account-chip in web/src/style.css)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onAccountClick),
                color = Color(0xFFE8ECE1),
                shape = RoundedCornerShape(999.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCFD7C7))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AvatarCircle(initials = "PS", size = 24.dp, bgColor = TheekHaiColors.Green)
                    Text(t("Priya Sharma", "प्रिया शर्मा"), fontSize = 12.sp, color = ext.ink, fontWeight = FontWeight.Medium)
                    Surface(
                        color = Color(0xFFD6DFD0),
                        shape = RoundedCornerShape(3.dp)
                    ) {
                        Text(
                            text = t("resident", "नागरिक"),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp,
                            color = ext.ink,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Sign Out quiet button
            TextButton(
                onClick = onSignOutClick,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                Text(
                    text = t("Sign out", "साइन आउट"),
                    fontSize = 12.sp,
                    color = ext.muted
                )
            }

            // Quick Report CTA Button (matching .button.small in web/src/style.css)
            Button(
                onClick = onReportClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TheekHaiColors.Green,
                    contentColor = Color(0xFFFFFDF5)
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
                modifier = Modifier.height(39.dp)
            ) {
                Text(
                    text = t("Report an issue  ↗", "समस्या दर्ज करें  ↗"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
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
            .height(52.dp)
            .background(TheekHaiColors.Background)
            .padding(horizontal = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(25.dp)
        ) {
            Text(
                text = t("TheekHai · Civic pilot", "ठीकहै · नागरिक पायलट"),
                fontSize = 11.sp,
                color = ext.muted
            )
            Text(
                text = t("Privacy", "गोपनीयता"),
                fontSize = 11.sp,
                color = ext.green,
                modifier = Modifier.clickable { onNavigate(Route.Library) }
            )
            Text(
                text = t("Terms", "शर्तें"),
                fontSize = 11.sp,
                color = ext.green,
                modifier = Modifier.clickable { onNavigate(Route.Library) }
            )
        }

        Text(
            text = t("Built around accountability.", "जवाबदेही के लिए बनाया गया।"),
            fontSize = 11.sp,
            color = ext.muted
        )
    }
}
