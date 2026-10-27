package com.example.theekhai.data.remote

import com.example.theekhai.contract.CaseDto
import com.example.theekhai.contract.CasePageDto
import com.example.theekhai.contract.CaseDetailDto
import com.example.theekhai.contract.ActionDto
import com.example.theekhai.contract.ApiErrorDto
import com.example.theekhai.contract.IdentityDto
import com.example.theekhai.contract.LoginDto
import com.example.theekhai.contract.RegisterDto
import com.example.theekhai.contract.ReportDto
import com.example.theekhai.contract.ProfileDto
import com.example.theekhai.contract.SavedSourceDto
import com.example.theekhai.contract.SaveSourceBookmarkDto
import com.example.theekhai.contract.LegalSourceDto
import com.example.theekhai.contract.LegalAnswerDto
import com.example.theekhai.contract.LegalQuestionDto
import com.example.theekhai.contract.ReverseGeocodeResponseDto
import com.example.theekhai.contract.TriageAssistRequestDto
import com.example.theekhai.contract.TriageAssistResponseDto
import com.example.theekhai.data.session.NativeSessionToken
import com.example.theekhai.domain.model.SignedInUser
import com.example.theekhai.domain.model.SubmittedReport
import com.example.theekhai.domain.model.*
import com.example.theekhai.domain.repository.NativeCivicRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.delete
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.uuid.Uuid

