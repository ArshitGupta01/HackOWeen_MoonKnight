package com.example.theekhai.routes

import com.example.theekhai.contract.ActorTypeDto
import com.example.theekhai.contract.ApiRoutes
import com.example.theekhai.contract.AuditLogDto
import com.example.theekhai.contract.SyncBatchRequestDto
import com.example.theekhai.contract.SyncBatchResponseDto
import com.example.theekhai.domain.model.ActorType
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.auditRoutes(
    auditRepository: AuditRepository,
    complaintRepository: ComplaintRepository
) {
    get(ApiRoutes.AUDIT_LOGS) {
        val complaintId = call.request.queryParameters["complaintId"]
        val logs = if (complaintId != null) {
            auditRepository.getAuditLogs(complaintId)
        } else {
            emptyList()
        }

        call.respond(
            logs.map { log ->
                AuditLogDto(
                    id = log.id,
                    complaintId = log.complaintId,
                    actorType = ActorTypeDto.valueOf(log.actorType.name),
                    actorId = log.actorId,
                    action = log.action,
                    reasoning = log.reasoning,
                    decisionPayloadJson = log.decisionPayloadJson,
                    timestampEpochMillis = log.timestampEpochMillis
                )
            }
        )
    }

    post(ApiRoutes.SYNC_BATCH) {
        val request = call.receive<SyncBatchRequestDto>()
        val synced = mutableListOf<String>()

        for (logDto in request.auditLogs) {
            auditRepository.recordAuditLog(
                AuditLog(
                    id = logDto.id,
                    complaintId = logDto.complaintId,
                    actorType = ActorType.valueOf(logDto.actorType.name),
                    actorId = logDto.actorId,
                    action = logDto.action,
                    reasoning = logDto.reasoning,
                    decisionPayloadJson = logDto.decisionPayloadJson,
                    timestampEpochMillis = logDto.timestampEpochMillis
                )
            )
        }

        for (cmp in request.offlineComplaints) {
            synced.add(cmp.id)
        }

        call.respond(
            SyncBatchResponseDto(
                syncedComplaintIds = synced,
                rejectedComplaintIds = emptyList(),
                serverTimestampEpochMillis = System.currentTimeMillis()
            )
        )
    }
}
