package com.example.theekhai

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
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") { module(System.getenv("THEEKHAI_DB_PATH") ?: "theekhai-server.db") }.start(wait = true)
}

/** Test identity is injectable only in-process; production has no development login bypass. */
fun Application.module(databasePath: String? = null, testIdentity: ((ApplicationCall) -> Actor)? = null) {
    install(Koin) { modules(serverModule(databasePath)) }
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = false; isLenient = false }) }
    val allowedOrigins = (System.getenv("THEEKHAI_ALLOWED_ORIGINS") ?: "http://localhost:5173,http://127.0.0.1:5173").split(',').map { it.trim() }.filter { it.isNotEmpty() }
    install(CORS) {
        allowedOrigins.forEach { val uri = java.net.URI(it); require(uri.scheme in listOf("http","https") && uri.host != null); allowHost(uri.authority, schemes = listOf(uri.scheme)) }
        allowHeader(HttpHeaders.ContentType); allowHeader(HttpHeaders.Authorization); allowHeader("X-CSRF-Token"); allowHeader("Idempotency-Key")
        allowMethod(HttpMethod.Post); allowCredentials = true
    }
    val workflow: Workflow by inject()
    val database: AppDatabase by inject()
    val http = HttpClient(OkHttp) { install(HttpTimeout) { requestTimeoutMillis = 20_000; connectTimeoutMillis = 5_000 }; followRedirects = false }
    val staff = (System.getenv("THEEKHAI_STAFF") ?: "").split(';').filter { it.isNotBlank() }.associate { entry ->
        val parts = entry.split('|'); require(parts.size >= 2); parts[0] to (Role.valueOf(parts[1]) to parts.getOrNull(2))
    }
    val auth = SupabaseJwtAuth(http,System.getenv("SUPABASE_URL") ?: "",System.getenv("SUPABASE_PUBLISHABLE_KEY") ?: "",staff)
    val legal = com.example.theekhai.legal.LegalService(database,http)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    scope.launch { while(isActive) { try { synchronized(workflow) { workflow.checkDeadlines() } } catch (e: Exception) { if (e is CancellationException) throw e; log.error("Deadline worker failed: {}",e.javaClass.simpleName) }; delay(60_000) } }
    scope.launch { while(isActive) { delay(24 * 60 * 60_000L); try { legal.runScheduledMonitors() } catch (e: Exception) { if (e is CancellationException) throw e; log.error("Monitor worker failed: {}",e.javaClass.simpleName) } } }
    monitor.subscribe(ApplicationStopped) { scope.cancel(); http.close() }
    suspend fun identity(call: ApplicationCall): Actor = testIdentity?.invoke(call) ?: auth.authenticate(call)
    routing {
        get("/") { call.respondText("THEEKHAI Smart Cities Backend Server") }
        get("/health") { call.respond(mapOf("status" to "HEALTHY", "version" to "pilot")) }
        post("/api/v1/auth/login") { safe(call) {
            val origin = call.request.header(HttpHeaders.Origin)
            if (origin != null && origin !in allowedOrigins) throw WorkflowFailure("FORBIDDEN","Origin is not allowed.")
            val (id,_) = auth.login(call.receive<LoginDto>())
            call.response.cookies.append(Cookie("theekhai_session",id,httpOnly=true,secure=System.getenv("THEEKHAI_COOKIE_SECURE") != "false",path="/",extensions=mapOf("SameSite" to "Strict")))
            call.respond(mapOf("status" to "signed_in"))
        } }
        get("/api/v1/auth/me") { safe(call) { val actor = identity(call); call.respond(IdentityDto(actor.id,actor.role.name,actor.department,auth.csrf(call))) } }
        post("/api/v1/auth/logout") { safe(call) { identity(call); auth.logout(call); call.response.cookies.append(Cookie("theekhai_session","",maxAge=0,httpOnly=true,path="/",secure=System.getenv("THEEKHAI_COOKIE_SECURE") != "false",extensions=mapOf("SameSite" to "Strict"))); call.respond(mapOf("status" to "signed_out")) } }
        get("/api/v1/complaints") { safe(call) {
            val actor = identity(call); val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val all = synchronized(workflow) { workflow.list(actor) }
            call.respond(CasePageDto(all.drop(offset).take(50).map { it.dto() },all.size,offset))
        } }
        post("/api/v1/complaints") { safe(call) {
            val actor = identity(call); val input = call.receive<ReportDto>(); val key = call.request.header("Idempotency-Key") ?: ""
            val record = synchronized(workflow) { workflow.submit(actor,ReportInput(input.title,input.description,input.category,input.address,input.latitude,input.longitude,input.consent),key) }
            call.respond(HttpStatusCode.Created,record.dto())
        } }
        get("/api/v1/complaints/{id}") { safe(call) {
            val actor = identity(call); val id = call.parameters["id"]!!
            val result = synchronized(workflow) { CaseDetailDto(workflow.detail(actor,id).dto(),workflow.tasks(actor,id).map { TaskDto(it.id,it.caseId,it.department,it.title,it.required,it.complete) },workflow.events(actor,id).map { EventDto(it.id,it.caseId,it.actorId,it.action,it.note,it.at) }) }
            call.respond(result)
        } }
        post("/api/v1/complaints/{id}/actions") { safe(call) {
            val actor = identity(call); val input = call.receive<ActionDto>()
            val result = synchronized(workflow) { workflow.act(actor,call.parameters["id"]!!,CaseAction(input.action,input.version,input.note,input.target,input.department)) }
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
            val req = call.receive<TranscribeRequestDto>()
            if (req.audioBase64.length !in 16..5_000_000) throw WorkflowFailure("INVALID_INPUT","Audio recording is empty or exceeds size limit.")
            call.respond(TranscribeResponseDto("Civic issue reported via voice note (${req.mimeType}); please review and edit details before submitting."))
        } }
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
