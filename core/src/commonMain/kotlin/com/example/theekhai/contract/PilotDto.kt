package com.example.theekhai.contract

import kotlinx.serialization.Serializable

@Serializable data class CaseDto(val id: String, val title: String, val description: String, val category: String, val address: String, val latitude: Double?, val longitude: Double?, val citizenId: String, val department: String, val priority: String, val status: String, val createdAt: Long, val updatedAt: Long, val dueAt: Long, val version: Long, val escalation: Long, val closureBy: String?)
@Serializable data class ReportDto(val title: String, val description: String, val category: String, val address: String, val latitude: Double? = null, val longitude: Double? = null, val consent: Boolean = false, val priority: String = "MEDIUM")
@Serializable data class ActionDto(val action: String, val version: Long, val note: String, val target: String = "", val department: String = "")
@Serializable data class TaskDto(val id: String, val caseId: String, val department: String, val title: String, val required: Boolean, val complete: Boolean)
@Serializable data class EventDto(val id: String, val caseId: String, val actorId: String, val action: String, val note: String, val at: Long)
@Serializable data class CaseLinkDto(val caseId: String, val targetId: String, val kind: String)
@Serializable data class CaseDetailDto(val case: CaseDto, val tasks: List<TaskDto>, val events: List<EventDto>, val links: List<CaseLinkDto> = emptyList())
@Serializable data class CasePageDto(val items: List<CaseDto>, val total: Int, val offset: Int)
@Serializable data class IdentityDto(val id: String, val role: String, val department: String?, val csrf: String? = null, val email: String = "", val fullName: String = "", val organization: String = "", val ward: String = "Ward 1", val preferredLanguage: String = "en")
@Serializable data class AuthConfigDto(val configured: Boolean, val supabaseUrl: String, val publishableKey: String)
@Serializable data class LoginDto(val email: String, val password: String = "", val code: String? = null)
@Serializable data class RequestLoginCodeDto(val email: String, val password: String? = null)
@Serializable data class RequestLoginCodeResponseDto(
    val status: String,
    val email: String,
    val message: String,
    val expiresInSeconds: Int = 600,
    val devCode: String? = null
)
@Serializable data class VerifyLoginCodeDto(val email: String, val code: String)
@Serializable data class VerifyLoginCodeResponseDto(
    val status: String,
    val accessToken: String,
    val email: String,
    val message: String
)
@Serializable data class RegisterDto(val email: String, val password: String, val fullName: String, val organization: String = "Citizen Resident", val ward: String = "Ward 1", val preferredLanguage: String = "en")
@Serializable data class RegisterResponseDto(
    val status: String,
    val email: String,
    val message: String,
    val verificationSent: Boolean = true,
    val devVerificationLink: String? = null,
    val devToken: String? = null,
    val accessToken: String? = null
)
@Serializable data class VerifyEmailDto(val token: String, val email: String? = null)
@Serializable data class VerifyEmailResponseDto(
    val status: String,
    val accessToken: String,
    val email: String,
    val message: String
)
@Serializable data class ResendVerificationDto(val email: String)
@Serializable data class RecoverPasswordDto(val email: String)
@Serializable data class ProfileDto(val userId: String = "", val email: String = "", val fullName: String, val organization: String = "", val role: String = "CITIZEN", val department: String? = null, val ward: String = "Ward 1", val preferredLanguage: String = "en")
@Serializable data class SavedSourceDto(val id: String, val userId: String, val title: String, val sourceUrl: String, val jurisdiction: String, val snippet: String, val createdAt: String = "")
@Serializable data class SaveSourceBookmarkDto(val title: String, val sourceUrl: String, val jurisdiction: String = "CENTRAL", val snippet: String = "")
@Serializable data class ApiErrorDto(val code: String, val message: String, val requestId: String)
@Serializable data class LegalSourceDto(val id: String, val title: String, val url: String, val jurisdiction: String, val language: String, val content: String, val hash: String, val retrievedAt: Long, val status: String, val supersedes: String?)
@Serializable data class LegalQuestionDto(val question: String, val jurisdiction: String = "CENTRAL")
@Serializable data class LegalAnswerDto(val answer: String, val citations: List<LegalSourceDto>, val mode: String, val limitation: String)
@Serializable data class SourceImportDto(val title: String, val url: String, val jurisdiction: String, val language: String, val content: String)
@Serializable data class SourceReviewDto(val approve: Boolean)
@Serializable data class SourceDiscoveryDto(val query: String = "", val url: String = "", val jurisdiction: String = "CENTRAL", val language: String = "en")
@Serializable data class CreateMonitorDto(val name: String, val monitorType: String = "fetch", val target: String, val jurisdiction: String = "CENTRAL", val language: String = "en", val scheduleCron: String = "0 0 * * *")
@Serializable data class LegalMonitorDto(val id: String, val remoteMonitorId: String?, val name: String, val monitorType: String, val target: String, val jurisdiction: String, val language: String, val scheduleCron: String, val status: String, val lastCheckedAt: Long?, val lastStatus: String?)
@Serializable data class MonitorRunResultDto(val monitor: LegalMonitorDto, val queuedSources: List<LegalSourceDto>, val providerMode: String)
@Serializable data class TranscribeRequestDto(val audioBase64: String, val mimeType: String = "audio/webm", val language: String = "en", val transcriptHint: String = "")
@Serializable data class TranscribeResponseDto(val text: String, val suggestedTitle: String = "", val suggestedCategory: String = "", val provider: String = "SARVAM_AI")
@Serializable data class ReverseGeocodeResponseDto(val latitude: Double, val longitude: Double, val address: String, val ward: String = "Ward 4", val landmark: String = "", val source: String = "NOMINATIM_OSM")
@Serializable data class TriageAssistRequestDto(val title: String = "", val description: String = "", val address: String = "", val language: String = "en")
@Serializable data class TriageAssistResponseDto(val suggestedTitle: String, val suggestedCategory: String, val suggestedPriority: String, val suggestedDepartment: String, val duplicateCaseIds: List<String> = emptyList(), val summary: String, val provider: String)
@Serializable data class ProviderStatusDto(val supabaseConfigured: Boolean, val tinyFishConfigured: Boolean, val tavilyConfigured: Boolean, val openRouterConfigured: Boolean, val sarvamConfigured: Boolean = false, val groqConfigured: Boolean = false)

