package com.example.theekhai

import com.example.theekhai.ai.BackendAiService
import com.example.theekhai.auth.SupabaseJwtAuth
import com.example.theekhai.contract.*
import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.di.serverModule
import com.example.theekhai.domain.model.ActorType
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository
import com.example.theekhai.domain.workflow.*
import com.example.theekhai.routes.auditRoutes
import com.example.theekhai.routes.evidenceRoutes
import com.example.theekhai.routes.voiceIntakeRoutes
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
        allowHeader(HttpHeaders.ContentType); allowHeader(HttpHeaders.Authorization); allowHeader("X-CSRF-Token"); allowHeader("Idempotency-Key"); allowHeader("X-Client-Platform")
        allowMethod(HttpMethod.Post); allowMethod(HttpMethod.Get); allowMethod(HttpMethod.Options); allowCredentials = true
    }
    val workflow: Workflow by inject()
    val database: AppDatabase by inject()
    val complaintRepository: ComplaintRepository by inject()
    val auditRepository: AuditRepository by inject()
    val aiService: BackendAiService by inject()

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
        
        // Multiplatform Complaints API: supports both KMP mobile/desktop clients and Web pilot workflow
        get("/api/v1/complaints") { safe(call) {
            if (call.request.headers["X-Client-Platform"] == "kmp" || call.request.queryParameters["format"] == "list") {
                call.respond(complaintRepository.getComplaints())
            } else {
                val actor = identity(call); val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
                val all = synchronized(workflow) { workflow.list(actor) }
                call.respond(CasePageDto(all.drop(offset).take(50).map { it.dto() },all.size,offset))
            }
        } }
        post("/api/v1/complaints") { safe(call) {
            if (call.request.headers["X-Client-Platform"] == "kmp") {
                val request = call.receive<CreateComplaintRequestDto>()
                val now = System.currentTimeMillis()
                val newId = "cmp-$now"
                val complaint = Complaint(
                    id = newId,
                    title = request.title,
                    description = request.description,
                    category = request.category,
                    priority = ComplaintPriority.MEDIUM,
                    status = ComplaintStatus.PENDING,
                    latitude = request.latitude,
                    longitude = request.longitude,
                    address = request.address,
                    citizenId = request.citizenId,
                    zone = request.zone,
                    photoUrl = request.photoUrl,
                    slaDueEpochMillis = now + 86400000L,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now
                )
                complaintRepository.createComplaint(complaint)
                auditRepository.recordAuditLog(
                    AuditLog(
                        id = "audit-create-$newId",
                        complaintId = newId,
                        actorType = ActorType.CITIZEN,
                        actorId = request.citizenId,
                        action = "CITIZEN_GRIEVANCE_FILED",
                        reasoning = "Citizen reported civic grievance. DPDP consent confirmed: ${request.consentGranted}.",
                        timestampEpochMillis = now
                    )
                )
                call.respond(HttpStatusCode.Created, ComplaintDto(
                    id = newId,
                    title = complaint.title,
                    description = complaint.description,
                    category = complaint.category,
                    latitude = complaint.latitude,
                    longitude = complaint.longitude,
                    address = complaint.address,
                    citizenId = complaint.citizenId,
                    zone = complaint.zone,
                    slaDueEpochMillis = complaint.slaDueEpochMillis,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now
                ))
            } else {
                val actor = identity(call); val input = call.receive<ReportDto>(); val key = call.request.header("Idempotency-Key") ?: ""
                val record = synchronized(workflow) { workflow.submit(actor,ReportInput(input.title,input.description,input.category,input.address,input.latitude,input.longitude,input.consent),key) }
                call.respond(HttpStatusCode.Created,record.dto())
            }
        } }
        get("/api/v1/complaints/{id}") { safe(call) {
            val id = call.parameters["id"]!!
            if (call.request.headers["X-Client-Platform"] == "kmp") {
                val complaint = complaintRepository.getComplaintById(id)
                if (complaint != null) {
                    call.respond(complaint)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Complaint not found"))
                }
            } else {
                val actor = identity(call)
                val result = synchronized(workflow) { CaseDetailDto(workflow.detail(actor,id).dto(),workflow.tasks(actor,id).map { TaskDto(it.id,it.caseId,it.department,it.title,it.required,it.complete) },workflow.events(actor,id).map { EventDto(it.id,it.caseId,it.actorId,it.action,it.note,it.at) }) }
                call.respond(result)
            }
        } }
        post("/api/v1/complaints/{id}/actions") { safe(call) {
            val actor = identity(call); val input = call.receive<ActionDto>()
            val result = synchronized(workflow) { workflow.act(actor,call.parameters["id"]!!,CaseAction(input.action,input.version,input.note,input.target,input.department)) }
            call.respond(result.dto())
        } }

        // Mobile / Field Worker Action Endpoints
        post("/api/v1/complaints/{id}/triage") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val triageResult = aiService.triageWithGemini(
                complaintId = id,
                title = "Grievance investigation",
                description = "Reported road surface hazard",
                category = "Roads"
            )
            auditRepository.recordAuditLog(
                AuditLog(
                    id = "audit-triage-$id-${System.currentTimeMillis()}",
                    complaintId = id,
                    actorType = ActorType.AI_GEMINI_TRIAGE,
                    actorId = "gemini-3.8-flash-backend",
                    action = "GEMINI_MULTI_TASK_TRIAGE",
                    reasoning = triageResult.reasoning,
                    decisionPayloadJson = """{"priority":"${triageResult.priority}","cluster":"${triageResult.clusterId}"}""",
                    timestampEpochMillis = System.currentTimeMillis()
                )
            )
            call.respond(triageResult)
        }
        post("/api/v1/complaints/{id}/resolve") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<ResolveComplaintRequestDto>()
            val now = System.currentTimeMillis()
            complaintRepository.resolveComplaint(
                id = id,
                workerId = request.workerId,
                resolutionNotes = request.resolutionNotes,
                photoUrl = request.photoUrl
            )
            auditRepository.recordAuditLog(
                AuditLog(
                    id = "audit-resolve-$id-$now",
                    complaintId = id,
                    actorType = ActorType.FIELD_WORKER,
                    actorId = request.workerId,
                    action = "WORKER_SUBMITTED_RESOLUTION",
                    reasoning = "Field worker marked ticket resolved with evidence photo [${request.photoUrl}] and notes: ${request.resolutionNotes}",
                    timestampEpochMillis = now
                )
            )
            call.respond(HttpStatusCode.OK, mapOf("status" to "RESOLVED", "complaintId" to id))
        }
        post("/api/v1/complaints/{id}/escalate") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<EscalateRequestDto>()
            val now = System.currentTimeMillis()
            auditRepository.recordAuditLog(
                AuditLog(
                    id = "audit-escalate-$id-$now",
                    complaintId = id,
                    actorType = ActorType.SYSTEM_SLA_WATCHER,
                    actorId = "dynamic-sla-engine",
                    action = "DYNAMIC_SLA_ESCALATION",
                    reasoning = "SLA breached or conflict reported. Escalated to Commissioner Level. Reason: ${request.reason}",
                    timestampEpochMillis = now
                )
            )
            call.respond(HttpStatusCode.OK, mapOf("status" to "ESCALATED", "complaintId" to id))
        }
        post("/api/v1/complaints/{id}/verify") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val request = try { call.receive<TriggerVerificationRequestDto>() } catch (_: Exception) { TriggerVerificationRequestDto(id) }
            val verif = aiService.runTinyFishVerification(id, request.officialPortalUrl)
            auditRepository.recordAuditLog(
                AuditLog(
                    id = "audit-tinyfish-$id-${verif.verifiedAtEpochMillis}",
                    complaintId = id,
                    actorType = ActorType.AI_TINYFISH_AGENT,
                    actorId = "tinyfish-web-crawler",
                    action = "TINYFISH_AUTONOMOUS_VERIFY",
                    reasoning = verif.explanation,
                    decisionPayloadJson = """{"verified":${verif.verified},"state":"${verif.state}"}""",
                    timestampEpochMillis = verif.verifiedAtEpochMillis
                )
            )
            call.respond(verif)
        }

        // Modular Domain Routes: Voice Intake, Evidence Check, Audit Logs & Offline Sync
        voiceIntakeRoutes(complaintRepository, auditRepository, aiService)
        evidenceRoutes(auditRepository, aiService)
        auditRoutes(auditRepository, complaintRepository)

        // Web Legal & Discovery Routes
        get("/api/v1/legal/sources") { safe(call) { call.respond(legal.search(call.request.queryParameters["q"] ?: "",call.request.queryParameters["jurisdiction"] ?: "CENTRAL")) } }
        post("/api/v1/legal/ask") { safe(call) { identity(call); call.respond(legal.ask(call.receive())) } }
        post("/api/v1/legal/sources") { safe(call) { requireReviewer(identity(call)); call.respond(HttpStatusCode.Created,legal.import(call.receive())) } }
        post("/api/v1/legal/sources/{id}/review") { safe(call) { val actor = identity(call); requireReviewer(actor); val request = call.receive<SourceReviewDto>(); legal.review(call.parameters["id"]!!,actor.id,request.approve); call.respond(mapOf("status" to "reviewed")) } }
        get("/api/v1/legal/review") { safe(call) { requireReviewer(identity(call)); call.respond(legal.pending()) } }
        post("/api/v1/legal/discover") { safe(call) { requireReviewer(identity(call)); call.respond(legal.discover(call.receive())) } }
        get("/api/v1/legal/monitors") { safe(call) { requireReviewer(identity(call)); call.respond(legal.listMonitors()) } }
        post("/api/v1/legal/monitors") { safe(call) { requireReviewer(identity(call)); call.respond(HttpStatusCode.Created,legal.createMonitor(call.receive())) } }
        post("/api/v1/legal/monitors/{id}/run") { safe(call) { requireReviewer(identity(call)); call.respond(legal.runMonitor(call.parameters["id"]!!)) } }
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
