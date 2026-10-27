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
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = false; encodeDefaults = true }) }
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
    if (testIdentity == null) {
        seedPilotCasesIfEmpty(database)
    }
    val http = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 60_000
        }
        followRedirects = true
    }
    val staff = EnvConfig.get("THEEKHAI_STAFF").split(';').filter { it.isNotBlank() }.associate { entry ->
        val parts = entry.split('|'); require(parts.size >= 2); parts[0] to (Role.valueOf(parts[1]) to parts.getOrNull(2))
    }
    val auth = SupabaseJwtAuth(http, EnvConfig.get("SUPABASE_URL"), EnvConfig.get("SUPABASE_PUBLISHABLE_KEY"), staff, database)
    val legal = com.example.theekhai.legal.LegalService(database, http)
    val gmail = com.example.theekhai.mail.GmailService(http)
    val cookieSecure = EnvConfig.get("THEEKHAI_COOKIE_SECURE", "false") != "false"
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    scope.launch { while(isActive) { try { synchronized(workflow) { workflow.checkDeadlines() } } catch (e: Exception) { if (e is CancellationException) throw e; log.error("Deadline worker failed: {}",e.javaClass.simpleName) }; delay(60_000) } }
    scope.launch { while(isActive) { delay(24 * 60 * 60_000L); try { legal.runScheduledMonitors() } catch (e: Exception) { if (e is CancellationException) throw e; log.error("Monitor worker failed: {}",e.javaClass.simpleName) } } }
    monitor.subscribe(ApplicationStopped) { scope.cancel(); http.close() }
    suspend fun identity(call: ApplicationCall): Actor = testIdentity?.invoke(call) ?: auth.authenticate(call)
    routing {
        get("/") { call.respondText("THEEKHAI Smart Cities Backend Server") }
        get("/api/v1/auth/dummy-users") { safe(call) {
            val users = database.pilotQueries.allPilotUsers().executeAsList().map { u ->
                val isAuthority = u.role in setOf("DEPARTMENT", "REVIEWER", "ADMIN")
                DummyUserCredentialDto(
                    id = u.id,
                    shortName = u.fullName.substringBefore(' '),
                    fullName = u.fullName,
                    email = u.email,
                    password = u.passwordHash,
                    role = u.role,
                    userType = if (isAuthority) "AUTHORITY" else "REGULAR",
                    department = u.department,
                    ward = u.ward,
                    score = u.baseScore.toInt()
                )
            }
            call.respond(users)
        } }

        post("/api/v1/auth/instant-login") { safe(call) {
            val origin = call.request.header(HttpHeaders.Origin)
            if (!isAllowedOrigin(origin)) throw WorkflowFailure("FORBIDDEN", "Origin is not allowed.")
            val req = call.receive<InstantLoginDto>()
            val (id, session) = auth.instantLogin(req.email)
            call.response.cookies.append(
                Cookie("theekhai_session", id, httpOnly = true, secure = cookieSecure, path = "/", extensions = mapOf("SameSite" to "Lax"))
            )
            call.respond(mapOf("status" to "signed_in", "accessToken" to session.token))
        } }

        get("/api/v1/leaderboard") { safe(call) {
            val roleFilter = call.request.queryParameters["role"]?.uppercase() ?: "ALL"
            val allUsers = database.pilotQueries.allPilotUsers().executeAsList()
            val userMap = allUsers.associateBy { it.id }

            val filtered = allUsers.filter { u ->
                val isAuthority = u.role in setOf("DEPARTMENT", "REVIEWER", "ADMIN")
                when (roleFilter) {
                    "AUTHORITY" -> isAuthority
                    "CITIZEN", "REGULAR" -> !isAuthority
                    else -> true
                }
            }.sortedWith(compareByDescending<com.example.theekhai.data.db.PilotUser> { it.baseScore }.thenBy { it.fullName })

            val entries = filtered.mapIndexed { idx, u ->
                val isAuthority = u.role in setOf("DEPARTMENT", "REVIEWER", "ADMIN")
                val score = u.baseScore.toInt()
                val badge = when {
                    isAuthority && score >= 450 -> "Civic Champion ★"
                    isAuthority && score >= 350 -> "Watchdog Auditor"
                    isAuthority && score >= 280 -> "Rapid Responder"
                    isAuthority -> "Field Specialist"
                    score >= 350 -> "Lead Civic Sentinel"
                    score >= 250 -> "Ward Guardian"
                    score >= 170 -> "Active Reporter"
                    else -> "Rising Citizen"
                }
                LeaderboardEntryDto(
                    rank = idx + 1,
                    userId = u.id,
                    fullName = u.fullName,
                    email = u.email,
                    role = u.role,
                    userType = if (isAuthority) "AUTHORITY" else "REGULAR",
                    department = u.department,
                    ward = u.ward,
                    score = score,
                    actionsTakenCount = u.actionsTakenCount.toInt(),
                    complaintsSubmittedCount = u.complaintsSubmittedCount.toInt(),
                    complaintsApprovedCount = u.complaintsApprovedCount.toInt(),
                    badge = badge
                )
            }

            val recentLogs = database.pilotQueries.allScoreLogs().executeAsList().take(25).map { log ->
                ScoreEventDto(
                    id = log.id,
                    userId = log.userId,
                    userName = userMap[log.userId]?.fullName ?: "Civic Member",
                    caseId = log.caseId,
                    reasonCode = log.reasonCode,
                    pointsDelta = log.pointsDelta.toInt(),
                    description = log.description,
                    createdAt = log.createdAt
                )
            }

            call.respond(
                LeaderboardResponseDto(
                    entries = entries,
                    recentActivity = recentLogs,
                    totalParticipants = allUsers.size,
                    totalPointsAwarded = allUsers.sumOf { it.baseScore.toInt() },
                    updatedAtEpochMillis = System.currentTimeMillis()
                )
            )
        } }

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
                val r = workflow.submit(actor,ReportInput(input.title,input.description,input.category,input.address,input.latitude,input.longitude,input.consent,input.priority),key)
                Triple(r, workflow.tasks(actor, r.id), workflow.events(actor, r.id))
            }
            scope.launch { auth.syncCaseToSupabase(call, record, tasks, events) }
            call.respond(HttpStatusCode.Created,record.dto())
        } }
        get("/api/v1/complaints/{id}") { safe(call) {
            val actor = identity(call); val id = call.parameters["id"]!!
            val result = synchronized(workflow) {
                CaseDetailDto(
                    workflow.detail(actor,id).dto(),
                    workflow.tasks(actor,id).map { TaskDto(it.id,it.caseId,it.department,it.title,it.required,it.complete) },
                    workflow.events(actor,id).map { EventDto(it.id,it.caseId,it.actorId,it.action,it.note,it.at) },
                    workflow.links(actor,id).map { CaseLinkDto(it.caseId,it.targetId,it.kind) }
                )
            }
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
        post("/api/v1/complaints/{id}/verify") { safe(call) {
            val actor = identity(call)
            val caseId = call.parameters["id"]!!
            val req = runCatching { call.receive<TriggerVerificationRequestDto>() }.getOrDefault(TriggerVerificationRequestDto(caseId))
            val (record, tasks, events) = synchronized(workflow) {
                Triple(workflow.detail(actor, caseId), workflow.tasks(actor, caseId), workflow.events(actor, caseId))
            }
            val verif = legal.verifyCaseWithTinyFish(record, tasks, events, req)
            synchronized(workflow) {
                workflow.act(
                    actor = Actor("system-tinyfish", Role.ADMIN),
                    id = caseId,
                    command = CaseAction("TINYFISH_VERIFICATION", record.version, "TinyFish verification result: ${verif.state} (${verif.explanation})", verif.id)
                )
            }
            call.respond(verif)
        } }
        post("/api/v1/complaints/{id}/triage") { safe(call) {
            val actor = identity(call)
            val caseId = call.parameters["id"]!!
            val (record, allCases) = synchronized(workflow) {
                workflow.detail(actor, caseId) to workflow.list(Actor("system-triage", Role.ADMIN))
            }
            val triageAssist = legal.triageAssist(
                TriageAssistRequestDto(title = record.title, description = record.description, address = record.address),
                allCases
            )
            val prioDto = runCatching { ComplaintPriorityDto.valueOf(triageAssist.suggestedPriority) }.getOrDefault(ComplaintPriorityDto.MEDIUM)
            call.respond(
                TriageResponseDto(
                    complaintId = caseId,
                    clusterId = triageAssist.duplicateCaseIds.firstOrNull() ?: "cluster-${triageAssist.suggestedCategory.lowercase()}",
                    priority = prioDto,
                    assignedDepartment = triageAssist.suggestedDepartment,
                    isDuplicate = triageAssist.duplicateCaseIds.isNotEmpty(),
                    reasoning = triageAssist.summary,
                    confidenceScore = 0.95
                )
            )
        } }
        post("/api/v1/complaints/{id}/resolve") { safe(call) {
            val actor = identity(call)
            val caseId = call.parameters["id"]!!
            val req = call.receive<ResolveComplaintRequestDto>()
            val result = synchronized(workflow) {
                val record = workflow.detail(actor, caseId)
                val openTasks = workflow.tasks(actor, caseId).filter { !it.complete && (actor.role == Role.ADMIN || it.department == actor.department) }
                for (t in openTasks) {
                    workflow.act(actor, caseId, CaseAction("COMPLETE_TASK", workflow.detail(actor, caseId).version, "Resolved by worker: ${req.resolutionNotes}", t.id))
                }
                val updated = workflow.detail(actor, caseId)
                if (updated.status == CaseStatus.IN_PROGRESS && workflow.tasks(actor, caseId).none { it.required && !it.complete }) {
                    workflow.act(actor, caseId, CaseAction("REQUEST_CLOSURE", updated.version, req.resolutionNotes))
                } else updated
            }
            call.respond(mapOf("status" to "resolved", "caseId" to result.id))
        } }
        post("/api/v1/complaints/{id}/escalate") { safe(call) {
            val actor = identity(call)
            val caseId = call.parameters["id"]!!
            val req = call.receive<EscalateRequestDto>()
            synchronized(workflow) {
                val record = workflow.detail(actor, caseId)
                workflow.act(actor, caseId, CaseAction("SLA_ESCALATION", record.version, "Manual escalation: ${req.reason}"))
            }
            call.respond(mapOf("status" to "escalated", "caseId" to caseId))
        } }
        post("/api/v1/evidence/verify") { safe(call) {
            val actor = identity(call)
            val req = call.receive<EvidenceCheckRequestDto>()
            val record = synchronized(workflow) { workflow.detail(actor, req.complaintId) }
            val check = legal.verifyVisualEvidence(req, record)
            synchronized(workflow) {
                val fresh = workflow.detail(actor, req.complaintId)
                workflow.act(actor, req.complaintId, CaseAction("EVIDENCE_CHECK", fresh.version, "Evidence check: approved=${check.approved}, distance=${check.distanceMeters.toInt()}m (${check.reasoning})"))
            }
            call.respond(check)
        } }
        get("/api/v1/audit/logs") { safe(call) {
            val actor = identity(call)
            val cId = call.request.queryParameters["complaintId"]
            val logs = synchronized(workflow) {
                if (cId != null) {
                    workflow.events(actor, cId).map {
                        AuditLogDto(it.id, it.caseId, runCatching { ActorTypeDto.valueOf(actor.role.name) }.getOrDefault(ActorTypeDto.CITIZEN), it.actorId, it.action, it.note, null, it.at)
                    }
                } else {
                    database.pilotQueries.allEvents().executeAsList().map {
                        AuditLogDto(it.id, it.caseId, runCatching { ActorTypeDto.valueOf(actor.role.name) }.getOrDefault(ActorTypeDto.CITIZEN), it.actorId, it.action, it.note, null, it.at)
                    }
                }
            }
            call.respond(logs)
        } }
        get("/api/v1/gmail/status") { safe(call) {
            val redirect = call.request.queryParameters["redirect_uri"]
            call.respond(gmail.getStatus(redirect))
        } }
        get("/api/v1/gmail/auth-url") { safe(call) {
            val redirect = call.request.queryParameters["redirect_uri"]
            call.respond(mapOf("authUrl" to gmail.getAuthUrl(redirect)))
        } }
        get("/api/v1/gmail/oauth2callback") {
            val code = call.request.queryParameters["code"]
            val redirect = call.request.queryParameters["redirect_uri"]
            if (code != null) {
                val success = gmail.handleOAuthCallback(code, redirect)
                val msg = if (success) {
                    "Google Gmail authorization successful! Connected as arshitgupta34@gmail.com. You can close this window and return to TheekHai."
                } else {
                    "Google Gmail authorization failed. Please check backend logs or try again."
                }
                call.respondText(
                    "<html><body style='font-family:sans-serif;padding:40px;text-align:center;'><h2>TheekHai Civic Redressal</h2><p>$msg</p><button onclick='window.close()'>Close Window</button></body></html>",
                    io.ktor.http.ContentType.Text.Html
                )
            } else {
                call.respondText("Missing Google authorization code.", io.ktor.http.ContentType.Text.Plain)
            }
        }
        post("/api/v1/complaints/{id}/email/draft") { safe(call) {
            val actor = identity(call)
            val caseId = call.parameters["id"]!!
            val (record, tasks) = synchronized(workflow) {
                workflow.detail(actor, caseId) to workflow.tasks(actor, caseId)
            }
            // Dynamically discover official contact from government portals via TinyFish
            val contact = legal.discoverOfficialContactWithTinyFish(
                department = record.department,
                ward = if (record.address.contains("Ward", ignoreCase = true)) "Ward " + record.address.substringAfter("Ward").takeWhile { it.isDigit() || it.isWhitespace() }.trim() else "Ward 4",
                address = record.address,
                category = record.category
            )
            // Draft factual statutory complaint email using OpenRouter / grounded template
            val draft = legal.draftComplaintEmail(record, tasks, contact)
            call.respond(draft)
        } }
        post("/api/v1/complaints/{id}/email/send") { safe(call) {
            val actor = identity(call)
            val caseId = call.parameters["id"]!!
            val req = call.receive<SendEmailRequestDto>()
            val record = synchronized(workflow) { workflow.detail(actor, caseId) }
            val result = gmail.sendEmail(
                recipientEmail = req.recipientEmail,
                subject = req.subject,
                bodyText = req.body
            )
            synchronized(workflow) {
                workflow.act(
                    actor = actor,
                    id = caseId,
                    command = CaseAction(
                        action = "GRIEVANCE_EMAILED",
                        version = record.version,
                        note = "Official petition sent to ${req.recipientEmail} via Gmail (${result.messageId})",
                        target = req.recipientEmail
                    )
                )
            }
            call.respond(result)
        } }
        post("/api/v1/sync/batch") { safe(call) {
            val actor = identity(call)
            val req = call.receive<SyncBatchRequestDto>()
            val synced = mutableListOf<String>()
            val rejected = mutableListOf<String>()
            for (c in req.offlineComplaints) {
                try {
                    synchronized(workflow) {
                        workflow.submit(
                            actor,
                            ReportInput(c.title, c.description, c.category, c.address, c.latitude, c.longitude, true, c.priority.name),
                            "sync-${c.id}"
                        )
                    }
                    synced.add(c.id)
                } catch (_: Exception) {
                    rejected.add(c.id)
                }
            }
            call.respond(SyncBatchResponseDto(synced, rejected, System.currentTimeMillis()))
        } }
    }
}

