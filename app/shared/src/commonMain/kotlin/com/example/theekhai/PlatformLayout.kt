package com.example.theekhai

import androidx.compose.runtime.Composable
import com.example.theekhai.navigation.AppNavigator
import com.example.theekhai.navigation.Route

/**
 * Platform-specific application scaffold.
 *
 * Targets:
 * - Android: Hand-sketched mobile interface with top bar ("Theek Hai" + subtitle + hamburger ≡)
 *   and 5-tab bottom navigation (HOME, MAP, Report (+), WORKSPACE, PROFILE).
 * - JVM (Desktop): Exact mirror of the web app with left persistent sidebar, citizen services 01-05,
 *   operations, topbar with neighborhood label, and civic footer.
 */
@Composable
expect fun PlatformAppScaffold(
    navigator: AppNavigator,
    currentRoute: Route,
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    content: @Composable () -> Unit
)
