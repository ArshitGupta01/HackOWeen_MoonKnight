package com.example.theekhai.domain.usecase

import com.example.theekhai.contract.VoiceIntakeRequestDto
import com.example.theekhai.contract.VoiceIntakeResponseDto
import com.example.theekhai.domain.model.ActorType
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository

interface VoiceSpeechToTextClient {
    suspend fun transcribeAndTranslate(
        audioBase64: String,
        mimeType: String,
        languageHint: String?
    ): VoiceIntakeResponseDto
}

class VoiceIntakeUseCase(
    private val complaintRepository: ComplaintRepository,
    private val auditRepository: AuditRepository,
    private val speechToTextClient: VoiceSpeechToTextClient
) {
    suspend operator fun invoke(request: VoiceIntakeRequestDto): Result<Complaint> {
        if (!request.dpdpConsentGranted) {
            return Result.failure(
                IllegalStateException("DPDP Act 2026 Violation: Voice grievance processing rejected without explicit citizen consent.")
            )
        }

        return try {
            val sttResult = speechToTextClient.transcribeAndTranslate(
                audioBase64 = request.audioBase64,
                mimeType = request.mimeType,
                languageHint = request.languageHint
            )

            val now = 1793100000000L
            val initialPriority = when (sttResult.proposedPriority) {
                com.example.theekhai.contract.ComplaintPriorityDto.LOW -> ComplaintPriority.LOW
                com.example.theekhai.contract.ComplaintPriorityDto.MEDIUM -> ComplaintPriority.MEDIUM
                com.example.theekhai.contract.ComplaintPriorityDto.HIGH -> ComplaintPriority.HIGH
                com.example.theekhai.contract.ComplaintPriorityDto.CRITICAL -> ComplaintPriority.CRITICAL
            }

            val newComplaint = Complaint(
                id = sttResult.complaintId.ifBlank { "cmp-${now}" },
                title = sttResult.translatedEnglishText.take(60),
                description = "Original (${sttResult.detectedLanguage}): \"${sttResult.originalTranscript}\"\nTranslated: \"${sttResult.translatedEnglishText}\"",
                category = sttResult.inferredCategory,
                priority = initialPriority,
                status = ComplaintStatus.PENDING,
                latitude = request.latitude,
                longitude = request.longitude,
                address = request.address,
                citizenId = request.citizenId,
                zone = request.zone,
                slaDueEpochMillis = now + (24 * 3600 * 1000L), // default 24h SLA
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now
            )

            val created = complaintRepository.createComplaint(newComplaint)

            // DPDP 2026 Audit Trail
            auditRepository.recordAuditLog(
                AuditLog(
                    id = "audit-voice-intake-${created.id}",
                    complaintId = created.id,
                    actorType = ActorType.CITIZEN,
                    actorId = request.citizenId,
                    action = "DPDP_CONSENT_AND_VOICE_INTAKE",
                    reasoning = "Citizen provided explicit DPDP 2026 consent. Multilingual voice transcribed from [${sttResult.detectedLanguage}] to English with confidence ${sttResult.confidence}. Inferred category [${sttResult.inferredCategory}]. Rationale: ${sttResult.reasoning}",
                    decisionPayloadJson = """{"consentNotice":"${request.dpdpConsentText}","detectedLang":"${sttResult.detectedLanguage}","confidence":${sttResult.confidence}}""",
                    timestampEpochMillis = now
                )
            )

            Result.success(created)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
