package com.example.theekhai.domain.usecase

import com.example.theekhai.contract.ComplaintPriorityDto
import com.example.theekhai.contract.EvidenceCheckRequestDto
import com.example.theekhai.contract.VoiceIntakeRequestDto
import com.example.theekhai.contract.VoiceIntakeResponseDto
import com.example.theekhai.domain.model.*
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.*

private class FakeComplaintRepo(
    private val initialComplaints: List<Complaint> = emptyList()
) : ComplaintRepository {
    val complaints = initialComplaints.toMutableList()

    override fun observeComplaints(): Flow<List<Complaint>> = flowOf(complaints)

    override suspend fun getComplaints(): List<Complaint> = complaints

    override suspend fun getComplaintById(id: String): Complaint? =
        complaints.find { it.id == id }

    override suspend fun createComplaint(complaint: Complaint) {
        complaints.add(complaint)
    }

    override suspend fun updateComplaintStatus(id: String, status: ComplaintStatus) {
        val index = complaints.indexOfFirst { it.id == id }
        if (index != -1) {
            complaints[index] = complaints[index].copy(status = status)
        }
    }

    override suspend fun resolveComplaint(id: String, workerId: String, resolutionNotes: String, photoUrl: String?) {
        val index = complaints.indexOfFirst { it.id == id }
        if (index != -1) {
            complaints[index] = complaints[index].copy(
                status = ComplaintStatus.RESOLVED,
                assignedWorkerId = workerId,
                resolutionNotes = resolutionNotes,
                resolutionPhotoUrl = photoUrl
            )
        }
    }

    override suspend fun syncOfflineComplaints() {}
}

private class FakeAuditRepo : AuditRepository {
    val logs = mutableListOf<AuditLog>()

    override suspend fun recordAuditLog(log: AuditLog) {
        logs.add(log)
    }

    override suspend fun getAuditLogs(complaintId: String?): List<AuditLog> =
        if (complaintId != null) logs.filter { it.complaintId == complaintId } else logs
}

class DomainEdgeCaseTest {

