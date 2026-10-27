package com.example.theekhai.domain.model

enum class ComplaintStatus {
    PENDING,
    ASSIGNED,
    TRIAGED,
    IN_PROGRESS,
    RESOLVED,
    VERIFIED,
    PENDING_VERIFICATION,
    CLOSED,
    ESCALATED,
    REOPENED
}

enum class ComplaintPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

data class Complaint(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: ComplaintPriority,
    val status: ComplaintStatus,
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
    val updatedAtEpochMillis: Long,
    val version: Long = 1
) {
    fun isOverdue(currentEpochMillis: Long): Boolean {
        return (status != ComplaintStatus.RESOLVED && status != ComplaintStatus.VERIFIED) &&
                slaDueEpochMillis > 0 &&
                slaDueEpochMillis < currentEpochMillis
    }
}
