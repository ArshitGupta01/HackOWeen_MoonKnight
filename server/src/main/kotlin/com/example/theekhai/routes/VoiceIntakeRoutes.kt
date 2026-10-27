package com.example.theekhai.routes

import com.example.theekhai.ai.BackendAiService
import com.example.theekhai.contract.ApiRoutes
import com.example.theekhai.contract.VoiceIntakeRequestDto
import com.example.theekhai.domain.model.ActorType
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.voiceIntakeRoutes(
    complaintRepository: ComplaintRepository,
    auditRepository: AuditRepository,
    aiService: BackendAiService
) {
    post(ApiRoutes.VOICE_INTAKE) {
        val request = call.receive<VoiceIntakeRequestDto>()
        if (!request.dpdpConsentGranted) {
            call.respond(
                HttpStatusCode.Forbidden,
                mapOf("error" to "DPDP Act 2026 Violation: Voice processing denied without explicit citizen consent.")
            )
            return@post
        }

        val voiceResult = aiService.transcribeVoice(
            audioBase64 = request.audioBase64,
            mimeType = request.mimeType,
            languageHint = request.languageHint
        )

        val now = System.currentTimeMillis()
        auditRepository.recordAuditLog(
            AuditLog(
                id = "audit-stt-${voiceResult.complaintId}-$now",
                complaintId = voiceResult.complaintId,
                actorType = ActorType.CITIZEN,
                actorId = request.citizenId,
                action = "MULTILINGUAL_VOICE_INTAKE",
                reasoning = "Audio transcribed via Groq Whisper from [${voiceResult.detectedLanguage}] to English with confidence ${voiceResult.confidence}. DPDP consent validated: [${request.dpdpConsentText}].",
                timestampEpochMillis = now
            )
        )

        call.respond(HttpStatusCode.Created, voiceResult)
    }
}
