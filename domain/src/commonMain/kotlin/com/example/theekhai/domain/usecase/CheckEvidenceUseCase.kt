package com.example.theekhai.domain.usecase

import com.example.theekhai.contract.EvidenceCheckRequestDto
import com.example.theekhai.contract.EvidenceCheckResponseDto
import com.example.theekhai.domain.model.ActorType
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository
import kotlin.math.*

interface VisionEvidenceClient {
    suspend fun analyzeImageEvidence(
        photoBase64: String,
        expectedCategory: String
    ): Pair<List<String>, Double> // detected labels and confidence
}

class CheckEvidenceUseCase(
    private val complaintRepository: ComplaintRepository,
    private val auditRepository: AuditRepository,
    private val visionEvidenceClient: VisionEvidenceClient
) {
    suspend operator fun invoke(request: EvidenceCheckRequestDto): Result<EvidenceCheckResponseDto> {
        val complaint = complaintRepository.getComplaintById(request.complaintId)
            ?: return Result.failure(IllegalArgumentException("Complaint ${request.complaintId} not found"))

        // Haversine distance calculation in meters between reported location and field worker location
        val distance = computeHaversineMeters(
            lat1 = request.reportedLatitude,
            lon1 = request.reportedLongitude,
            lat2 = request.workerLatitude,
            lon2 = request.workerLongitude
        )

        val isGpsMatched = distance <= request.maxAllowedRadiusMeters

        val (labels, visualConfidence) = visionEvidenceClient.analyzeImageEvidence(
            photoBase64 = request.photoBase64,
            expectedCategory = complaint.category
        )

        val approved = isGpsMatched && visualConfidence >= 0.70
        val reasoning = if (approved) {
            "Field worker GPS is within ${distance.roundToInt()}m of incident (threshold ${request.maxAllowedRadiusMeters.roundToInt()}m). Visual evidence confirmed (${labels.joinToString(", ")}) with ${(visualConfidence * 100).roundToInt()}% confidence."
        } else if (!isGpsMatched) {
            "Geofence violation: Field worker uploaded photo ${distance.roundToInt()}m away from reported location (max permitted ${request.maxAllowedRadiusMeters.roundToInt()}m)."
        } else {
            "Visual verification shortfall: Uploaded photo does not conclusively exhibit resolution of ${complaint.category}. Confidence ${(visualConfidence * 100).roundToInt()}% below 70% threshold."
        }

        val response = EvidenceCheckResponseDto(
            complaintId = request.complaintId,
            isGpsMatched = isGpsMatched,
            distanceMeters = distance,
            detectedVisualElements = labels,
            anomalyDetected = !approved,
            resolutionConfidence = visualConfidence,
            reasoning = reasoning,
            approved = approved
        )

        // Log to explainable audit trail
        val now = 1793100000000L
        auditRepository.recordAuditLog(
            AuditLog(
                id = "audit-evidence-${request.complaintId}-${now.hashCode()}",
                complaintId = request.complaintId,
                actorType = ActorType.AI_GROQ_VISION,
                actorId = "groq-vision-evidence-agent",
                action = if (approved) "EVIDENCE_VERIFIED_PASSED" else "EVIDENCE_VERIFICATION_REJECTED",
                reasoning = reasoning,
                decisionPayloadJson = """{"approved":$approved,"distanceMeters":$distance,"confidence":$visualConfidence}""",
                timestampEpochMillis = now
            )
        )

        return Result.success(response)
    }

    private fun computeHaversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = (lat2 - lat1) * (PI / 180.0)
        val dLon = (lon2 - lon1) * (PI / 180.0)
        val a = sin(dLat / 2).pow(2) +
                cos(lat1 * (PI / 180.0)) * cos(lat2 * (PI / 180.0)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
