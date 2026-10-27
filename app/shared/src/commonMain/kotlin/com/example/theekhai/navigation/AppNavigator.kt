package com.example.theekhai.navigation

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple state-based navigator — no external dependency needed.
 * Compose recomposes when `currentRoute` changes.
 */
class AppNavigator {
    private val _currentRoute = MutableStateFlow<Route>(Route.Home)
    val currentRoute: StateFlow<Route> = _currentRoute.asStateFlow()

    private val backStack = mutableListOf<Route>()

    fun navigateTo(route: Route) {
        backStack.add(_currentRoute.value)
        _currentRoute.value = route
    }

    fun goBack(): Boolean {
        if (backStack.isEmpty()) return false
        _currentRoute.value = backStack.removeLast()
        return true
    }

    fun resetTo(route: Route) {
        backStack.clear()
        _currentRoute.value = route
    }
}

val LocalNavigator = staticCompositionLocalOf { AppNavigator() }