class NativeCivicRemoteRepository(
    private val client: HttpClient,
    private val baseUrl: String,
    private val session: NativeSessionToken
) : NativeCivicRepository {
    private fun url(path: String) = "${baseUrl.trimEnd('/')}/api/v1$path"

    override suspend fun signIn(email: String, password: String): SignedInUser {
        val httpResponse = client.post(url("/auth/login")) {
            contentType(ContentType.Application.Json); setBody(LoginDto(email, password))
        }
        requireSuccess(httpResponse)
        val response: Map<String, String> = httpResponse.body()
        session.value = response["accessToken"] ?: error("The server did not return a sign-in token.")
        return currentUser()
    }

    override suspend fun register(email: String, password: String, fullName: String, ward: String): SignedInUser {
        val httpResponse = client.post(url("/auth/register")) {
            contentType(ContentType.Application.Json)
            setBody(RegisterDto(email, password, fullName, ward = ward))
        }
        requireSuccess(httpResponse)
        val response: Map<String, String> = httpResponse.body()
        session.value = response["accessToken"] ?: error("The server did not return a sign-in token.")
        return currentUser()
    }

    private suspend fun currentUser(): SignedInUser {
        val response = client.get(url("/auth/me"))
        requireSuccess(response)
        val identity: IdentityDto = response.body()
        return SignedInUser(identity.id, identity.email, identity.fullName, identity.role)
    }

    override suspend fun submitReport(
        title: String, description: String, category: String, address: String, latitude: Double?, longitude: Double?, consent: Boolean
    ): SubmittedReport {
        val response = client.post(url("/complaints")) {
            contentType(ContentType.Application.Json)
            header("Idempotency-Key", Uuid.random().toString())
            setBody(ReportDto(title, description, category, address, latitude, longitude, consent))
        }
        requireSuccess(response)
        val created: CaseDto = response.body()
        return SubmittedReport(created.id, created.title, created.status)
    }

    override suspend fun signOut() { val response = client.post(url("/auth/logout")); requireSuccess(response); session.value = null }
    override suspend fun getProfile(): CivicProfile { val response = client.get(url("/profile")); requireSuccess(response); return response.body<ProfileDto>().toDomain() }
    override suspend fun saveProfile(profile: CivicProfile): CivicProfile {
        val response = client.post(url("/profile")) { contentType(ContentType.Application.Json); setBody(ProfileDto(fullName = profile.fullName, organization = profile.organization, role = profile.role, department = profile.department, ward = profile.ward, preferredLanguage = profile.preferredLanguage)) }
        requireSuccess(response); return response.body<ProfileDto>().toDomain()
    }
    override suspend fun getSavedSources(): List<SavedCivicSource> { val response = client.get(url("/saved-sources")); requireSuccess(response); return response.body<List<SavedSourceDto>>().map { it.toDomain() } }
    override suspend fun deleteSavedSource(id: String) { val response = client.delete(url("/saved-sources/${id.encodePathSegment()}")); requireSuccess(response) }
    override suspend fun saveSource(source: CivicGuidanceSource): SavedCivicSource {
        val response = client.post(url("/saved-sources")) { contentType(ContentType.Application.Json); setBody(SaveSourceBookmarkDto(source.title, source.url, source.jurisdiction, source.content.take(400))) }
        requireSuccess(response); return response.body<SavedSourceDto>().toDomain()
    }
    override suspend fun getCases(offset: Int): List<Complaint> {
        val response = client.get(url("/complaints")) { parameter("offset", offset.coerceAtLeast(0)) }; requireSuccess(response)
        return response.body<CasePageDto>().items.map { it.toDomain() }
    }
    override suspend fun getCaseDetail(id: String): CivicCaseDetail {
        val response = client.get(url("/complaints/${id.encodePathSegment()}")); requireSuccess(response); return response.body<CaseDetailDto>().toDomain()
    }
    override suspend fun performCaseAction(id: String, action: String, version: Long, note: String, target: String, department: String): CivicCaseDetail {
        val response = client.post(url("/complaints/${id.encodePathSegment()}/actions")) { contentType(ContentType.Application.Json); setBody(ActionDto(action, version, note, target, department)) }
        requireSuccess(response); return getCaseDetail(response.body<CaseDto>().id)
    }
    override suspend fun searchGuidance(query: String, jurisdiction: String): List<CivicGuidanceSource> {
        val response = client.get(url("/legal/sources")) { parameter("q", query); parameter("jurisdiction", jurisdiction) }; requireSuccess(response)
        return response.body<List<LegalSourceDto>>().map { it.toDomain() }
    }
    override suspend fun askGuidance(query: String, jurisdiction: String): CivicGuidanceAnswer {
        val response = client.post(url("/legal/ask")) { contentType(ContentType.Application.Json); setBody(LegalQuestionDto(query, jurisdiction)) }; requireSuccess(response)
        val answer = response.body<LegalAnswerDto>(); return CivicGuidanceAnswer(answer.answer, answer.citations.map { it.toDomain() }, answer.limitation)
    }
    override suspend fun reverseGeocode(latitude: Double, longitude: Double, language: String): CivicLocation {
        val response = client.get(url("/geo/reverse")) { parameter("lat", latitude); parameter("lon", longitude); parameter("lang", language) }; requireSuccess(response)
        return response.body<ReverseGeocodeResponseDto>().let { CivicLocation(it.latitude, it.longitude, it.address, it.ward, it.landmark) }
    }
    override suspend fun triageReport(title: String, description: String, address: String, language: String): CivicTriageSuggestion {
        val response = client.post(url("/complaints/triage")) { contentType(ContentType.Application.Json); setBody(TriageAssistRequestDto(title, description, address, language)) }; requireSuccess(response)
        return response.body<TriageAssistResponseDto>().let { CivicTriageSuggestion(it.suggestedTitle, it.suggestedCategory, it.suggestedPriority, it.suggestedDepartment, it.duplicateCaseIds, it.summary) }
    }

    private suspend fun requireSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val message = runCatching { response.body<ApiErrorDto>().message }.getOrNull()
        throw IllegalStateException(message ?: "Request failed (${response.status.value}).")
    }

    private fun ProfileDto.toDomain() = CivicProfile(userId, email, fullName, organization, role, department, ward, preferredLanguage)
    private fun SavedSourceDto.toDomain() = SavedCivicSource(id, title, sourceUrl, jurisdiction, snippet)
    private fun LegalSourceDto.toDomain() = CivicGuidanceSource(id, title, url, jurisdiction, language, content, retrievedAt)
    private fun CaseDetailDto.toDomain() = CivicCaseDetail(case.toDomain(), tasks.map { CivicCaseTask(it.id, it.department, it.title, it.required, it.complete) }, events.map { CivicCaseEvent(it.id, it.actorId, it.action, it.note, it.at) })
    private fun CaseDto.toDomain() = Complaint(id, title, description, category, runCatching { ComplaintPriority.valueOf(priority) }.getOrDefault(ComplaintPriority.MEDIUM), runCatching { ComplaintStatus.valueOf(status) }.getOrDefault(ComplaintStatus.PENDING), latitude ?: 0.0, longitude ?: 0.0, address, citizenId, zone = department, slaDueEpochMillis = dueAt, createdAtEpochMillis = createdAt, updatedAtEpochMillis = updatedAt, version = version)
}

private fun String.encodePathSegment(): String = encodeToByteArray().joinToString("") { b ->
    val n = b.toInt() and 0xff
    if (n in 0x41..0x5a || n in 0x61..0x7a || n in 0x30..0x39 || n in listOf(0x2d, 0x2e, 0x5f, 0x7e)) n.toChar().toString() else "%" + n.toString(16).uppercase().padStart(2, '0')
}
