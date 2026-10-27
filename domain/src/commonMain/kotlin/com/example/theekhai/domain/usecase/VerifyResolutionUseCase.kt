package com.example.theekhai.domain.usecase

import com.example.theekhai.domain.model.ActorType
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.domain.model.VerificationResult
import com.example.theekhai.domain.model.VerificationState
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository
import com.example.theekhai.domain.repository.VerificationRepository

class VerifyResolutionUseCase(
    private val complaintRepository: ComplaintRepository,
    private val verificationRepository: VerificationRepository,
    private val auditRepository: AuditRepository
) {
    suspend operator fun invoke(
        complaintId: String,
        portalUrl: String? = null,
        proofUrl: String? = null
    ): Result<VerificationResult> {
        val complaint = complaintRepository.getComplaintById(complaintId)
            ?: return Result.failure(IllegalArgumentException("Complaint $complaintId not found"))

        val verificationResult = verificationRepository.triggerAutonomousVerification(
            complaintId = complaintId,
            officialPortalUrl = portalUrl,
            resolutionProofUrl = proofUrl ?: complaint.resolutionPhotoUrl
        ).getOrElse { error ->
            return Result.failure(error)
        }

        if (verificationResult.verified && !verificationResult.contradictionDetected) {
            complaintRepository.updateStatus(complaintId, ComplaintStatus.VERIFIED)
            auditRepository.recordAuditLog(
                AuditLog(
                    id = "audit-verify-pass-$complaintId-${verificationResult.verifiedAtEpochMillis}",
                    complaintId = complaintId,
                    actorType = ActorType.AI_TINYFISH_AGENT,
                    actorId = "tinyfish-web-scraper",
                    action = "AUTONOMOUS_VERIFICATION_PASSED",
                    reasoning = "TinyFish autonomous scraper crawled municipal portal [${verificationResult.scrapedPortalUrl ?: "civic.gov.in"}] and confirmed physical evidence aligns with worker completion notes: ${verificationResult.explanation}",
                    decisionPayloadJson = """{"verified":true,"state":"${verificationResult.state}"}""",
                    timestampEpochMillis = verificationResult.verifiedAtEpochMillis
                )
            )
        } else {
            // Contradiction or failure detected -> Dynamic escalation and auto-reopening
            complaintRepository.updateStatus(complaintId, ComplaintStatus.REOPENED)
            auditRepository.recordAuditLog(
                AuditLog(
                    id = "audit-verify-fail-$complaintId-${verificationResult.verifiedAtEpochMillis}",
                    complaintId = complaintId,
                    actorType = ActorType.AI_TINYFISH_AGENT,
                    actorId = "tinyfish-web-scraper",
                    action = "CONTRADICTION_DETECTED_AUTO_REOPEN",
                    reasoning = "TinyFish autonomous verification detected discrepancies between worker claim and municipal ground report: ${verificationResult.explanation}. Automated SLA escalation initiated.",
                    decisionPayloadJson = """{"verified":false,"contradictionDetected":true,"state":"${verificationResult.state}"}""",
                    timestampEpochMillis = verificationResult.verifiedAtEpochMillis
                )
            )
        }

        return Result.success(verificationResult)
    }
}
