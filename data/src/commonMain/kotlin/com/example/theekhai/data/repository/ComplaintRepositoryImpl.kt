package com.example.theekhai.data.repository

import com.example.theekhai.contract.CreateComplaintRequestDto
import com.example.theekhai.contract.ResolveComplaintRequestDto
import com.example.theekhai.contract.SyncBatchRequestDto
import com.example.theekhai.data.db.ComplaintEntity
import com.example.theekhai.data.local.ComplaintLocalDataSource
import com.example.theekhai.data.mapper.toDomain
import com.example.theekhai.data.mapper.toDto
import com.example.theekhai.data.mapper.toEntity
import com.example.theekhai.data.remote.ComplaintRemoteDataSource
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.domain.repository.ComplaintRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ComplaintRepositoryImpl(
    private val local: ComplaintLocalDataSource,
    private val remote: ComplaintRemoteDataSource
) : ComplaintRepository {

    override fun observeComplaints(): Flow<List<Complaint>> =
        local.observeComplaints().map { list -> list.map { it.toDomain() } }

    override suspend fun getComplaintById(id: String): Complaint? {
        val cached = local.getComplaintById(id)
        if (cached != null) return cached.toDomain()

        return try {
            val remoteDto = remote.getComplaintById(id)
            if (remoteDto != null) {
                val domain = remoteDto.toDomain()
                local.insertOrReplace(toEntity(domain, isSynced = 1L))
                domain
            } else null
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun createComplaint(complaint: Complaint): Complaint {
        // Save locally first with isSynced = 0
        local.insertOrReplace(toEntity(complaint, isSynced = 0L))

        // Attempt remote push
        return try {
            val created = remote.createComplaint(
                CreateComplaintRequestDto(
                    title = complaint.title,
                    description = complaint.description,
                    category = complaint.category,
                    latitude = complaint.latitude,
                    longitude = complaint.longitude,
                    address = complaint.address,
                    citizenId = complaint.citizenId,
                    zone = complaint.zone,
                    photoUrl = complaint.photoUrl
                )
            ).toDomain()
            local.insertOrReplace(toEntity(created, isSynced = 1L))
            created
        } catch (_: Exception) {
            // Keep local un-synced
            complaint
        }
    }

    override suspend fun updateComplaint(complaint: Complaint): Complaint {
        local.insertOrReplace(toEntity(complaint, isSynced = 0L))
        return complaint
    }

    override suspend fun updateStatus(id: String, status: ComplaintStatus): Boolean {
        val now = 1793100000000L
        local.updateStatus(id, status.name, now, isSynced = 0L)
        return true
    }

    override suspend fun updateTriage(
        id: String,
        clusterId: String?,
        priority: ComplaintPriority,
        category: String
    ): Boolean {
        val now = 1793100000000L
        local.updateTriage(
            id = id,
            clusterId = clusterId,
            priority = priority.name,
            category = category,
            updatedAt = now,
            isSynced = 0L
        )
        return true
    }

    override suspend fun resolveComplaint(
        id: String,
        workerId: String,
        resolutionNotes: String,
        photoUrl: String
    ): Boolean {
        val now = 1793100000000L
        local.updateResolution(
            id = id,
            workerId = workerId,
            resolutionNotes = resolutionNotes,
            resolutionPhotoUrl = photoUrl,
            updatedAt = now,
            isSynced = 0L
        )

        return try {
            remote.resolveComplaint(
                ResolveComplaintRequestDto(
                    complaintId = id,
                    workerId = workerId,
                    resolutionNotes = resolutionNotes,
                    photoUrl = photoUrl,
                    latitude = 0.0,
                    longitude = 0.0
                )
            )
            local.markComplaintSynced(id)
            true
        } catch (_: Exception) {
            true // Queued offline
        }
    }

    override suspend fun syncOfflineQueue(): Result<Int> {
        return try {
            val unsyncedComplaints = local.getUnsyncedComplaints().map { it.toDomain().toDto() }
            val unsyncedLogs = local.getUnsyncedAuditLogs().map { it.toDomain() }

            if (unsyncedComplaints.isEmpty() && unsyncedLogs.isEmpty()) {
                return Result.success(0)
            }

            val response = remote.syncBatch(
                SyncBatchRequestDto(
                    offlineComplaints = unsyncedComplaints,
                    auditLogs = unsyncedLogs.map { log ->
                        com.example.theekhai.contract.AuditLogDto(
                            id = log.id,
                            complaintId = log.complaintId,
                            actorType = com.example.theekhai.contract.ActorTypeDto.valueOf(log.actorType.name),
                            actorId = log.actorId,
                            action = log.action,
                            reasoning = log.reasoning,
                            decisionPayloadJson = log.decisionPayloadJson,
                            timestampEpochMillis = log.timestampEpochMillis
                        )
                    }
                )
            )

            for (syncedId in response.syncedComplaintIds) {
                local.markComplaintSynced(syncedId)
            }
            for (log in unsyncedLogs) {
                local.markAuditLogSynced(log.id)
            }

            Result.success(response.syncedComplaintIds.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun toEntity(domain: Complaint, isSynced: Long): ComplaintEntity =
        ComplaintEntity(
            id = domain.id,
            title = domain.title,
            description = domain.description,
            category = domain.category,
            priority = domain.priority.name,
            status = domain.status.name,
            latitude = domain.latitude,
            longitude = domain.longitude,
            address = domain.address,
            citizenId = domain.citizenId,
            assignedWorkerId = domain.assignedWorkerId,
            zone = domain.zone,
            photoUrl = domain.photoUrl,
            resolutionNotes = domain.resolutionNotes,
            resolutionPhotoUrl = domain.resolutionPhotoUrl,
            clusterId = domain.clusterId,
            slaDueEpochMillis = domain.slaDueEpochMillis,
            createdAtEpochMillis = domain.createdAtEpochMillis,
            updatedAtEpochMillis = domain.updatedAtEpochMillis,
            isSynced = isSynced
        )
}
