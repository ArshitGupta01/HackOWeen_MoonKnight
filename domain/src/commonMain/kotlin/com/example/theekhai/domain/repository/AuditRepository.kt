package com.example.theekhai.domain.repository

import com.example.theekhai.domain.model.AuditLog
import kotlinx.coroutines.flow.Flow

interface AuditRepository {
    fun observeAuditLogsForComplaint(complaintId: String): Flow<List<AuditLog>>
    fun observeAllAuditLogs(): Flow<List<AuditLog>>
    suspend fun recordAuditLog(log: AuditLog): AuditLog
    suspend fun getAuditLogs(complaintId: String): List<AuditLog>
}
