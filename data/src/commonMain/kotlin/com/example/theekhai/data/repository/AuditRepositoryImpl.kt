package com.example.theekhai.data.repository

import com.example.theekhai.data.local.ComplaintLocalDataSource
import com.example.theekhai.data.mapper.toDomain
import com.example.theekhai.data.mapper.toEntity
import com.example.theekhai.data.remote.ComplaintRemoteDataSource
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.repository.AuditRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuditRepositoryImpl(
    private val local: ComplaintLocalDataSource,
    private val remote: ComplaintRemoteDataSource
) : AuditRepository {

    override fun observeAuditLogsForComplaint(complaintId: String): Flow<List<AuditLog>> =
        local.observeAuditLogsForComplaint(complaintId).map { list -> list.map { it.toDomain() } }

    override fun observeAllAuditLogs(): Flow<List<AuditLog>> =
        local.observeAllAuditLogs().map { list -> list.map { it.toDomain() } }

    override suspend fun recordAuditLog(log: AuditLog): AuditLog {
        local.insertAuditLog(log.toEntity(isSynced = 0L))
        return log
    }

    override suspend fun getAuditLogs(complaintId: String): List<AuditLog> {
        return try {
            val remoteLogs = remote.getAuditLogs(complaintId)
            remoteLogs.map { dto ->
                AuditLog(
                    id = dto.id,
                    complaintId = dto.complaintId,
                    actorType = com.example.theekhai.domain.model.ActorType.valueOf(dto.actorType.name),
                    actorId = dto.actorId,
                    action = dto.action,
                    reasoning = dto.reasoning,
                    decisionPayloadJson = dto.decisionPayloadJson,
                    timestampEpochMillis = dto.timestampEpochMillis
                )
            }
        } catch (_: Exception) {
            local.getUnsyncedAuditLogs().map { it.toDomain() }.filter { it.complaintId == complaintId }
        }
    }
}
