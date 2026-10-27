package com.example.theekhai.data.remote

import com.example.theekhai.contract.TriggerVerificationRequestDto
import com.example.theekhai.contract.VerificationResultDto
import com.example.theekhai.contract.VerificationStateDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class TinyFishApiClient(
    private val client: HttpClient? = null,
    private val backendBaseUrl: String = ""
) {
    suspend fun verifyResolutionOnPortal(
        complaintId: String,
        targetPortalUrl: String?,
        resolutionProofUrl: String?
    ): VerificationResultDto {
        if (client != null && backendBaseUrl.isNotBlank()) {
            try {
                val response = client.post("${backendBaseUrl.trimEnd('/')}/api/v1/complaints/$complaintId/verify") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        TriggerVerificationRequestDto(
                            complaintId = complaintId,
                            officialPortalUrl = targetPortalUrl,
                            resolutionProofUrl = resolutionProofUrl
                        )
                    )
                }
                if (response.status.isSuccess()) {
                    return response.body<VerificationResultDto>()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Fall through to offline fallback
            }
        }

        val portal = targetPortalUrl ?: "https://haryana-rtsc.gov.in/guidelines"
        return VerificationResultDto(
            id = "tinyfish-verif-$complaintId",
            complaintId = complaintId,
            state = VerificationStateDto.MANUAL_INSPECTION_REQUIRED,
            verified = false,
            scrapedPortalUrl = portal,
            scrapedEvidenceSnippet = "Offline fallback: live TinyFish verification could not reach backend for case #$complaintId.",
            contradictionDetected = false,
            explanation = "Backend verification endpoint was unreachable; manual reviewer verification is required.",
            verifiedAtEpochMillis = kotlin.time.Clock.System.now().toEpochMilliseconds()
        )
    }
}
