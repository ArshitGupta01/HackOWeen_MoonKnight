package com.example.theekhai.routes

import com.example.theekhai.ai.BackendAiService
import com.example.theekhai.contract.ApiRoutes
import com.example.theekhai.contract.EvidenceCheckRequestDto
import com.example.theekhai.contract.EvidenceCheckResponseDto
import com.example.theekhai.domain.model.ActorType
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.repository.AuditRepository
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlin.math.*

fun Route.evidenceRoutes(
    auditRepository: AuditRepository,
    aiService: BackendAiService
) {
    post(ApiRoutes.EVIDENCE_CHECK) {
        val request = call.receive<EvidenceCheckRequestDto>()

        // Haversine distance
        val r = 6371000.0
        val dLat = (request.workerLatitude - request.reportedLatitude) * (PI / 180.0)
        val dLon = (request.workerLongitude - request.reportedLongitude) * (PI / 180.0)
        val a = sin(dLat / 2).pow(2) +
                cos(request.reportedLatitude * (PI / 180.0)) * cos(request.workerLatitude * (PI / 180.0)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = r * c

        val isGpsMatched = distance <= request.maxAllowedRadiusMeters

        val (labels, confidence) = aiService.verifyVisualEvidence(
            photoBase64 = request.photoBase64,
            expectedCategory = "Roads"
        )

        val approved = isGpsMatched && confidence >= 0.70
        val reasoning = if (approved) {
            "Groq Vision verified resolution elements (${labels.joinToString(", ")}). Geofence check passed: worker GPS within ${distance.toInt()}m of reported complaint."
        } else if (!isGpsMatched) {
            "Geofence violation: Worker photo location was ${distance.toInt()}m away from reported site (exceeds ${request.maxAllowedRadiusMeters.toInt()}m tolerance)."
        } else {
            "Visual validation shortfall: AI confidence ${(confidence * 100).toInt()}% below threshold."
        }

        val response = EvidenceCheckResponseDto(
            complaintId = request.complaintId,
            isGpsMatched = isGpsMatched,
            distanceMeters = distance,
            detectedVisualElements = labels,
            anomalyDetected = !approved,
            resolutionConfidence = confidence,
            reasoning = reasoning,
            approved = approved
        )

        val now = System.currentTimeMillis()
        auditRepository.recordAuditLog(
            AuditLog(
                id = "audit-evidence-${request.complaintId}-$now",
                complaintId = request.complaintId,
                actorType = ActorType.AI_GROQ_VISION,
                actorId = "groq-vision-auditor",
                action = if (approved) "EVIDENCE_VERIFIED_PASSED" else "EVIDENCE_REJECTED",
                reasoning = reasoning,
                decisionPayloadJson = """{"approved":$approved,"distanceMeters":$distance,"confidence":$confidence}""",
                timestampEpochMillis = now
            )
        )

        call.respond(response)
    }
}
