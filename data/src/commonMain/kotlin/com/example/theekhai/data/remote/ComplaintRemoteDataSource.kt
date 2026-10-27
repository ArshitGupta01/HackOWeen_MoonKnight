package com.example.theekhai.data.remote

import com.example.theekhai.contract.ApiRoutes
import com.example.theekhai.contract.AuditLogDto
import com.example.theekhai.contract.ComplaintDto
import com.example.theekhai.contract.CreateComplaintRequestDto
import com.example.theekhai.contract.EscalateRequestDto
import com.example.theekhai.contract.EvidenceCheckRequestDto
import com.example.theekhai.contract.EvidenceCheckResponseDto
import com.example.theekhai.contract.ResolveComplaintRequestDto
import com.example.theekhai.contract.SyncBatchRequestDto
import com.example.theekhai.contract.SyncBatchResponseDto
import com.example.theekhai.contract.TriageResponseDto
import com.example.theekhai.contract.TriggerVerificationRequestDto
import com.example.theekhai.contract.VerificationResultDto
import com.example.theekhai.contract.VoiceIntakeRequestDto
import com.example.theekhai.contract.VoiceIntakeResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

interface ComplaintRemoteDataSource {
    suspend fun getComplaints(): List<ComplaintDto>
    suspend fun getComplaintById(id: String): ComplaintDto?
    suspend fun createComplaint(request: CreateComplaintRequestDto): ComplaintDto
    suspend fun resolveComplaint(request: ResolveComplaintRequestDto): Boolean
    suspend fun triageComplaint(id: String): TriageResponseDto
    suspend fun triggerVerification(request: TriggerVerificationRequestDto): VerificationResultDto
    suspend fun escalateComplaint(request: EscalateRequestDto): Boolean
    suspend fun voiceIntake(request: VoiceIntakeRequestDto): VoiceIntakeResponseDto
    suspend fun checkEvidence(request: EvidenceCheckRequestDto): EvidenceCheckResponseDto
    suspend fun syncBatch(request: SyncBatchRequestDto): SyncBatchResponseDto
    suspend fun getAuditLogs(complaintId: String? = null): List<AuditLogDto>
}

class KtorComplaintRemoteDataSource(
    private val client: HttpClient,
    private val baseUrl: String
) : ComplaintRemoteDataSource {

    private fun url(path: String): String = "${baseUrl.trimEnd('/')}$path"

    override suspend fun getComplaints(): List<ComplaintDto> =
        try {
            val page = client.get(url(ApiRoutes.COMPLAINTS)).body<com.example.theekhai.contract.CasePageDto>()
            page.items.map { it.toComplaintDto() }
        } catch (_: Exception) {
            client.get(url(ApiRoutes.COMPLAINTS)).body()
        }

    override suspend fun getComplaintById(id: String): ComplaintDto? =
        try {
            val detail = client.get(url(ApiRoutes.COMPLAINTS) + "/$id").body<com.example.theekhai.contract.CaseDetailDto>()
            detail.case.toComplaintDto()
        } catch (_: Exception) {
            try {
                client.get(url(ApiRoutes.COMPLAINTS) + "/$id").body()
            } catch (_: Exception) {
                null
            }
        }

    override suspend fun createComplaint(request: CreateComplaintRequestDto): ComplaintDto =
        client.post(url(ApiRoutes.COMPLAINTS)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun resolveComplaint(request: ResolveComplaintRequestDto): Boolean =
        try {
            client.post(url(ApiRoutes.COMPLAINTS) + "/${request.complaintId}/resolve") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            true
        } catch (_: Exception) {
            false
        }

    override suspend fun triageComplaint(id: String): TriageResponseDto =
        client.post(url(ApiRoutes.COMPLAINTS) + "/$id/triage").body()

    override suspend fun triggerVerification(request: TriggerVerificationRequestDto): VerificationResultDto =
        client.post(url(ApiRoutes.COMPLAINTS) + "/${request.complaintId}/verify") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun escalateComplaint(request: EscalateRequestDto): Boolean =
        try {
            client.post(url(ApiRoutes.COMPLAINTS) + "/${request.complaintId}/escalate") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            true
        } catch (_: Exception) {
            false
        }

    override suspend fun voiceIntake(request: VoiceIntakeRequestDto): VoiceIntakeResponseDto =
        client.post(url(ApiRoutes.VOICE_INTAKE)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun checkEvidence(request: EvidenceCheckRequestDto): EvidenceCheckResponseDto =
        client.post(url(ApiRoutes.EVIDENCE_CHECK)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun syncBatch(request: SyncBatchRequestDto): SyncBatchResponseDto =
        client.post(url(ApiRoutes.SYNC_BATCH)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()

    override suspend fun getAuditLogs(complaintId: String?): List<AuditLogDto> {
        val path = if (complaintId != null) "${ApiRoutes.AUDIT_LOGS}?complaintId=$complaintId" else ApiRoutes.AUDIT_LOGS
        return client.get(url(path)).body()
    }
}

private fun com.example.theekhai.contract.CaseDto.toComplaintDto(): ComplaintDto = ComplaintDto(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = runCatching { com.example.theekhai.contract.ComplaintPriorityDto.valueOf(priority) }.getOrDefault(com.example.theekhai.contract.ComplaintPriorityDto.MEDIUM),
    status = runCatching { com.example.theekhai.contract.ComplaintStatusDto.valueOf(status) }.getOrDefault(com.example.theekhai.contract.ComplaintStatusDto.PENDING),
    latitude = latitude ?: 0.0,
    longitude = longitude ?: 0.0,
    address = address,
    citizenId = citizenId,
    assignedWorkerId = null,
    zone = department,
    photoUrl = null,
    resolutionNotes = null,
    resolutionPhotoUrl = null,
    clusterId = null,
    slaDueEpochMillis = dueAt,
    createdAtEpochMillis = createdAt,
    updatedAtEpochMillis = updatedAt
)
