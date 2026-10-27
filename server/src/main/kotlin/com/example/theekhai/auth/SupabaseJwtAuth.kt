package com.example.theekhai.auth

import com.example.theekhai.contract.*
import com.example.theekhai.domain.workflow.*
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import kotlinx.serialization.json.*
import java.io.File
import java.net.URI
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

object EnvConfig {
    private val dotenv: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        listOf(File(".env"), File("../.env")).filter { it.isFile }.forEach { file ->
            file.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && '=' in trimmed) {
                    val key = trimmed.substringBefore('=').trim()
                    val value = trimmed.substringAfter('=').trim().removeSurrounding("\"").removeSurrounding("'")
                    if (key.isNotEmpty() && value.isNotEmpty() && key !in map) {
                        map[key] = value
                    }
                }
            }
        }
        map
    }

    fun get(key: String, default: String = ""): String =
        System.getenv(key)?.takeIf { it.isNotBlank() } ?: dotenv[key] ?: default
}

/**
 * Online Supabase Auth + PostgreSQL PostgREST RLS service (inspired by TenderLens).
 * Connects to Supabase project `TheekHai` (`ffmcurazxvygsifbglwy`):
 * - Verifies credentials & sessions via `/auth/v1/...`
 * - Resolves and syncs user profiles (`public.profiles`), bookmarked sources (`public.saved_sources`),
 *   and workflow tables (`public.complaints`, `public.case_tasks`, `public.audit_logs`)
 *   via PostgREST (`/rest/v1/...`) scoped to the user's JWT Bearer token so PostgreSQL RLS policies apply.
 */