    @Test
    fun voiceIntakeRejectsWhenDpdpConsentNotGranted() = runTest {
        val complaintRepo = FakeComplaintRepo()
        val auditRepo = FakeAuditRepo()
        val sttClient = object : VoiceSpeechToTextClient {
            override suspend fun transcribeAndTranslate(audioBase64: String, mimeType: String, languageHint: String?): VoiceIntakeResponseDto {
                return VoiceIntakeResponseDto("cmp-1", "Original text", "Translated text", "hi", 0.95, ComplaintPriorityDto.HIGH)
            }
        }

        val useCase = VoiceIntakeUseCase(complaintRepo, auditRepo, sttClient)
        val request = VoiceIntakeRequestDto(
            audioBase64 = "UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA=",
            mimeType = "audio/wav",
            citizenId = "cit-edge-99",
            dpdpConsentGranted = false, // Edge case: consent denied
            dpdpConsentText = "Deny consent"
        )

        val result = useCase(request)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("DPDP Act 2026 Violation") == true)
        assertTrue(complaintRepo.complaints.isEmpty())
        assertTrue(auditRepo.logs.isEmpty())
    }

    @Test
    fun voiceIntakeAcceptsWithExplicitConsentAndAuditTrail() = runTest {
        val complaintRepo = FakeComplaintRepo()
        val auditRepo = FakeAuditRepo()
        val sttClient = object : VoiceSpeechToTextClient {
            override suspend fun transcribeAndTranslate(audioBase64: String, mimeType: String, languageHint: String?): VoiceIntakeResponseDto {
                return VoiceIntakeResponseDto("cmp-v1", "टूटी हुई सड़क", "Broken road surface causing hazard", "hi", 0.92, ComplaintPriorityDto.CRITICAL)
            }
        }

        val useCase = VoiceIntakeUseCase(complaintRepo, auditRepo, sttClient)
        val request = VoiceIntakeRequestDto(
            audioBase64 = "UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA=",
            mimeType = "audio/wav",
            citizenId = "cit-101",
            dpdpConsentGranted = true,
            dpdpConsentText = "Explicit DPDP 2026 consent provided"
        )

        val result = useCase(request)
        assertTrue(result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("cmp-v1", created.id)
        assertEquals(ComplaintPriority.CRITICAL, created.priority)
        assertEquals(1, complaintRepo.complaints.size)
        assertEquals(1, auditRepo.logs.size)
        assertEquals("MULTILINGUAL_VOICE_INTAKE", auditRepo.logs.first().action)
    }

    @Test
    fun checkEvidenceFailsWhenGeofenceRadiusExceeded() = runTest {
        val existingComplaint = Complaint(
            id = "cmp-geo-1",
            title = "Pothole on Main Road",
            description = "Hazardous road pothole",
            category = "Roads",
            priority = ComplaintPriority.HIGH,
            status = ComplaintStatus.IN_PROGRESS,
            latitude = 28.6139,
            longitude = 77.2090,
            address = "Connaught Place",
            citizenId = "cit-1",
            zone = "Central",
            slaDueEpochMillis = 1800000000000L,
            createdAtEpochMillis = 1790000000000L,
            updatedAtEpochMillis = 1790000000000L
        )

        val complaintRepo = FakeComplaintRepo(listOf(existingComplaint))
        val auditRepo = FakeAuditRepo()
        val visionClient = object : VisionEvidenceClient {
            override suspend fun analyzeImageEvidence(photoBase64: String, expectedCategory: String): Pair<List<String>, Double> {
                return listOf("Fresh Asphalt Paving") to 0.95
            }
        }

        val useCase = CheckEvidenceUseCase(complaintRepo, auditRepo, visionClient)

        // Field worker is 5 km away (latitude ~28.66 vs 28.6139) -> far exceeds 150m tolerance
        val request = EvidenceCheckRequestDto(
            complaintId = "cmp-geo-1",
            workerId = "wrk-55",
            workerLatitude = 28.6600,
            workerLongitude = 77.2090,
            reportedLatitude = 28.6139,
            reportedLongitude = 77.2090,
            photoBase64 = "dummy-base64",
            maxAllowedRadiusMeters = 150.0
        )

        val result = useCase(request)
        assertTrue(result.isSuccess)
        val check = result.getOrNull()
        assertNotNull(check)
        assertFalse(check.isGpsMatched)
        assertFalse(check.approved)
        assertTrue(check.reasoning.contains("Geofence violation"))
    }

    @Test
    fun checkEvidenceFailsWhenVisualConfidenceBelowThreshold() = runTest {
        val existingComplaint = Complaint(
            id = "cmp-geo-2",
            title = "Drainage Overflow",
            description = "Blocked sewer drain",
            category = "Drainage",
            priority = ComplaintPriority.HIGH,
            status = ComplaintStatus.IN_PROGRESS,
            latitude = 28.6139,
            longitude = 77.2090,
            address = "Connaught Place",
            citizenId = "cit-1",
            zone = "Central",
            slaDueEpochMillis = 1800000000000L,
            createdAtEpochMillis = 1790000000000L,
            updatedAtEpochMillis = 1790000000000L
        )

        val complaintRepo = FakeComplaintRepo(listOf(existingComplaint))
        val auditRepo = FakeAuditRepo()
        val visionClient = object : VisionEvidenceClient {
            override suspend fun analyzeImageEvidence(photoBase64: String, expectedCategory: String): Pair<List<String>, Double> {
                return listOf("Blurry Surface") to 0.45 // Low confidence < 0.70
            }
        }

        val useCase = CheckEvidenceUseCase(complaintRepo, auditRepo, visionClient)

        // GPS is within 50 meters
        val request = EvidenceCheckRequestDto(
            complaintId = "cmp-geo-2",
            workerId = "wrk-55",
            workerLatitude = 28.6140,
            workerLongitude = 77.2091,
            reportedLatitude = 28.6139,
            reportedLongitude = 77.2090,
            photoBase64 = "dummy-base64",
            maxAllowedRadiusMeters = 150.0
        )

        val result = useCase(request)
        assertTrue(result.isSuccess)
        val check = result.getOrNull()
        assertNotNull(check)
        assertTrue(check.isGpsMatched)
        assertFalse(check.approved)
        assertTrue(check.reasoning.contains("below 70% threshold"))
    }

    @Test
    fun checkEvidenceReturnsFailureForNonExistentComplaint() = runTest {
        val complaintRepo = FakeComplaintRepo()
        val auditRepo = FakeAuditRepo()
        val visionClient = object : VisionEvidenceClient {
            override suspend fun analyzeImageEvidence(photoBase64: String, expectedCategory: String) = emptyList<String>() to 1.0
        }

        val useCase = CheckEvidenceUseCase(complaintRepo, auditRepo, visionClient)
        val request = EvidenceCheckRequestDto(
            complaintId = "ghost-id-404",
            workerId = "wrk-1",
            workerLatitude = 28.0,
            workerLongitude = 77.0,
            reportedLatitude = 28.0,
            reportedLongitude = 77.0,
            photoBase64 = "data",
            maxAllowedRadiusMeters = 100.0
        )

        val result = useCase(request)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
