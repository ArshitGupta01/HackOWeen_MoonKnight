package com.example.theekhai.contract

import kotlinx.serialization.Serializable

@Serializable
enum class ActorTypeDto {
    CITIZEN,
    FIELD_WORKER,
    ADMIN,
    AI_GEMINI_TRIAGE,
    AI_TINYFISH_AGENT,
    AI_GROQ_VISION,
    SYSTEM_SLA_WATCHER
}

@Serializable
data class AuditLogDto(
    val id: String,
    val complaintId: String,
    val actorType: ActorTypeDto,
    val actorId: String,
    val action: String,
    /**
     * Oct 2026 DPDP & Explainable AI mandate:
     * Full transparent human-readable reasoning for every automated action or escalation.
     */
    val reasoning: String,
    val decisionPayloadJson: String? = null,
    val timestampEpochMillis: Long
)

@Serializable
data class CreateAuditLogRequestDto(
    val complaintId: String,
    val actorType: ActorTypeDto,
    val actorId: String,
    val action: String,
    val reasoning: String,
    val decisionPayloadJson: String? = null
)