class SupabaseJwtAuth(
    private val client: HttpClient,
    rawUrl: String,
    rawKey: String,
    private val staff: Map<String, Pair<Role, String?>> = emptyMap(),
) {
    val url: String = rawUrl.ifBlank {
        EnvConfig.get("SUPABASE_URL", "https://ffmcurazxvygsifbglwy.supabase.co")
    }.trimEnd('/')

    val key: String = rawKey.ifBlank {
        EnvConfig.get("SUPABASE_PUBLISHABLE_KEY", EnvConfig.get("SUPABASE_ANON_KEY", "sb_publishable_tmyZa6z3AUsdo82lh9HkRQ_Oet3XWay"))
    }.trim()

    private val json = Json { ignoreUnknownKeys = true }

    data class Session(val token: String, val csrf: String, val expiresAt: Long)
    data class VerifiedUser(
        val actor: Actor,
        val token: String,
        val email: String,
        val fullName: String,
        val organization: String,
        val ward: String,
        val preferredLanguage: String,
    )

    private val sessions = ConcurrentHashMap<String, Session>()

    suspend fun login(input: LoginDto): Pair<String, Session> {
        if (url.isBlank() || key.isBlank()) throw WorkflowFailure("UNAVAILABLE", "Sign-in is not configured on this server.")
        val cleanEmail = input.email.trim()
        if (cleanEmail.length !in 3..254 || input.password.length !in 1..256) {
            throw WorkflowFailure("INVALID_INPUT", "Enter your email and password.")
        }
        val response = client.post("$url/auth/v1/token?grant_type=password") {
            header("apikey", key)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("email", cleanEmail); put("password", input.password) }.toString())
        }
        if (!response.status.isSuccess()) {
            throw WorkflowFailure("UNAUTHORIZED", "Sign-in failed. Check your email and password.")
        }
        return createSessionFromAuthJson(response.bodyAsText())
    }

    suspend fun register(input: RegisterDto): Pair<String, Session> {
        if (url.isBlank() || key.isBlank()) throw WorkflowFailure("UNAVAILABLE", "Registration is not configured on this server.")
        val cleanEmail = input.email.trim()
        val cleanName = input.fullName.trim()
        val cleanOrg = input.organization.trim().ifBlank { "Citizen Resident" }.take(100)
        val cleanWard = input.ward.trim().ifBlank { "Ward 1" }.take(80)
        val cleanLang = if (input.preferredLanguage in setOf("en", "hi")) input.preferredLanguage else "en"
        if (cleanEmail.length !in 3..254 || !cleanEmail.contains("@")) {
            throw WorkflowFailure("INVALID_INPUT", "Enter a valid email address.")
        }
        if (input.password.length !in 8..256) {
            throw WorkflowFailure("INVALID_INPUT", "Password must be at least 8 characters.")
        }
        if (cleanName.length !in 2..100) {
            throw WorkflowFailure("INVALID_INPUT", "Enter your full name (2 to 100 characters).")
        }

        val response = client.post("$url/auth/v1/signup") {
            header("apikey", key)
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("email", cleanEmail)
                    put("password", input.password)
                    put(
                        "data",
                        buildJsonObject {
                            put("full_name", cleanName)
                            put("organization", cleanOrg)
                            put("ward", cleanWard)
                            put("preferred_language", cleanLang)
                        },
                    )
                }.toString()
            )
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val errMsg = runCatching {
                val obj = json.parseToJsonElement(body).jsonObject
                obj["msg"]?.jsonPrimitive?.contentOrNull
                    ?: obj["error_description"]?.jsonPrimitive?.contentOrNull
                    ?: obj["message"]?.jsonPrimitive?.contentOrNull
            }.getOrNull() ?: "Could not create account."
            throw WorkflowFailure("INVALID_INPUT", errMsg)
        }
        val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        if (parsed != null && parsed["access_token"]?.jsonPrimitive?.contentOrNull != null) {
            return createSessionFromAuthJson(body)
        }
        return login(LoginDto(cleanEmail, input.password))
    }

    suspend fun recoverPassword(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.length !in 3..254 || !cleanEmail.contains("@")) {
            throw WorkflowFailure("INVALID_INPUT", "Enter your email address first.")
        }
        client.post("$url/auth/v1/recover") {
            header("apikey", key)
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject { put("email", cleanEmail) }.toString())
        }
    }

    suspend fun authenticate(call: ApplicationCall): Actor = verifyCall(call).actor

    suspend fun authenticateIdentity(call: ApplicationCall): IdentityDto {
        val verified = verifyCall(call)
        return IdentityDto(
            id = verified.actor.id,
            role = verified.actor.role.name,
            department = verified.actor.department,
            csrf = csrf(call),
            email = verified.email,
            fullName = verified.fullName,
            organization = verified.organization,
            ward = verified.ward,
            preferredLanguage = verified.preferredLanguage,
        )
    }

    suspend fun getProfile(call: ApplicationCall): ProfileDto {
        val verified = verifyCall(call)
        return ProfileDto(
            userId = verified.actor.id,
            email = verified.email,
            fullName = verified.fullName,
            organization = verified.organization,
            role = verified.actor.role.name,
            department = verified.actor.department,
            ward = verified.ward,
            preferredLanguage = verified.preferredLanguage,
        )
    }

    suspend fun saveProfile(call: ApplicationCall, input: ProfileDto): ProfileDto {
        val verified = verifyCall(call)
        val cleanName = input.fullName.trim().take(100)
        val cleanOrg = input.organization.trim().take(100)
        val cleanWard = input.ward.trim().ifBlank { "Ward 1" }.take(80)
        val cleanLang = if (input.preferredLanguage in setOf("en", "hi")) input.preferredLanguage else "en"
        if (cleanName.length < 2) throw WorkflowFailure("INVALID_INPUT", "Enter your name (at least 2 characters).")

        val response = client.post("$url/rest/v1/profiles?on_conflict=user_id") {
            header("apikey", key)
            bearerAuth(verified.token)
            header("Prefer", "resolution=merge-duplicates,return=representation")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("user_id", verified.actor.id)
                    put("email", verified.email)
                    put("full_name", cleanName)
                    put("organization", cleanOrg)
                    put("ward", cleanWard)
                    put("preferred_language", cleanLang)
                }.toString()
            )
        }
        if (!response.status.isSuccess()) {
            throw WorkflowFailure("UNAVAILABLE", "Could not sync profile to Supabase PostgreSQL.")
        }
        return ProfileDto(
            userId = verified.actor.id,
            email = verified.email,
            fullName = cleanName,
            organization = cleanOrg,
            role = verified.actor.role.name,
            department = verified.actor.department,
            ward = cleanWard,
            preferredLanguage = cleanLang,
        )
    }

    suspend fun listSavedSources(call: ApplicationCall): List<SavedSourceDto> {
        val verified = verifyCall(call)
        val response = client.get("$url/rest/v1/saved_sources?user_id=eq.${verified.actor.id}&order=created_at.desc") {
            header("apikey", key)
            bearerAuth(verified.token)
        }
        if (!response.status.isSuccess()) return emptyList()
        val arr = runCatching { json.parseToJsonElement(response.bodyAsText()).jsonArray }.getOrNull() ?: return emptyList()
        return arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            SavedSourceDto(
                id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                userId = obj["user_id"]?.jsonPrimitive?.contentOrNull ?: verified.actor.id,
                title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Reference source",
                sourceUrl = obj["source_url"]?.jsonPrimitive?.contentOrNull ?: "",
                jurisdiction = obj["jurisdiction"]?.jsonPrimitive?.contentOrNull ?: "CENTRAL",
                snippet = obj["snippet"]?.jsonPrimitive?.contentOrNull ?: "",
                createdAt = obj["created_at"]?.jsonPrimitive?.contentOrNull ?: "",
            )
        }
    }

    suspend fun saveBookmark(call: ApplicationCall, input: SaveSourceBookmarkDto): SavedSourceDto {
        val verified = verifyCall(call)
        val uri = runCatching { URI(input.sourceUrl.trim()) }.getOrNull()
        if (uri == null || uri.scheme !in setOf("https", "http")) {
            throw WorkflowFailure("INVALID_INPUT", "Source does not have a valid official URL to save.")
        }
        val cleanTitle = input.title.trim().ifBlank { "Official civic reference" }.take(250)
        val cleanJurisdiction = if (input.jurisdiction in setOf("CENTRAL", "HARYANA")) input.jurisdiction else "CENTRAL"
        val cleanSnippet = input.snippet.trim().take(1500)

        val response = client.post("$url/rest/v1/saved_sources?on_conflict=user_id,source_url") {
            header("apikey", key)
            bearerAuth(verified.token)
            header("Prefer", "resolution=merge-duplicates,return=representation")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("user_id", verified.actor.id)
                    put("title", cleanTitle)
                    put("source_url", uri.toString().take(2048))
                    put("jurisdiction", cleanJurisdiction)
                    put("snippet", cleanSnippet)
                }.toString()
            )
        }
        if (!response.status.isSuccess()) {
            throw WorkflowFailure("UNAVAILABLE", "Could not save reference source to your account.")
        }
        val obj = runCatching { json.parseToJsonElement(response.bodyAsText()).jsonArray.firstOrNull()?.jsonObject }.getOrNull()
        return SavedSourceDto(
            id = obj?.get("id")?.jsonPrimitive?.contentOrNull ?: "",
            userId = verified.actor.id,
            title = cleanTitle,
            sourceUrl = uri.toString(),
            jurisdiction = cleanJurisdiction,
            snippet = cleanSnippet,
            createdAt = obj?.get("created_at")?.jsonPrimitive?.contentOrNull ?: "",
        )
    }

    suspend fun deleteBookmark(call: ApplicationCall, id: String) {
        val verified = verifyCall(call)
        val cleanId = id.trim()
        if (cleanId.isBlank()) throw WorkflowFailure("INVALID_INPUT", "Bookmark ID is required.")
        client.delete("$url/rest/v1/saved_sources?id=eq.$cleanId&user_id=eq.${verified.actor.id}") {
            header("apikey", key)
            bearerAuth(verified.token)
        }
    }

    /**
     * Best-effort synchronization of local SQLDelight workflow changes to Supabase PostgreSQL
     * (`public.complaints`, `public.case_tasks`, `public.audit_logs`) using the authenticated user's JWT.
     */
    suspend fun syncCaseToSupabase(
        call: ApplicationCall,
        record: CaseRecord,
        tasks: List<CaseTask>,
        events: List<CaseEvent>,
    ) {
        val token = extractToken(call) ?: return
        if (url.isBlank() || key.isBlank()) return
        runCatching {
            client.post("$url/rest/v1/complaints?on_conflict=id") {
                header("apikey", key)
                bearerAuth(token)
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("id", record.id)
                        put("citizen_id", record.citizenId)
                        put("title", record.title)
                        put("description", record.description)
                        put("category", record.category)
                        put("address", record.address)
                        record.latitude?.let { put("latitude", it) } ?: put("latitude", JsonNull)
                        record.longitude?.let { put("longitude", it) } ?: put("longitude", JsonNull)
                        put("department", record.department)
                        put("priority", record.priority)
                        put("status", record.status.name)
                        put("created_at", record.createdAt)
                        put("updated_at", record.updatedAt)
                        put("due_at", record.dueAt)
                        put("version", record.version)
                        put("escalation", record.escalation)
                        record.closureBy?.let { put("closure_by", it) } ?: put("closure_by", JsonNull)
                    }.toString()
                )
            }
            for (t in tasks) {
                client.post("$url/rest/v1/case_tasks?on_conflict=id") {
                    header("apikey", key)
                    bearerAuth(token)
                    header("Prefer", "resolution=merge-duplicates")
                    contentType(ContentType.Application.Json)
                    setBody(
                        buildJsonObject {
                            put("id", t.id)
                            put("case_id", t.caseId)
                            put("department", t.department)
                            put("title", t.title)
                            put("required", t.required)
                            put("complete", t.complete)
                        }.toString()
                    )
                }
            }
            for (ev in events) {
                client.post("$url/rest/v1/audit_logs?on_conflict=id") {
                    header("apikey", key)
                    bearerAuth(token)
                    header("Prefer", "resolution=merge-duplicates")
                    contentType(ContentType.Application.Json)
                    setBody(
                        buildJsonObject {
                            put("id", ev.id)
                            put("case_id", ev.caseId)
                            put("actor_id", ev.actorId)
                            put("action", ev.action)
                            put("note", ev.note)
                            put("at", ev.at)
                        }.toString()
                    )
                }
            }
        }
    }

    private fun extractToken(call: ApplicationCall): String? {
        val cookie = call.request.cookies["theekhai_session"]
        val session = cookie?.let { sessions[it] }
        if (session != null && session.expiresAt > System.currentTimeMillis()) {
            return session.token
        }
        return call.request.header(HttpHeaders.Authorization)
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()
    }

    private suspend fun verifyCall(call: ApplicationCall): VerifiedUser {
        val cookie = call.request.cookies["theekhai_session"]
        val session = cookie?.let { sessions[it] }
        val bearer = call.request.header(HttpHeaders.Authorization)
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()
        val token = if (cookie != null && session != null && session.expiresAt > System.currentTimeMillis()) {
            if (call.request.local.method !in setOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options) &&
                call.request.header("X-CSRF-Token") != session.csrf
            ) {
                throw WorkflowFailure("FORBIDDEN", "Invalid request token.")
            }
            session.token
        } else {
            if (cookie != null) sessions.remove(cookie)
            bearer ?: throw WorkflowFailure("UNAUTHORIZED", if (cookie != null) "Session expired. Sign in again." else "Sign in to continue.")
        }
        if (url.isBlank() || key.isBlank()) throw WorkflowFailure("UNAVAILABLE", "Authentication is not configured.")
        val response = client.get("$url/auth/v1/user") {
            header("apikey", key)
            bearerAuth(token)
        }
        if (!response.status.isSuccess()) throw WorkflowFailure("UNAUTHORIZED", "Session could not be verified.")
        val user = json.parseToJsonElement(response.bodyAsText()).jsonObject
        val id = user["id"]?.jsonPrimitive?.content ?: throw WorkflowFailure("UNAUTHORIZED", "Identity missing.")
        if (user["email_confirmed_at"] == null || user["email_confirmed_at"] is JsonNull) {
            throw WorkflowFailure("FORBIDDEN", "Verify your email before continuing.")
        }
        val email = user["email"]?.jsonPrimitive?.contentOrNull ?: ""
        val meta = user["user_metadata"] as? JsonObject
        val fallbackName = meta?.get("full_name")?.jsonPrimitive?.contentOrNull ?: email.substringBefore('@')
        val fallbackOrg = meta?.get("organization")?.jsonPrimitive?.contentOrNull ?: "Citizen Resident"
        val fallbackWard = meta?.get("ward")?.jsonPrimitive?.contentOrNull ?: "Ward 1"
        val fallbackLang = meta?.get("preferred_language")?.jsonPrimitive?.contentOrNull ?: "en"

        // Fetch RLS-protected profile from Supabase PostgreSQL `public.profiles`
        val profileObj = fetchProfileRow(id, token)
        val dbRole = profileObj?.get("role")?.jsonPrimitive?.contentOrNull
            ?.let { runCatching { Role.valueOf(it) }.getOrNull() }
        val dbDept = profileObj?.get("department")?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val fullName = profileObj?.get("full_name")?.jsonPrimitive?.contentOrNull?.ifBlank { fallbackName } ?: fallbackName
        val organization = profileObj?.get("organization")?.jsonPrimitive?.contentOrNull?.ifBlank { fallbackOrg } ?: fallbackOrg
        val ward = profileObj?.get("ward")?.jsonPrimitive?.contentOrNull?.ifBlank { fallbackWard } ?: fallbackWard
        val preferredLang = profileObj?.get("preferred_language")?.jsonPrimitive?.contentOrNull?.ifBlank { fallbackLang } ?: fallbackLang

        val staffOverride = staff[id] ?: staff[email]
        val finalRole = staffOverride?.first ?: dbRole ?: Role.CITIZEN
        val finalDept = staffOverride?.second ?: dbDept
        return VerifiedUser(
            actor = Actor(id, finalRole, finalDept),
            token = token,
            email = email,
            fullName = fullName,
            organization = organization,
            ward = ward,
            preferredLanguage = preferredLang,
        )
    }

    private suspend fun fetchProfileRow(userId: String, token: String): JsonObject? {
        return try {
            val resp = client.get("$url/rest/v1/profiles?user_id=eq.$userId&select=*") {
                header("apikey", key)
                bearerAuth(token)
            }
            if (!resp.status.isSuccess()) return null
            json.parseToJsonElement(resp.bodyAsText()).jsonArray.firstOrNull() as? JsonObject
        } catch (_: Exception) {
            null
        }
    }

    private fun createSessionFromAuthJson(bodyText: String): Pair<String, Session> {
        val obj = json.parseToJsonElement(bodyText).jsonObject
        val token = obj["access_token"]?.jsonPrimitive?.contentOrNull
            ?: throw WorkflowFailure("UNAUTHORIZED", "No session returned.")
        val expires = obj["expires_in"]?.jsonPrimitive?.longOrNull?.coerceIn(1, 3600) ?: 3600
        sessions.entries.removeIf { it.value.expiresAt <= System.currentTimeMillis() }
        val id = randomToken()
        val session = Session(token, randomToken(), System.currentTimeMillis() + expires * 1000)
        sessions[id] = session
        return id to session
    }

    fun csrf(call: ApplicationCall) = call.request.cookies["theekhai_session"]?.let { sessions[it]?.csrf }
    fun logout(call: ApplicationCall) { call.request.cookies["theekhai_session"]?.let { sessions.remove(it) } }
    private fun randomToken(): String = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(32).also { SecureRandom().nextBytes(it) })
}
