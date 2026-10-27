package com.example.theekhai.domain.model

data class VoiceIntakeResult(
    val complaintId: String,
    val originalTranscript: String,
    val translatedEnglishText: String,
    val detectedLanguage: String,
    val confidence: Double,
    val inferredCategory: String,
    val proposedPriority: ComplaintPriority,
    val reasoning: String
)

data class ConsentRecord(
    val citizenId: String,
    val granted: Boolean,
    val consentTimestampEpochMillis: Long,
    val noticeVersion: String = "DPDP-2026-v1.4"
)
