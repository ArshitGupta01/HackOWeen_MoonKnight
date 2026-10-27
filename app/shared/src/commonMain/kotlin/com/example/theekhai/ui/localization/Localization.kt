package com.example.theekhai.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

enum class AppLanguage(val code: String, val label: String) {
    EN("en", "English"),
    HI("hi", "हिन्दी")
}

val LocalAppLanguage = compositionLocalOf { AppLanguage.EN }

/**
 * Returns English or Hindi string based on active LocalAppLanguage.
 * Exactly matches web app's `t(en, hi)`.
 */
@Composable
fun t(en: String, hi: String): String {
    return if (LocalAppLanguage.current == AppLanguage.EN) en else hi
}
