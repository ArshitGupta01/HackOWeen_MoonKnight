package com.example.theekhai.domain.usecase

import com.example.theekhai.domain.model.CivicGuidanceSource
import com.example.theekhai.domain.model.CivicProfile
import com.example.theekhai.domain.repository.NativeCivicRepository

class CivicBackendUseCases(private val repository: NativeCivicRepository) {
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
}
