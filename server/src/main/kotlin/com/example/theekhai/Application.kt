package com.example.theekhai

import com.example.theekhai.auth.EnvConfig
import com.example.theekhai.auth.SupabaseJwtAuth
import com.example.theekhai.contract.*
import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.di.serverModule
import com.example.theekhai.domain.workflow.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import java.util.UUID

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") { module(EnvConfig.get("THEEKHAI_DB_PATH", "theekhai-server.db")) }.start(wait = true)
}

/** Test identity is injectable only in-process; production has no development login bypass. */
fun Application.module(databasePath: String? = null, testIdentity: ((ApplicationCall) -> Actor)? = null) {
    install(Koin) { modules(serverModule(databasePath)) }
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = false }) }
    val allowedOrigins = EnvConfig.get("THEEKHAI_ALLOWED_ORIGINS", "http://localhost:5173,http://127.0.0.1:5173,http://localhost:8080,http://127.0.0.1:8080").split(',').map { it.trim() }.filter { it.isNotEmpty() }
    fun isAllowedOrigin(origin: String?): Boolean {
        if (origin == null) return true
        if (origin in allowedOrigins) return true
        val uri = runCatching { java.net.URI(origin) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        return uri.scheme in listOf("http", "https") && (host in listOf("localhost", "127.0.0.1") || host.endsWith(".ngrok-free.app") || host.endsWith(".ngrok-free.dev") || host.endsWith(".ngrok.app") || host.endsWith(".ngrok.io"))
    }
    install(CORS) {
        allowedOrigins.forEach { val uri = java.net.URI(it); require(uri.scheme in listOf("http","https") && uri.host != null); allowHost(uri.authority, schemes = listOf(uri.scheme)) }
        allowOrigins { origin -> isAllowedOrigin(origin) }
        allowHeader(HttpHeaders.ContentType); allowHeader(HttpHeaders.Authorization); allowHeader("X-CSRF-Token"); allowHeader("Idempotency-Key")
        allowMethod(HttpMethod.Post); allowMethod(HttpMethod.Delete); allowCredentials = true
    }
    val workflow: Workflow by inject()
    val database: AppDatabase by inject()
    if (databasePath != null && testIdentity == null) {
        seedPilotCasesIfEmpty(database)
    }
    val http = HttpClient(OkHttp) { install(HttpTimeout) { requestTimeoutMillis = 20_000; connectTimeoutMillis = 5_000 }; followRedirects = false }
    val staff = EnvConfig.get("THEEKHAI_STAFF").split(';').filter { it.isNotBlank() }.associate { entry ->
        val parts = entry.split('|'); require(parts.size >= 2); parts[0] to (Role.valueOf(parts[1]) to parts.getOrNull(2))
    }
    val auth = SupabaseJwtAuth(http, EnvConfig.get("SUPABASE_URL"), EnvConfig.get("SUPABASE_PUBLISHABLE_KEY"), staff)
    val legal = com.example.theekhai.legal.LegalService(database, http)
    val cookieSecure = EnvConfig.get("THEEKHAI_COOKIE_SECURE", "false") != "false"
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    scope.launch { while(isActive) { try { synchronized(workflow) { workflow.checkDeadlines() } } catch (e: Exception) { if (e is CancellationException) throw e; log.error("Deadline worker failed: {}",e.javaClass.simpleName) }; delay(60_000) } }
    scope.launch { while(isActive) { delay(24 * 60 * 60_000L); try { legal.runScheduledMonitors() } catch (e: Exception) { if (e is CancellationException) throw e; log.error("Monitor worker failed: {}",e.javaClass.simpleName) } } }
    monitor.subscribe(ApplicationStopped) { scope.cancel(); http.close() }
    suspend fun identity(call: ApplicationCall): Actor = testIdentity?.invoke(call) ?: auth.authenticate(call)
    routing {
        get("/") { call.respondText("THEEKHAI Smart Cities Backend Server") }
        get("/health") {
            val ps = legal.providerStatus(auth.config().configured)
            call.respond(
                mapOf(
                    "status" to "HEALTHY",
                    "version" to "pilot",
                    "supabaseConfigured" to ps.supabaseConfigured.toString(),
                    "tinyFishConfigured" to ps.tinyFishConfigured.toString(),
                    "tavilyConfigured" to ps.tavilyConfigured.toString(),
                    "openRouterConfigured" to ps.openRouterConfigured.toString(),
                    "sarvamConfigured" to ps.sarvamConfigured.toString(),
                )
            )
        }
        get("/api/v1/providers") { safe(call) { call.respond(legal.providerStatus(auth.config().configured)) } }
        get("/api/v1/auth/config") { safe(call) { call.respond(auth.config()) } }
        post("/api/v1/auth/login") { safe(call) {
            val origin = call.request.header(HttpHeaders.Origin)
            if (!isAllowedOrigin(origin)) throw WorkflowFailure("FORBIDDEN","Origin is not allowed.")
            val (id, session) = auth.login(call.receive<LoginDto>())
            call.response.cookies.append(Cookie("theekhai_session",id,httpOnly=true,secure=cookieSecure,path="/",extensions=mapOf("SameSite" to "Lax")))
            call.respond(mapOf("status" to "signed_in", "accessToken" to session.token))
        } }
        post("/api/v1/auth/register") { safe(call) {
            val origin = call.request.header(HttpHeaders.Origin)
            if (!isAllowedOrigin(origin)) throw WorkflowFailure("FORBIDDEN","Origin is not allowed.")
            val (id, session) = auth.register(call.receive<RegisterDto>())
            call.response.cookies.append(Cookie("theekhai_session",id,httpOnly=true,secure=cookieSecure,path="/",extensions=mapOf("SameSite" to "Lax")))
            call.respond(HttpStatusCode.Created, mapOf("status" to "registered", "accessToken" to session.token))
        } }
        post("/api/v1/auth/recover") { safe(call) {
            val origin = call.request.header(HttpHeaders.Origin)
            if (!isAllowedOrigin(origin)) throw WorkflowFailure("FORBIDDEN","Origin is not allowed.")
            auth.recoverPassword(call.receive<RecoverPasswordDto>().email)
            call.respond(mapOf("status" to "recovery_sent"))
        } }
        get("/api/v1/auth/me") { safe(call) {
            if (testIdentity != null) {
                val actor = testIdentity.invoke(call)
                call.respond(IdentityDto(actor.id, actor.role.name, actor.department, auth.csrf(call)))
            } else {
                call.respond(auth.authenticateIdentity(call))
            }
        } }
        post("/api/v1/auth/logout") { safe(call) { identity(call); auth.logout(call); call.response.cookies.append(Cookie("theekhai_session","",maxAge=0,httpOnly=true,path="/",secure=cookieSecure,extensions=mapOf("SameSite" to "Lax"))); call.respond(mapOf("status" to "signed_out")) } }
        get("/api/v1/profile") { safe(call) { call.respond(auth.getProfile(call)) } }
        post("/api/v1/profile") { safe(call) { call.respond(auth.saveProfile(call, call.receive())) } }
        get("/api/v1/saved-sources") { safe(call) { call.respond(auth.listSavedSources(call)) } }
        post("/api/v1/saved-sources") { safe(call) { call.respond(HttpStatusCode.Created, auth.saveBookmark(call, call.receive())) } }
        delete("/api/v1/saved-sources/{id}") { safe(call) { auth.deleteBookmark(call, call.parameters["id"]!!); call.respond(mapOf("status" to "deleted")) } }
        get("/api/v1/complaints") { safe(call) {
            val actor = identity(call); val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val all = synchronized(workflow) { workflow.list(actor) }
            call.respond(CasePageDto(all.drop(offset).take(50).map { it.dto() },all.size,offset))
        } }
        post("/api/v1/complaints/triage") { safe(call) {
            val req = call.receive<TriageAssistRequestDto>()
            val existing = synchronized(workflow) { workflow.list(Actor("system-triage", Role.ADMIN)) }
            call.respond(legal.triageAssist(req, existing))
        } }
        post("/api/v1/complaints") { safe(call) {
            val actor = identity(call); val input = call.receive<ReportDto>(); val key = call.request.header("Idempotency-Key") ?: ""
            val (record, tasks, events) = synchronized(workflow) {
                val r = workflow.submit(actor,ReportInput(input.title,input.description,input.category,input.address,input.latitude,input.longitude,input.consent),key)
                Triple(r, workflow.tasks(actor, r.id), workflow.events(actor, r.id))
            }
            scope.launch { auth.syncCaseToSupabase(call, record, tasks, events) }
            call.respond(HttpStatusCode.Created,record.dto())
        } }
        get("/api/v1/complaints/{id}") { safe(call) {
            val actor = identity(call); val id = call.parameters["id"]!!
            val result = synchronized(workflow) { CaseDetailDto(workflow.detail(actor,id).dto(),workflow.tasks(actor,id).map { TaskDto(it.id,it.caseId,it.department,it.title,it.required,it.complete) },workflow.events(actor,id).map { EventDto(it.id,it.caseId,it.actorId,it.action,it.note,it.at) }) }
            call.respond(result)
        } }
        post("/api/v1/complaints/{id}/actions") { safe(call) {
            val actor = identity(call); val input = call.receive<ActionDto>(); val caseId = call.parameters["id"]!!
            val (result, tasks, events) = synchronized(workflow) {
                val r = workflow.act(actor,caseId,CaseAction(input.action,input.version,input.note,input.target,input.department))
                Triple(r, workflow.tasks(actor, caseId), workflow.events(actor, caseId))
            }
            scope.launch { auth.syncCaseToSupabase(call, result, tasks, events) }
            call.respond(result.dto())
        } }
        get("/api/v1/legal/sources") { safe(call) { call.respond(legal.search(call.request.queryParameters["q"] ?: "",call.request.queryParameters["jurisdiction"] ?: "CENTRAL")) } }
        post("/api/v1/legal/ask") { safe(call) { identity(call); call.respond(legal.ask(call.receive())) } }
        post("/api/v1/legal/sources") { safe(call) { requireReviewer(identity(call)); call.respond(HttpStatusCode.Created,legal.import(call.receive())) } }
        post("/api/v1/legal/sources/{id}/review") { safe(call) { val actor = identity(call); requireReviewer(actor); val request = call.receive<SourceReviewDto>(); legal.review(call.parameters["id"]!!,actor.id,request.approve); call.respond(mapOf("status" to "reviewed")) } }
        get("/api/v1/legal/review") { safe(call) { requireReviewer(identity(call)); call.respond(legal.pending()) } }
        post("/api/v1/legal/discover") { safe(call) { requireReviewer(identity(call)); call.respond(legal.discover(call.receive())) } }
        get("/api/v1/legal/monitors") { safe(call) { requireReviewer(identity(call)); call.respond(legal.listMonitors()) } }
        post("/api/v1/legal/monitors") { safe(call) { requireReviewer(identity(call)); call.respond(HttpStatusCode.Created,legal.createMonitor(call.receive())) } }
        post("/api/v1/legal/monitors/{id}/run") { safe(call) { requireReviewer(identity(call)); call.respond(legal.runMonitor(call.parameters["id"]!!)) } }
        post("/api/v1/voice/transcribe") { safe(call) {
            val req = call.receive<TranscribeRequestDto>(); call.respond(legal.transcribeVoice(req))
            // Voice transcription handled by LegalService.transcribeVoice
            // Also expose GET /api/v1/geo/reverse below
        } }
        get("/api/v1/geo/reverse") { safe(call) {
            val lat = call.request.queryParameters["lat"]?.toDoubleOrNull() ?: throw WorkflowFailure("INVALID_INPUT", "Missing or invalid 'lat' query parameter.")
            val lon = call.request.queryParameters["lon"]?.toDoubleOrNull() ?: throw WorkflowFailure("INVALID_INPUT", "Missing or invalid 'lon' query parameter.")
            call.respond(legal.reverseGeocode(lat, lon, call.request.queryParameters["lang"] ?: "en"))
        } }
    }
}