private fun recordCitizenSubmissionPoints(database: AppDatabase, citizenId: String, caseRecord: CaseRecord) {
    val q = database.pilotQueries
    val user = q.pilotUserById(citizenId).executeAsOneOrNull() ?: return
    val delta = 35L
    q.transaction {
        q.incrementCitizenSubmittedScore(delta, user.id)
        q.insertScoreLog(
            UUID.randomUUID().toString(),
            user.id,
            caseRecord.id,
            "GENUINE_COMPLAINT_SUBMITTED",
            delta,
            "Submitted genuine civic report '${caseRecord.title.take(50)}' routed to ${caseRecord.department}",
            System.currentTimeMillis()
        )
    }
}

private fun recordWorkflowActionPoints(database: AppDatabase, actor: Actor, caseRecord: CaseRecord, action: String) {
    val q = database.pilotQueries
    val now = System.currentTimeMillis()
    val authorityPoints = when (action) {
        "START" -> 15L
        "ADD_TASK" -> 10L
        "COMPLETE_TASK" -> 25L
        "REQUEST_CLOSURE" -> 30L
        "APPROVE_CLOSURE" -> 40L
        "REJECT_CLOSURE", "REOPEN" -> 25L
        else -> 0L
    }
    q.transaction {
        if (authorityPoints > 0L && actor.role in setOf(Role.DEPARTMENT, Role.REVIEWER, Role.ADMIN)) {
            val authUser = q.pilotUserById(actor.id).executeAsOneOrNull()
            if (authUser != null) {
                q.incrementAuthorityScore(authorityPoints, authUser.id)
                q.insertScoreLog(
                    UUID.randomUUID().toString(),
                    authUser.id,
                    caseRecord.id,
                    "AUTHORITY_$action",
                    authorityPoints,
                    "Authority action ($action) on case #${caseRecord.id.take(8)}: ${caseRecord.title.take(45)}",
                    now
                )
            }
        }
        if (action == "APPROVE_CLOSURE") {
            val citizen = q.pilotUserById(caseRecord.citizenId).executeAsOneOrNull()
            if (citizen != null) {
                val citizenBonus = 50L
                q.incrementCitizenApprovedScore(citizenBonus, citizen.id)
                q.insertScoreLog(
                    UUID.randomUUID().toString(),
                    citizen.id,
                    caseRecord.id,
                    "COMPLAINT_APPROVED_BY_AUTHORITY",
                    citizenBonus,
                    "Complaint #${caseRecord.id.take(8)} ('${caseRecord.title.take(40)}') verified genuine and approved by authority",
                    now
                )
            }
        }
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
        if (q.allPilotUsers().executeAsList().isEmpty()) {
        val hour = 3_600_000L
        q.transaction {
            q.savePilotUser("11111111-1111-4111-8111-111111111101", "mohit@theekhai.in", "TheekHai@2026", "Mohit Sharma", "Ward 4 RWA", "CITIZEN", null, "Ward 4", "en", 395L, 0L, 6L, 4L, now - 72 * hour)
            q.savePilotUser("11111111-1111-4111-8111-111111111102", "arshit@theekhai.in", "TheekHai@2026", "Arshit Gupta", "Municipal Command Center", "ADMIN", "Municipal Command", "Ward 4", "en", 485L, 16L, 2L, 2L, now - 96 * hour)
            q.savePilotUser("11111111-1111-4111-8111-111111111103", "atri@theekhai.in", "TheekHai@2026", "Atri Chatterjee", "Independent Civic Audit Cell", "REVIEWER", "Quality & Audit Cell", "Ward 4", "en", 430L, 12L, 1L, 1L, now - 90 * hour)
            q.savePilotUser("11111111-1111-4111-8111-111111111104", "himanshu@theekhai.in", "TheekHai@2026", "Himanshu Verma", "PWD B&R Division", "DEPARTMENT", "Public works", "Ward 4", "en", 360L, 13L, 0L, 0L, now - 84 * hour)
            q.savePilotUser("11111111-1111-4111-8111-111111111105", "priya@theekhai.in", "TheekHai@2026", "Priya Nair", "Murthal Citizen Forum", "CITIZEN", null, "Ward 4", "en", 290L, 0L, 4L, 3L, now - 60 * hour)
            q.savePilotUser("11111111-1111-4111-8111-111111111106", "rohan@theekhai.in", "TheekHai@2026", "Rohan Deshmukh", "DHBVN Electrical Ops", "DEPARTMENT", "Electrical services", "Ward 2", "en", 310L, 11L, 0L, 0L, now - 78 * hour)
            q.savePilotUser("11111111-1111-4111-8111-111111111107", "ananya@theekhai.in", "TheekHai@2026", "Ananya Iyer", "Sector 14 Residents Collective", "CITIZEN", null, "Ward 2", "en", 235L, 0L, 4L, 2L, now - 50 * hour)
            q.savePilotUser("11111111-1111-4111-8111-111111111108", "vikram@theekhai.in", "TheekHai@2026", "Vikramaditya Singh", "MCG Water & Drainage", "DEPARTMENT", "Water services", "Ward 3", "en", 265L, 9L, 0L, 0L, now - 65 * hour)
            q.savePilotUser("11111111-1111-4111-8111-111111111109", "neha@theekhai.in", "TheekHai@2026", "Neha Kapoor", "Citizen Resident", "CITIZEN", null, "Ward 4", "hi", 185L, 0L, 3L, 2L, now - 40 * hour)
            q.savePilotUser("11111111-1111-4111-8111-111111111110", "kabir@theekhai.in", "TheekHai@2026", "Kabir Mehta", "Youth Civic Watch", "CITIZEN", null, "Ward 3", "en", 135L, 0L, 2L, 1L, now - 30 * hour)

            q.insertScoreLog("s-log-01", "11111111-1111-4111-8111-111111111102", "case-pilot-004", "AUTHORITY_APPROVE_CLOSURE", 40L, "Approved verified road resurfacing near Murthal Chowk", now - 5 * hour)
            q.insertScoreLog("s-log-02", "11111111-1111-4111-8111-111111111101", "case-pilot-004", "COMPLAINT_APPROVED_BY_AUTHORITY", 50L, "Complaint #case-pilot-004 verified genuine and approved by authority", now - 5 * hour)
            q.insertScoreLog("s-log-03", "11111111-1111-4111-8111-111111111103", "case-pilot-005", "AUTHORITY_APPROVE_CLOSURE", 40L, "Independent review & sign-off on LED streetlight restoration", now - 4 * hour)
            q.insertScoreLog("s-log-04", "11111111-1111-4111-8111-111111111105", "case-pilot-005", "COMPLAINT_APPROVED_BY_AUTHORITY", 50L, "Streetlight outage report verified genuine and resolved", now - 4 * hour)
            q.insertScoreLog("s-log-05", "11111111-1111-4111-8111-111111111104", "case-pilot-003", "AUTHORITY_REQUEST_CLOSURE", 30L, "Completed stormwater grate re-seating and requested closure review", now - 2 * hour)
            q.insertScoreLog("s-log-06", "11111111-1111-4111-8111-111111111106", "case-pilot-002", "AUTHORITY_COMPLETE_TASK", 25L, "Replaced faulty LED driver units on poles 14-16", now - 90 * 60_000L)
            q.insertScoreLog("s-log-07", "11111111-1111-4111-8111-111111111108", "case-pilot-006", "AUTHORITY_START", 15L, "Dispatched jetting machine for main market sewer line", now - 60 * 60_000L)
            q.insertScoreLog("s-log-08", "11111111-1111-4111-8111-111111111107", "case-pilot-006", "GENUINE_COMPLAINT_SUBMITTED", 35L, "Submitted verified drainage overflow report in Sector 14", now - 75 * 60_000L)
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
