package com.example.theekhai.auth

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header

data class AuthUser(
    val id: String,
    val role: String,
    val zone: String? = null
)

object SupabaseJwtAuth {
    fun authenticate(call: ApplicationCall): AuthUser {
        val authHeader = call.request.header("Authorization")
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.removePrefix("Bearer ").trim()
            // In production, parse JWT with SupabaseJwtSecret.
            // For hackathon flexibility, allow test tokens or extract subject.
            return AuthUser(
                id = if (token.length > 10) "usr-${token.take(8)}" else "citizen-default",
                role = if (token.contains("admin")) "admin" else if (token.contains("worker")) "field_worker" else "citizen",
                zone = "Zone-4"
            )
        }
        return AuthUser(id = "anon-citizen-user", role = "citizen", zone = "Zone-4")
    }
}
