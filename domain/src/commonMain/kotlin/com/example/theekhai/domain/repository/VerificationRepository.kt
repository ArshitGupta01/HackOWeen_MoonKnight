package com.example.theekhai.domain.repository

import com.example.theekhai.domain.model.VerificationResult
import kotlinx.coroutines.flow.Flow

interface VerificationRepository {
    fun observeVerificationForComplaint(complaintId: String): Flow<VerificationResult?>
    suspend fun getVerification(complaintId: String): VerificationResult?
    suspend fun triggerAutonomousVerification(
        complaintId: String,
        officialPortalUrl: String? = null,
        resolutionProofUrl: String? = null
    ): Result<VerificationResult>
}
