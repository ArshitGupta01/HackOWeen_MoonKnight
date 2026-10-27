package com.example.theekhai.contract

import kotlinx.serialization.Serializable

@Serializable data class CaseDto(val id: String, val title: String, val description: String, val category: String, val address: String, val latitude: Double?, val longitude: Double?, val citizenId: String, val department: String, val priority: String, val status: String, val createdAt: Long, val updatedAt: Long, val dueAt: Long, val version: Long, val escalation: Long, val closureBy: String?)
@Serializable data class ReportDto(val title: String, val description: String, val category: String, val address: String, val latitude: Double? = null, val longitude: Double? = null, val consent: Boolean = false)
@Serializable data class ActionDto(val action: String, val version: Long, val note: String, val target: String = "", val department: String = "")
@Serializable data class TaskDto(val id: String, val caseId: String, val department: String, val title: String, val required: Boolean, val complete: Boolean)
@Serializable data class EventDto(val id: String, val caseId: String, val actorId: String, val action: String, val note: String, val at: Long)
@Serializable data class CaseDetailDto(val case: CaseDto, val tasks: List<TaskDto>, val events: List<EventDto>)
@Serializable data class CasePageDto(val items: List<CaseDto>, val total: Int, val offset: Int)
@Serializable data class IdentityDto(val id: String, val role: String, val department: String?, val csrf: String? = null)
@Serializable data class LoginDto(val email: String, val password: String)
@Serializable data class ApiErrorDto(val code: String, val message: String, val requestId: String)
@Serializable data class LegalSourceDto(val id: String, val title: String, val url: String, val jurisdiction: String, val language: String, val content: String, val hash: String, val retrievedAt: Long, val status: String, val supersedes: String?)
@Serializable data class LegalQuestionDto(val question: String, val jurisdiction: String = "CENTRAL")
@Serializable data class LegalAnswerDto(val answer: String, val citations: List<LegalSourceDto>, val mode: String, val limitation: String)
@Serializable data class SourceImportDto(val title: String, val url: String, val jurisdiction: String, val language: String, val content: String)
@Serializable data class SourceReviewDto(val approve: Boolean)
