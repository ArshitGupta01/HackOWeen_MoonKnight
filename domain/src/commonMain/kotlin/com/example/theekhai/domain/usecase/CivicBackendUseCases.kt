package com.example.theekhai.domain.usecase

import com.example.theekhai.domain.model.CivicGuidanceSource
import com.example.theekhai.domain.model.CivicProfile
import com.example.theekhai.domain.repository.NativeCivicRepository

class CivicBackendUseCases(private val repository: NativeCivicRepository) {
    suspend fun requestLoginCode(email: String, password: String? = null) = repository.requestLoginCode(email.trim(), password)
    suspend fun verifyLoginCode(email: String, code: String) = repository.verifyLoginCode(email.trim(), code.trim())
    suspend fun instantSignIn(email: String) = repository.instantSignIn(email.trim())
    suspend fun dummyUsers() = repository.getDummyUsers()
    suspend fun leaderboard(roleFilter: String = "ALL") = repository.getLeaderboard(roleFilter)
    suspend fun verifyEmail(token: String, email: String? = null) = repository.verifyEmail(token.trim(), email?.trim())
    suspend fun resendVerification(email: String) = repository.resendVerification(email.trim())
    suspend fun profile() = repository.getProfile()
    suspend fun saveProfile(profile: CivicProfile) = repository.saveProfile(profile)
    suspend fun savedSources() = repository.getSavedSources()
    suspend fun deleteSavedSource(id: String) = repository.deleteSavedSource(id)
    suspend fun saveSource(source: CivicGuidanceSource) = repository.saveSource(source)
    suspend fun cases(offset: Int = 0) = repository.getCases(offset)
    suspend fun caseDetail(id: String) = repository.getCaseDetail(id)
    suspend fun caseAction(id: String, action: String, version: Long, note: String = "", target: String = "", department: String = "") = repository.performCaseAction(id, action, version, note, target, department)
    suspend fun searchGuidance(query: String, jurisdiction: String) = repository.searchGuidance(query, jurisdiction)
    suspend fun askGuidance(query: String, jurisdiction: String) = repository.askGuidance(query, jurisdiction)
    suspend fun reverseGeocode(latitude: Double, longitude: Double, language: String) = repository.reverseGeocode(latitude, longitude, language)
    suspend fun triage(title: String, description: String, address: String, language: String) = repository.triageReport(title, description, address, language)
    suspend fun signOut() = repository.signOut()
    suspend fun verifyCaseWithTinyFish(complaintId: String, portalUrl: String? = null) = repository.verifyCaseWithTinyFish(complaintId, portalUrl)
    suspend fun getMonitors() = repository.getMonitors()
    suspend fun createMonitor(name: String, monitorType: String, target: String, jurisdiction: String) = repository.createMonitor(name, monitorType, target, jurisdiction)
    suspend fun runMonitor(id: String) = repository.runMonitor(id)
    suspend fun discoverSources(query: String, url: String, jurisdiction: String) = repository.discoverSources(query, url, jurisdiction)
    suspend fun getPendingSources() = repository.getPendingSources()
    suspend fun reviewSource(id: String, approve: Boolean) = repository.reviewSource(id, approve)
    suspend fun draftComplaintEmail(caseId: String) = repository.draftComplaintEmail(caseId)
    suspend fun sendComplaintEmail(caseId: String, recipient: String, subject: String, body: String) = repository.sendComplaintEmail(caseId, recipient, subject, body)
}
class DraftComplaintEmailUseCase(private val repository: NativeCivicRepository) {
    suspend operator fun invoke(caseId: String) = repository.draftComplaintEmail(caseId)
}
class SendComplaintEmailUseCase(private val repository: NativeCivicRepository) {
    suspend operator fun invoke(caseId: String, recipientEmail: String, subject: String, body: String) =
        repository.sendComplaintEmail(caseId, recipientEmail, subject, body)
}
