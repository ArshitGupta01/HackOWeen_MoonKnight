package com.example.theekhai.domain.usecase

import com.example.theekhai.contract.TriageResponseDto
import com.example.theekhai.domain.model.ActorType
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository

interface TriageAiClient {
    suspend fun analyzeAndCluster(
        complaintId: String,
        title: String,
        description: String,
        category: String,
        existingClusters: List<Pair<String, String>>
    ): TriageResponseDto
}

class TriageComplaintUseCase(
    private val complaintRepository: ComplaintRepository,
    private val auditRepository: AuditRepository,
    private val triageAiClient: TriageAiClient
) {
    suspend operator fun invoke(complaintId: String): Result<TriageResponseDto> {
        val complaint = complaintRepository.getComplaintById(complaintId)
            ?: return Result.failure(IllegalArgumentException("Complaint $complaintId not found"))

        return try {
            val triageResult = triageAiClient.analyzeAndCluster(
                complaintId = complaint.id,
                title = complaint.title,
                description = complaint.description,
                category = complaint.category,
                existingClusters = emptyList()
            )

            val domainPriority = when (triageResult.priority) {
                com.example.theekhai.contract.ComplaintPriorityDto.LOW -> ComplaintPriority.LOW
                com.example.theekhai.contract.ComplaintPriorityDto.MEDIUM -> ComplaintPriority.MEDIUM
                com.example.theekhai.contract.ComplaintPriorityDto.HIGH -> ComplaintPriority.HIGH
                com.example.theekhai.contract.ComplaintPriorityDto.CRITICAL -> ComplaintPriority.CRITICAL
            }

            complaintRepository.updateTriage(
                id = complaintId,
                clusterId = triageResult.clusterId,
                priority = domainPriority,
                category = triageResult.assignedDepartment
            )
            complaintRepository.updateStatus(complaintId, ComplaintStatus.TRIAGED)

            // Mandated DPDP & Explainable AI Audit Log
            auditRepository.recordAuditLog(
                AuditLog(
                    id = "audit-triage-$complaintId-${triageResult.hashCode()}",
                    complaintId = complaintId,
                    actorType = ActorType.AI_GEMINI_TRIAGE,
                    actorId = "gemini-3.8-flash-agent",
                    action = "AI_TRIAGE_AND_CLUSTERING",
                    reasoning = triageResult.reasoning,
                    decisionPayloadJson = """{"priority":"${triageResult.priority}","clusterId":"${triageResult.clusterId}","isDuplicate":${triageResult.isDuplicate},"confidence":${triageResult.confidenceScore}}""",
                    timestampEpochMillis = complaint.updatedAtEpochMillis
                )
            )

            Result.success(triageResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
