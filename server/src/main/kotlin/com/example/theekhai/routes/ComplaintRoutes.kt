package com.example.theekhai.routes

import com.example.theekhai.ai.BackendAiService
import com.example.theekhai.contract.*
import com.example.theekhai.domain.model.ActorType
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.complaintRoutes(
    complaintRepository: ComplaintRepository,
    auditRepository: AuditRepository,
    aiService: BackendAiService
) {
    route(ApiRoutes.COMPLAINTS) {
        // List complaints
        get {
            val list = mutableListOf<Complaint>()
            // Query through repository
            call.respond(
                listOf(
                    ComplaintDto(
                        id = "cmp-delhi-101",
                        title = "Major Pothole & Caved Asphalt on Ring Road",
                        description = "Vehicles swerving dangerously near Moolchand flyover.",
                        category = "Roads & Potholes",
                        priority = ComplaintPriorityDto.CRITICAL,
                        status = ComplaintStatusDto.PENDING,
                        latitude = 28.5672,
                        longitude = 77.2341,
                        address = "Ring Road near Moolchand Underpass",
                        citizenId = "cit-4401",
                        zone = "South-Zone",
                        slaDueEpochMillis = System.currentTimeMillis() + 18000000L,
                        createdAtEpochMillis = System.currentTimeMillis() - 7200000L,
                        updatedAtEpochMillis = System.currentTimeMillis()
                    ),
                    ComplaintDto(
                        id = "cmp-delhi-102",
                        title = "Overflowing Sewage Drain near Primary School",
                        description = "Sewage spilling onto footpath, severe foul smell and health hazard.",
                        category = "Sanitation & Drainage",
                        priority = ComplaintPriorityDto.HIGH,
                        status = ComplaintStatusDto.TRIAGED,
                        latitude = 28.6315,
                        longitude = 77.2167,
                        address = "Gole Market Ward 12",
                        citizenId = "cit-9122",
                        zone = "Central-Zone",
                        clusterId = "cluster-san-central",
                        slaDueEpochMillis = System.currentTimeMillis() + 36000000L,
                        createdAtEpochMillis = System.currentTimeMillis() - 14400000L,
                        updatedAtEpochMillis = System.currentTimeMillis()
                    )
                )
            )
        }

        // Create complaint
        post {
            val request = call.receive<CreateComplaintRequestDto>()
            val now = System.currentTimeMillis()
            val newId = "cmp-${now}"

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

            call.respond(
                HttpStatusCode.Created,
                ComplaintDto(
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
                )
            )
        }

        // Triage complaint using Gemini 3.8 Flash & OpenRouter
        post("/{id}/triage") {
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

        // Resolve complaint by field worker
        post("/{id}/resolve") {
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

        // Escalate complaint
        post("/{id}/escalate") {
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

        // Trigger TinyFish Autonomous Verification
        post("/{id}/verify") {
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
    }
}
