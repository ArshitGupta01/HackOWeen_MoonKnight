package com.example.theekhai.domain.model

data class CivicProfile(val userId: String, val email: String, val fullName: String, val organization: String, val role: String, val department: String?, val ward: String, val preferredLanguage: String)
data class SavedCivicSource(val id: String, val title: String, val sourceUrl: String, val jurisdiction: String, val snippet: String)
data class CivicCaseTask(val id: String, val department: String, val title: String, val required: Boolean, val complete: Boolean)
data class CivicCaseEvent(val id: String, val actorId: String, val action: String, val note: String, val at: Long)
data class CivicCaseDetail(val complaint: Complaint, val tasks: List<CivicCaseTask>, val events: List<CivicCaseEvent>)
data class CivicGuidanceSource(val id: String, val title: String, val url: String, val jurisdiction: String, val language: String, val content: String, val retrievedAt: Long)
data class CivicGuidanceAnswer(val answer: String, val citations: List<CivicGuidanceSource>, val limitation: String)
data class CivicLocation(val latitude: Double, val longitude: Double, val address: String, val ward: String, val landmark: String)
data class CivicTriageSuggestion(val title: String, val category: String, val priority: String, val department: String, val similarCaseIds: List<String>, val summary: String)
