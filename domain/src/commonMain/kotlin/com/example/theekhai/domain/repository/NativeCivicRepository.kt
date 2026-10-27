package com.example.theekhai.domain.repository

import com.example.theekhai.domain.model.SignedInUser
import com.example.theekhai.domain.model.SubmittedReport
import com.example.theekhai.domain.model.*

interface NativeCivicRepository {
    suspend fun signIn(email: String, password: String): SignedInUser
    suspend fun requestLoginCode(email: String, password: String? = null): com.example.theekhai.contract.RequestLoginCodeResponseDto
    suspend fun verifyLoginCode(email: String, code: String): SignedInUser
    suspend fun instantSignIn(email: String): SignedInUser
    suspend fun getDummyUsers(): List<com.example.theekhai.contract.DummyUserCredentialDto>
    suspend fun getLeaderboard(roleFilter: String = "ALL"): com.example.theekhai.contract.LeaderboardResponseDto
    suspend fun register(email: String, password: String, fullName: String, ward: String): SignedInUser
    suspend fun verifyEmail(token: String, email: String? = null): SignedInUser
    suspend fun resendVerification(email: String): String
    suspend fun submitReport(title: String, description: String, category: String, address: String, latitude: Double?, longitude: Double?, consent: Boolean): SubmittedReport
    suspend fun signOut()
    suspend fun getProfile(): CivicProfile
    suspend fun saveProfile(profile: CivicProfile): CivicProfile
    suspend fun getSavedSources(): List<SavedCivicSource>
    suspend fun deleteSavedSource(id: String)
    suspend fun saveSource(source: CivicGuidanceSource): SavedCivicSource
    suspend fun getCases(offset: Int = 0): List<Complaint>
    suspend fun getCaseDetail(id: String): CivicCaseDetail
    suspend fun performCaseAction(id: String, action: String, version: Long, note: String = "", target: String = "", department: String = ""): CivicCaseDetail
    suspend fun searchGuidance(query: String, jurisdiction: String): List<CivicGuidanceSource>
    suspend fun askGuidance(query: String, jurisdiction: String): CivicGuidanceAnswer
    suspend fun reverseGeocode(latitude: Double, longitude: Double, language: String): CivicLocation
    suspend fun triageReport(title: String, description: String, address: String, language: String): CivicTriageSuggestion
    suspend fun verifyCaseWithTinyFish(complaintId: String, portalUrl: String? = null): com.example.theekhai.contract.VerificationResultDto
    suspend fun getMonitors(): List<com.example.theekhai.contract.LegalMonitorDto>
    suspend fun createMonitor(name: String, monitorType: String, target: String, jurisdiction: String): com.example.theekhai.contract.LegalMonitorDto
    suspend fun runMonitor(id: String): com.example.theekhai.contract.MonitorRunResultDto
    suspend fun discoverSources(query: String, url: String, jurisdiction: String): List<CivicGuidanceSource>
    suspend fun getPendingSources(): List<CivicGuidanceSource>
    suspend fun reviewSource(id: String, approve: Boolean)
    suspend fun draftComplaintEmail(caseId: String): com.example.theekhai.contract.EmailDraftDto
    suspend fun sendComplaintEmail(caseId: String, recipientEmail: String, subject: String, body: String): com.example.theekhai.contract.SendEmailResponseDto
}
