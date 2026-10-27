package com.example.theekhai.data.repository

import com.example.theekhai.contract.TriggerVerificationRequestDto
import com.example.theekhai.contract.VerificationResultDto
import com.example.theekhai.contract.VerificationStateDto
import com.example.theekhai.data.remote.ComplaintRemoteDataSource
import com.example.theekhai.domain.model.VerificationResult
import com.example.theekhai.domain.model.VerificationState
import com.example.theekhai.domain.repository.VerificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class VerificationRepositoryImpl(
    private val remote: ComplaintRemoteDataSource
) : VerificationRepository {

    private val verificationsState = MutableStateFlow<Map<String, VerificationResult>>(emptyMap())

    override fun observeVerificationForComplaint(complaintId: String): Flow<VerificationResult?> =
        verificationsState.map { it[complaintId] }

    override suspend fun getVerification(complaintId: String): VerificationResult? =
        verificationsState.value[complaintId]

    override suspend fun triggerAutonomousVerification(
        complaintId: String,
        officialPortalUrl: String?,
        resolutionProofUrl: String?
    ): Result<VerificationResult> {
        return try {
            val dto = remote.triggerVerification(
                TriggerVerificationRequestDto(
                    complaintId = complaintId,
                    officialPortalUrl = officialPortalUrl,
                    resolutionProofUrl = resolutionProofUrl
                )
            )
            val result = dto.toDomain()
            verificationsState.value = verificationsState.value + (complaintId to result)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun VerificationResultDto.toDomain(): VerificationResult = VerificationResult(
        id = id,
        complaintId = complaintId,
        state = when (state) {
            VerificationStateDto.IN_PROGRESS -> VerificationState.IN_PROGRESS
            VerificationStateDto.CONFIRMED_RESOLVED -> VerificationState.CONFIRMED_RESOLVED
            VerificationStateDto.CONTRADICTION_FOUND -> VerificationState.CONTRADICTION_FOUND
            VerificationStateDto.MANUAL_INSPECTION_REQUIRED -> VerificationState.MANUAL_INSPECTION_REQUIRED
        },
        verified = verified,
        scrapedPortalUrl = scrapedPortalUrl,
        scrapedEvidenceSnippet = scrapedEvidenceSnippet,
        contradictionDetected = contradictionDetected,
        explanation = explanation,
        verifiedAtEpochMillis = verifiedAtEpochMillis
    )
}
