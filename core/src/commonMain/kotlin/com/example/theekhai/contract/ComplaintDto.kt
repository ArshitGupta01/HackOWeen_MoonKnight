package com.example.theekhai.contract

import kotlinx.serialization.Serializable

@Serializable
enum class ComplaintStatusDto {
    PENDING,
    TRIAGED,
    IN_PROGRESS,
    RESOLVED,
    VERIFIED,
    ESCALATED,
    REOPENED
}

@Serializable
enum class ComplaintPriorityDto {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
data class ComplaintDto(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: ComplaintPriorityDto = ComplaintPriorityDto.MEDIUM,
    val status: ComplaintStatusDto = ComplaintStatusDto.PENDING,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val citizenId: String,
    val assignedWorkerId: String? = null,
    val zone: String,
    val photoUrl: String? = null,
    val resolutionNotes: String? = null,
    val resolutionPhotoUrl: String? = null,
    val clusterId: String? = null,
    val slaDueEpochMillis: Long,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

@Serializable
data class CreateComplaintRequestDto(
    val title: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val citizenId: String,
    val zone: String,
    val photoUrl: String? = null,
    val consentGranted: Boolean = true
)

@Serializable
data class ResolveComplaintRequestDto(
    val complaintId: String,
    val workerId: String,
    val resolutionNotes: String,
    val photoUrl: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class TriageRequestDto(
    val complaintId: String
)

@Serializable
data class TriageResponseDto(
    val complaintId: String,
    val clusterId: String?,
    val priority: ComplaintPriorityDto,
    val assignedDepartment: String,
    val isDuplicate: Boolean,
    val reasoning: String,
    val confidenceScore: Double
)

@Serializable
data class EscalateRequestDto(
    val complaintId: String,
    val reason: String
)
