package com.example.theekhai.domain.usecase

import com.example.theekhai.domain.model.ActorType
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository

class EscalateComplaintUseCase(
    private val complaintRepository: ComplaintRepository,
    private val auditRepository: AuditRepository
) {
    suspend operator fun invoke(
        complaintId: String,
        escalationReason: String,
        triggeredBy: ActorType = ActorType.SYSTEM_SLA_WATCHER,
        actorId: String = "sla-monitor-daemon"
    ): Result<Boolean> {
        val complaint = complaintRepository.getComplaintById(complaintId)
            ?: return Result.failure(IllegalArgumentException("Complaint $complaintId not found"))

        // Escalate status and elevate priority to CRITICAL
        complaintRepository.updateStatus(complaintId, ComplaintStatus.ESCALATED)
        complaintRepository.updateTriage(
            id = complaintId,
            clusterId = complaint.clusterId,
            priority = ComplaintPriority.CRITICAL,
            category = complaint.category
        )

        val nowEpoch = kotlin.time.Clock.System.now().toEpochMilliseconds() // 2026 epoch fallback or caller timestamp
        auditRepository.recordAuditLog(
            AuditLog(
                id = "audit-escalate-$complaintId-${nowEpoch.hashCode()}",
                complaintId = complaintId,
                actorType = triggeredBy,
                actorId = actorId,
                action = "DYNAMIC_SLA_ESCALATION",
                reasoning = "Grievance breached target resolution window or unverified resolution flagged. Escalated to Commissioner Level. Reason: $escalationReason",
                decisionPayloadJson = """{"previousStatus":"${complaint.status}","newStatus":"ESCALATED","newPriority":"CRITICAL"}""",
                timestampEpochMillis = nowEpoch
            )
        )

        return Result.success(true)
    }
}
