package com.example.theekhai.domain.repository

import com.example.theekhai.domain.model.SignedInUser
import com.example.theekhai.domain.model.SubmittedReport

interface NativeCivicRepository {
    suspend fun signIn(email: String, password: String): SignedInUser
    suspend fun register(email: String, password: String, fullName: String, ward: String): SignedInUser
    suspend fun submitReport(title: String, description: String, category: String, address: String, latitude: Double?, longitude: Double?, consent: Boolean): SubmittedReport
}
