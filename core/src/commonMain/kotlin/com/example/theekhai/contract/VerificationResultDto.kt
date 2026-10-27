package com.example.theekhai.contract

import kotlinx.serialization.Serializable

@Serializable
enum class VerificationStateDto {
    IN_PROGRESS,
    CONFIRMED_RESOLVED,
    CONTRADICTION_FOUND,
    MANUAL_INSPECTION_REQUIRED
}

@Serializable
data class VerificationResultDto(
    val id: String,
    val complaintId: String,
    val state: VerificationStateDto,
    val verified: Boolean,
    val scrapedPortalUrl: String? = null,
    val scrapedEvidenceSnippet: String? = null,
    val contradictionDetected: Boolean = false,
    val explanation: String,
    val verifiedAtEpochMillis: Long
)

@Serializable
data class TriggerVerificationRequestDto(
    val complaintId: String,
    val officialPortalUrl: String? = null,
    val resolutionProofUrl: String? = null
)
