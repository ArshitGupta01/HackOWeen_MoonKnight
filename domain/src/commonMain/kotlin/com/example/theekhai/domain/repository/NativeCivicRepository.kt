package com.example.theekhai.domain.repository

import com.example.theekhai.domain.model.SignedInUser
import com.example.theekhai.domain.model.SubmittedReport
import com.example.theekhai.domain.model.*

interface NativeCivicRepository {
    suspend fun signIn(email: String, password: String): SignedInUser
    suspend fun register(email: String, password: String, fullName: String, ward: String): SignedInUser
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
}
