package com.example.theekhai.domain.model

enum class ActorType {
    CITIZEN,
    FIELD_WORKER,
    ADMIN,
    AI_GEMINI_TRIAGE,
    AI_TINYFISH_AGENT,
    AI_GROQ_VISION,
    SYSTEM_SLA_WATCHER
}

data class AuditLog(
    val id: String,
    val complaintId: String,
    val actorType: ActorType,
    val actorId: String,
    val action: String,
    /**
     * Explainable AI requirement: every automated decision
     * MUST contain human-understandable justification and audit reasoning.
     */
    val reasoning: String,
    val decisionPayloadJson: String? = null,
    val timestampEpochMillis: Long
)
