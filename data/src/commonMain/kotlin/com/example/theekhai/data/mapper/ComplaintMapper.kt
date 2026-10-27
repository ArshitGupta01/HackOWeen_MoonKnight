package com.example.theekhai.data.mapper

import com.example.theekhai.contract.ComplaintDto
import com.example.theekhai.contract.ComplaintPriorityDto
import com.example.theekhai.contract.ComplaintStatusDto
import com.example.theekhai.data.db.AuditLogEntity
import com.example.theekhai.data.db.ComplaintEntity
import com.example.theekhai.domain.model.ActorType
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus

fun ComplaintEntity.toDomain(): Complaint = Complaint(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = ComplaintPriority.valueOf(priority),
    status = ComplaintStatus.valueOf(status),
    latitude = latitude,
    longitude = longitude,
    address = address,
    citizenId = citizenId,
    assignedWorkerId = assignedWorkerId,
    zone = zone,
    photoUrl = photoUrl,
    resolutionNotes = resolutionNotes,
    resolutionPhotoUrl = resolutionPhotoUrl,
    clusterId = clusterId,
    slaDueEpochMillis = slaDueEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun ComplaintDto.toDomain(): Complaint = Complaint(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = when (priority) {
        ComplaintPriorityDto.LOW -> ComplaintPriority.LOW
        ComplaintPriorityDto.MEDIUM -> ComplaintPriority.MEDIUM
        ComplaintPriorityDto.HIGH -> ComplaintPriority.HIGH
        ComplaintPriorityDto.CRITICAL -> ComplaintPriority.CRITICAL
    },
    status = when (status) {
        ComplaintStatusDto.PENDING -> ComplaintStatus.PENDING
        ComplaintStatusDto.TRIAGED -> ComplaintStatus.TRIAGED
        ComplaintStatusDto.IN_PROGRESS -> ComplaintStatus.IN_PROGRESS
        ComplaintStatusDto.RESOLVED -> ComplaintStatus.RESOLVED
        ComplaintStatusDto.VERIFIED -> ComplaintStatus.VERIFIED
        ComplaintStatusDto.ESCALATED -> ComplaintStatus.ESCALATED
        ComplaintStatusDto.REOPENED -> ComplaintStatus.REOPENED
    },
    latitude = latitude,
    longitude = longitude,
    address = address,
    citizenId = citizenId,
    assignedWorkerId = assignedWorkerId,
    zone = zone,
    photoUrl = photoUrl,
    resolutionNotes = resolutionNotes,
    resolutionPhotoUrl = resolutionPhotoUrl,
    clusterId = clusterId,
    slaDueEpochMillis = slaDueEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun Complaint.toDto(): ComplaintDto = ComplaintDto(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = when (priority) {
        ComplaintPriority.LOW -> ComplaintPriorityDto.LOW
        ComplaintPriority.MEDIUM -> ComplaintPriorityDto.MEDIUM
        ComplaintPriority.HIGH -> ComplaintPriorityDto.HIGH
        ComplaintPriority.CRITICAL -> ComplaintPriorityDto.CRITICAL
    },
    status = when (status) {
        ComplaintStatus.PENDING -> ComplaintStatusDto.PENDING
        ComplaintStatus.TRIAGED -> ComplaintStatusDto.TRIAGED
        ComplaintStatus.IN_PROGRESS -> ComplaintStatusDto.IN_PROGRESS
        ComplaintStatus.RESOLVED -> ComplaintStatusDto.RESOLVED
        ComplaintStatus.VERIFIED -> ComplaintStatusDto.VERIFIED
        ComplaintStatus.ESCALATED -> ComplaintStatusDto.ESCALATED
        ComplaintStatus.REOPENED -> ComplaintStatusDto.REOPENED
    },
    latitude = latitude,
    longitude = longitude,
    address = address,
    citizenId = citizenId,
    assignedWorkerId = assignedWorkerId,
    zone = zone,
    photoUrl = photoUrl,
    resolutionNotes = resolutionNotes,
    resolutionPhotoUrl = resolutionPhotoUrl,
    clusterId = clusterId,
    slaDueEpochMillis = slaDueEpochMillis,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun AuditLogEntity.toDomain(): AuditLog = AuditLog(
    id = id,
    complaintId = complaintId,
    actorType = ActorType.valueOf(actorType),
    actorId = actorId,
    action = action,
    reasoning = reasoning,
    decisionPayloadJson = decisionPayloadJson,
    timestampEpochMillis = timestampEpochMillis
)

fun AuditLog.toEntity(isSynced: Long = 1): AuditLogEntity = AuditLogEntity(
    id = id,
    complaintId = complaintId,
    actorType = actorType.name,
    actorId = actorId,
    action = action,
    reasoning = reasoning,
    decisionPayloadJson = decisionPayloadJson,
    timestampEpochMillis = timestampEpochMillis,
    isSynced = isSynced
)
