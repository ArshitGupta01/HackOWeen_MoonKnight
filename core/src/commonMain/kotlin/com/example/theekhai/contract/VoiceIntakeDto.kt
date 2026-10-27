package com.example.theekhai.contract

import kotlinx.serialization.Serializable

@Serializable
data class VoiceIntakeRequestDto(
    /** Base64-encoded audio bytes (e.g. WAV, MP3, WebM) */
    val audioBase64: String,
    val mimeType: String = "audio/wav",
    val languageHint: String? = null,
    val citizenId: String = "11111111-1111-4111-8111-111111111101",
    val zone: String = "Ward 4",
    val latitude: Double = 29.0295,
    val longitude: Double = 77.0715,
    val address: String = "Murthal Chowk, Ward 4",
    /** Strict DPDP Act 2026 consent verification */
    val dpdpConsentGranted: Boolean = true,
    val dpdpConsentText: String = "I explicitly consent under DPDP Act 2026 to the automated voice-to-text processing of this grievance recording for civic resolution."
)

@Serializable
data class VoiceIntakeResponseDto(
    val complaintId: String,
    val originalTranscript: String,
    val translatedEnglishText: String,
    val detectedLanguage: String,
    val confidence: Double,
    val inferredCategory: String,
    val proposedPriority: ComplaintPriorityDto,
    val reasoning: String
)
