package com.example.theekhai.domain.model

enum class VerificationState {
    IN_PROGRESS,
    CONFIRMED_RESOLVED,
    CONTRADICTION_FOUND,
    MANUAL_INSPECTION_REQUIRED
}

data class VerificationResult(
    val id: String,
    val complaintId: String,
    val state: VerificationState,
    val verified: Boolean,
    val scrapedPortalUrl: String? = null,
    val scrapedEvidenceSnippet: String? = null,
    val contradictionDetected: Boolean = false,
    val explanation: String,
    val verifiedAtEpochMillis: Long
)
