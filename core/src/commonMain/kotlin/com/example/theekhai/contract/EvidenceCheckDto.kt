package com.example.theekhai.contract

import kotlinx.serialization.Serializable

@Serializable
data class EvidenceCheckRequestDto(
    val complaintId: String,
    val photoBase64: String,
    val reportedLatitude: Double,
    val reportedLongitude: Double,
    val workerLatitude: Double,
    val workerLongitude: Double,
    val maxAllowedRadiusMeters: Double = 150.0
)

@Serializable
data class EvidenceCheckResponseDto(
    val complaintId: String,
    val isGpsMatched: Boolean,
    val distanceMeters: Double,
    val detectedVisualElements: List<String>,
    val anomalyDetected: Boolean,
    val resolutionConfidence: Double,
    val reasoning: String,
    val approved: Boolean
)

@Serializable
data class SyncBatchRequestDto(
    val offlineComplaints: List<ComplaintDto>,
    val auditLogs: List<AuditLogDto>
)

@Serializable
data class SyncBatchResponseDto(
    val syncedComplaintIds: List<String>,
    val rejectedComplaintIds: List<String>,
    val serverTimestampEpochMillis: Long
)