private fun seedPilotCasesIfEmpty(database: AppDatabase) {
    val q = database.pilotQueries
    val now = System.currentTimeMillis()
    if (q.legalMonitors().executeAsList().isEmpty()) {
        q.transaction {
            q.saveMonitor("mon-pilot-001", "tf-mon-rtsc-01", "Haryana RTSC & AAS Guidelines Page Monitor", "fetch", "https://haryana-rtsc.gov.in/guidelines", "HARYANA", "en", "0 0 * * *", "ACTIVE", now, "NO_CHANGES_DETECTED")
            q.saveMonitor("mon-pilot-002", "tf-mon-ecourts-02", "eCourts & Civic Redressal Topic Monitor", "search", "civic municipal grievance escalation timelines India", "CENTRAL", "en", "0 0 * * *", "ACTIVE", now, "NO_CHANGES_DETECTED")
        }
    }
    if (q.allCases().executeAsList().isNotEmpty()) return
    val hour = 3_600_000L
    val citizenId = "11111111-1111-4111-8111-111111111101"
    val pwdId = "22222222-2222-4222-8222-222222222202"
    q.transaction {
        // Case 1: IN_PROGRESS
        q.saveCase(
            "case-pilot-001",
            "Waterlogged pothole and broken kerb near Ward 4 primary school",
            "Deep pothole holding stagnant rainwater at the main school crossing, creating a hazard for pedestrians and two-wheelers.",
            "ROADS",
            "Main Market Road near Ward 4 Primary School, Ward 4",
            28.4595,
            77.0266,
            citizenId,
            "Public works",
            "HIGH",
            "IN_PROGRESS",
            now - 12 * hour,
            now - 2 * hour,
            now + 60 * hour,
            2L,
            0L,
            null,
        )
        q.saveTask("task-pilot-001", "case-pilot-001", "Public works", "Inspect pothole depth and place safety barricades", 1L, 1L)
        q.saveTask("task-pilot-002", "case-pilot-001", "Public works", "Cold-mix asphalt patching and kerb alignment", 1L, 0L)
        q.appendEvent("evt-pilot-001", "case-pilot-001", citizenId, "SUBMITTED", "Report received; consent recorded.", now - 12 * hour)
        q.appendEvent("evt-pilot-002", "case-pilot-001", "system", "ASSIGNED", "Routed by category to Public works under pilot policy.", now - 12 * hour)
        q.appendEvent("evt-pilot-003", "case-pilot-001", pwdId, "START", "Field crew dispatched with barricades and patching material.", now - 2 * hour)

        // Case 2: ASSIGNED
        q.saveCase(
            "case-pilot-002",
            "Three LED streetlights flickering and dark on Lane 2",
            "Three consecutive street poles near the community park entrance have been dark for two evenings.",
            "LIGHTING",
            "Community Park Entrance, Lane 2, Ward 4",
            28.4612,
            77.0289,
            citizenId,
            "Electrical services",
            "MEDIUM",
            "ASSIGNED",
            now - 6 * hour,
            now - 6 * hour,
            now + 66 * hour,
            1L,
            0L,
            null,
        )
        q.saveTask("task-pilot-003", "case-pilot-002", "Electrical services", "Inspect feeder line and replace LED drivers on poles 14-16", 1L, 0L)
        q.appendEvent("evt-pilot-004", "case-pilot-002", citizenId, "SUBMITTED", "Report received; consent recorded.", now - 6 * hour)

        // Case 3: PENDING_VERIFICATION
        q.saveCase(
            "case-pilot-003",
            "Stormwater drain grate dislodged at Sector 14 crossing",
            "Heavy vehicle traffic shifted the cast-iron stormwater grate, leaving a 40cm gap on the pedestrian edge.",
            "ROADS",
            "Sector 14 Rotary Crossing, Ward 4",
            28.4680,
            77.0310,
            citizenId,
            "Public works",
            "HIGH",
            "PENDING_VERIFICATION",
            now - 24 * hour,
            now - 1 * hour,
            now + 48 * hour,
            3L,
            0L,
            pwdId,
        )
        q.saveTask("task-pilot-004", "case-pilot-003", "Public works", "Re-seat cast-iron grate and lock collar bolts", 1L, 1L)
        q.appendEvent("evt-pilot-005", "case-pilot-003", pwdId, "REQUEST_CLOSURE", "Grate re-seated and locked; photographic inspection uploaded for independent reviewer.", now - 1 * hour)
    }
}

