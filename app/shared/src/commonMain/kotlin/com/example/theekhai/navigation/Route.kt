package com.example.theekhai.navigation

/**
 * All routes in the TheekHai multiplatform app.
 * Mirrors the web app's path structure (/login, /signup, /report, /workspace, /account, /library).
 */
sealed class Route(val path: String) {
    data object Home : Route("/")
    data object Login : Route("/login")
    data object Signup : Route("/signup")
    data object Report : Route("/report")
    data object Workspace : Route("/workspace")
    data object Map : Route("/map")
    data object Library : Route("/library")
    data object Account : Route("/account")
    data object CommandCenter : Route("/admin")
    data object FieldWorker : Route("/field")
    data class CaseDetail(val caseId: String) : Route("/cases/$caseId")
}
