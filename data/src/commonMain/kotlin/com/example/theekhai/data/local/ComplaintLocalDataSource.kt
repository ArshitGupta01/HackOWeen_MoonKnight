package com.example.theekhai.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.data.db.AuditLogEntity
import com.example.theekhai.data.db.ComplaintEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ComplaintLocalDataSource(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher
) {
    fun observeComplaints(): Flow<List<ComplaintEntity>> =
        database.complaintQueries.selectAll().asFlow().mapToList(dispatcher)

    fun observeComplaintById(id: String): Flow<ComplaintEntity?> =
        database.complaintQueries.selectById(id).asFlow().mapToOneOrNull(dispatcher)

    suspend fun getComplaintById(id: String): ComplaintEntity? = withContext(dispatcher) {
        database.complaintQueries.selectById(id).executeAsOneOrNull()
    }

    suspend fun insertOrReplace(entity: ComplaintEntity) = withContext(dispatcher) {
        database.complaintQueries.insertOrReplace(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            category = entity.category,
            priority = entity.priority,
            status = entity.status,
            latitude = entity.latitude,
            longitude = entity.longitude,
            address = entity.address,
            citizenId = entity.citizenId,
            assignedWorkerId = entity.assignedWorkerId,
            zone = entity.zone,
            photoUrl = entity.photoUrl,
            resolutionNotes = entity.resolutionNotes,
            resolutionPhotoUrl = entity.resolutionPhotoUrl,
            clusterId = entity.clusterId,
            slaDueEpochMillis = entity.slaDueEpochMillis,
            createdAtEpochMillis = entity.createdAtEpochMillis,
            updatedAtEpochMillis = entity.updatedAtEpochMillis,
            isSynced = entity.isSynced
        )
    }

    suspend fun updateStatus(id: String, status: String, updatedAt: Long, isSynced: Long) =
        withContext(dispatcher) {
            database.complaintQueries.updateStatus(
                status = status,
                updatedAt = updatedAt,
                isSynced = isSynced,
                id = id
            )
        }

    suspend fun updateTriage(
        id: String,
        clusterId: String?,
        priority: String,
        category: String,
        updatedAt: Long,
        isSynced: Long
    ) = withContext(dispatcher) {
        database.complaintQueries.updateTriage(
            clusterId = clusterId,
            priority = priority,
            category = category,
            updatedAt = updatedAt,
            isSynced = isSynced,
            id = id
        )
    }

    suspend fun updateResolution(
        id: String,
        workerId: String,
        resolutionNotes: String,
        resolutionPhotoUrl: String,
        updatedAt: Long,
        isSynced: Long
    ) = withContext(dispatcher) {
        database.complaintQueries.updateResolution(
            workerId = workerId,
            resolutionNotes = resolutionNotes,
            resolutionPhotoUrl = resolutionPhotoUrl,
            updatedAt = updatedAt,
            isSynced = isSynced,
            id = id
        )
    }

    suspend fun getUnsyncedComplaints(): List<ComplaintEntity> = withContext(dispatcher) {
        database.complaintQueries.selectUnsynced().executeAsList()
    }

    suspend fun markComplaintSynced(id: String) = withContext(dispatcher) {
        database.complaintQueries.markSynced(id)
    }

    // Audit Logs
    fun observeAuditLogsForComplaint(complaintId: String): Flow<List<AuditLogEntity>> =
        database.auditLogQueries.selectByComplaintId(complaintId).asFlow().mapToList(dispatcher)

    fun observeAllAuditLogs(): Flow<List<AuditLogEntity>> =
        database.auditLogQueries.selectAll().asFlow().mapToList(dispatcher)

    suspend fun insertAuditLog(entity: AuditLogEntity) = withContext(dispatcher) {
        database.auditLogQueries.insertAuditLog(
            id = entity.id,
            complaintId = entity.complaintId,
            actorType = entity.actorType,
            actorId = entity.actorId,
            action = entity.action,
            reasoning = entity.reasoning,
            decisionPayloadJson = entity.decisionPayloadJson,
            timestampEpochMillis = entity.timestampEpochMillis,
            isSynced = entity.isSynced
        )
    }

    suspend fun getUnsyncedAuditLogs(): List<AuditLogEntity> = withContext(dispatcher) {
        database.auditLogQueries.selectUnsynced().executeAsList()
    }

    suspend fun markAuditLogSynced(id: String) = withContext(dispatcher) {
        database.auditLogQueries.markSynced(id)
    }
}