// Gmail & TinyFish Automated Civic Dispatch Contracts
@Serializable data class EmailDraftRequestDto(val caseId: String)
@Serializable data class EmailDraftDto(
    val caseId: String,
    val senderEmail: String = "arshitgupta34@gmail.com",
    val recipientEmail: String,
    val recipientDepartment: String,
    val recipientAuthority: String,
    val officialSourceUrl: String,
    val subject: String,
    val body: String,
    val discoveryMode: String,
    val timestamp: Long = 0L
)
@Serializable data class SendEmailRequestDto(
    val recipientEmail: String,
    val subject: String,
    val body: String
)
@Serializable data class SendEmailResponseDto(
    val success: Boolean,
    val messageId: String,
    val sender: String = "arshitgupta34@gmail.com",
    val recipient: String,
    val sentAt: Long,
    val message: String
)
@Serializable data class GmailOAuthStatusDto(
    val configured: Boolean,
    val connected: Boolean,
    val senderEmail: String = "arshitgupta34@gmail.com",
    val authUrl: String? = null
)


@Serializable data class InstantLoginDto(val email: String)

@Serializable data class DummyUserCredentialDto(
    val id: String,
    val shortName: String,
    val fullName: String,
    val email: String,
    val password: String,
    val role: String,
    val userType: String, // "AUTHORITY" or "REGULAR"
    val department: String? = null,
    val ward: String,
    val score: Int
)

@Serializable data class ScoreEventDto(
    val id: String,
    val userId: String,
    val userName: String,
    val caseId: String? = null,
    val reasonCode: String,
    val pointsDelta: Int,
    val description: String,
    val createdAt: Long
)

@Serializable data class LeaderboardEntryDto(
    val rank: Int,
    val userId: String,
    val fullName: String,
    val email: String,
    val role: String,
    val userType: String, // "AUTHORITY" or "REGULAR"
    val department: String? = null,
    val ward: String,
    val score: Int,
    val actionsTakenCount: Int,
    val complaintsSubmittedCount: Int,
    val complaintsApprovedCount: Int,
    val badge: String
)

@Serializable data class LeaderboardResponseDto(
    val entries: List<LeaderboardEntryDto>,
    val recentActivity: List<ScoreEventDto>,
    val totalParticipants: Int,
    val totalPointsAwarded: Int,
    val updatedAtEpochMillis: Long
)
