package com.example.theekhai.auth

import com.example.theekhai.contract.LoginDto
import com.example.theekhai.domain.workflow.*
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import kotlinx.serialization.json.*
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/** Online Supabase identity validation. Roles come exclusively from server-managed configuration. */
class SupabaseJwtAuth(private val client: HttpClient, private val url: String, private val key: String, private val staff: Map<String, Pair<Role,String?>> = emptyMap()) {
    data class Session(val token: String, val csrf: String, val expiresAt: Long)
    private val sessions = ConcurrentHashMap<String,Session>()
    suspend fun login(input: LoginDto): Pair<String,Session> {
        if (url.isBlank() || key.isBlank()) throw WorkflowFailure("UNAVAILABLE", "Sign-in is not configured on this server.")
        if (input.email.length !in 3..254 || input.password.length !in 1..256) throw WorkflowFailure("INVALID_INPUT", "Enter your email and password.")
        val response = client.post("$url/auth/v1/token?grant_type=password") {
            header("apikey",key); contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("email",input.email); put("password",input.password) }.toString())
        }
        if (!response.status.isSuccess()) throw WorkflowFailure("UNAUTHORIZED", "Sign-in failed. Check your credentials and email verification.")
        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val token = json["access_token"]?.jsonPrimitive?.content ?: throw WorkflowFailure("UNAUTHORIZED", "No session returned.")
        val expires = json["expires_in"]?.jsonPrimitive?.longOrNull?.coerceIn(1,3600) ?: 3600
        sessions.entries.removeIf { it.value.expiresAt <= System.currentTimeMillis() }
        val id = randomToken(); val session = Session(token,randomToken(),System.currentTimeMillis() + expires*1000)
        sessions[id] = session
        return id to session
    }
    suspend fun authenticate(call: ApplicationCall): Actor {
        val cookie = call.request.cookies["theekhai_session"]
        val session = cookie?.let { sessions[it] }
        val bearer = call.request.header(HttpHeaders.Authorization)?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
        val token = if (cookie != null) {
            if (session == null || session.expiresAt <= System.currentTimeMillis()) { sessions.remove(cookie); throw WorkflowFailure("UNAUTHORIZED", "Session expired. Sign in again.") }
            if (call.request.local.method !in setOf(HttpMethod.Get,HttpMethod.Head,HttpMethod.Options) && call.request.header("X-CSRF-Token") != session.csrf) throw WorkflowFailure("FORBIDDEN", "Invalid request token.")
            session.token
        } else bearer ?: throw WorkflowFailure("UNAUTHORIZED", "Sign in to continue.")
        if (url.isBlank() || key.isBlank()) throw WorkflowFailure("UNAVAILABLE", "Authentication is not configured.")
        val response = client.get("$url/auth/v1/user") { header("apikey",key); bearerAuth(token) }
        if (!response.status.isSuccess()) throw WorkflowFailure("UNAUTHORIZED", "Session could not be verified.")
        val user = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val id = user["id"]?.jsonPrimitive?.content ?: throw WorkflowFailure("UNAUTHORIZED", "Identity missing.")
        if (user["email_confirmed_at"] == null || user["email_confirmed_at"] is JsonNull) throw WorkflowFailure("FORBIDDEN", "Verify your email before continuing.")
        val permission = staff[id]
        return Actor(id,permission?.first ?: Role.CITIZEN,permission?.second)
    }
    fun csrf(call: ApplicationCall) = call.request.cookies["theekhai_session"]?.let { sessions[it]?.csrf }
    fun logout(call: ApplicationCall) { call.request.cookies["theekhai_session"]?.let { sessions.remove(it) } }
    private fun randomToken(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
}