private fun requireReviewer(actor: Actor) { if (actor.role !in setOf(Role.ADMIN,Role.REVIEWER)) throw WorkflowFailure("FORBIDDEN","Reviewer access required.") }
private suspend fun safe(call: ApplicationCall, block: suspend () -> Unit) {
    call.response.header("X-Content-Type-Options","nosniff"); call.response.header("Cache-Control","no-store")
    try { block() } catch(e: CancellationException) { throw e } catch(e: Exception) {
        val code = (e as? WorkflowFailure)?.code ?: if(e is io.ktor.server.plugins.BadRequestException || e is kotlinx.serialization.SerializationException) "INVALID_INPUT" else "INTERNAL_ERROR"
        val status = when(code) { "UNAUTHORIZED" -> HttpStatusCode.Unauthorized; "FORBIDDEN" -> HttpStatusCode.Forbidden; "NOT_FOUND" -> HttpStatusCode.NotFound; "CONFLICT" -> HttpStatusCode.Conflict; "INVALID_INPUT" -> HttpStatusCode.BadRequest; "UNAVAILABLE" -> HttpStatusCode.ServiceUnavailable; else -> HttpStatusCode.InternalServerError }
        call.respond(status,ApiErrorDto(code,(e as? WorkflowFailure)?.message ?: "The request could not be completed.",UUID.randomUUID().toString()))
    }
}
private fun CaseRecord.dto() = CaseDto(id,title,description,category,address,latitude,longitude,citizenId,department,priority,status.name,createdAt,updatedAt,dueAt,version,escalation,closureBy)
