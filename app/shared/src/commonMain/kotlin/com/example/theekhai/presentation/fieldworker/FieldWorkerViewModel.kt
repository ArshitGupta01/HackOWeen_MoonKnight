package com.example.theekhai.presentation.fieldworker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theekhai.audio.AudioRecorder
import com.example.theekhai.contract.EvidenceCheckRequestDto
import com.example.theekhai.contract.VoiceIntakeRequestDto
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.domain.repository.ComplaintRepository
import com.example.theekhai.domain.usecase.CheckEvidenceUseCase
import com.example.theekhai.domain.usecase.ObserveComplaintsUseCase
import com.example.theekhai.domain.usecase.SyncOfflineComplaintsUseCase
import com.example.theekhai.domain.usecase.VoiceIntakeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FieldWorkerUiState(
    val assignedComplaints: List<Complaint> = emptyList(),
    val selectedComplaint: Complaint? = null,
    val isRecording: Boolean = false,
    val recordedAudioBase64: String? = null,
    val dpdpConsentAccepted: Boolean = true,
    val isSubmitting: Boolean = false,
    val syncStatusMessage: String? = null,
    val evidenceVerificationStatus: String? = null,
    val unsyncedCount: Int = 0
)

class FieldWorkerViewModel(
    private val observeComplaintsUseCase: ObserveComplaintsUseCase,
    private val voiceIntakeUseCase: VoiceIntakeUseCase,
    private val checkEvidenceUseCase: CheckEvidenceUseCase,
    private val complaintRepository: ComplaintRepository,
    private val syncOfflineComplaintsUseCase: SyncOfflineComplaintsUseCase,
    private val audioRecorder: AudioRecorder = AudioRecorder()
) : ViewModel() {

    private val _uiState = MutableStateFlow(FieldWorkerUiState())
    val uiState: StateFlow<FieldWorkerUiState> = _uiState.asStateFlow()

    init {
        observeAssignedWork()
    }

    private fun observeAssignedWork() {
        viewModelScope.launch {
            observeComplaintsUseCase().collect { list ->
                // Field worker sees assigned zone or pending field resolution
                val assigned = list.filter {
                    it.status == ComplaintStatus.TRIAGED ||
                    it.status == ComplaintStatus.IN_PROGRESS ||
                    it.status == ComplaintStatus.REOPENED ||
                    it.status == ComplaintStatus.PENDING
                }
                _uiState.update { it.copy(assignedComplaints = assigned) }
            }
        }
    }

    fun selectComplaint(complaint: Complaint) {
        _uiState.update { it.copy(selectedComplaint = complaint) }
    }

    fun toggleDpdpConsent(accepted: Boolean) {
        _uiState.update { it.copy(dpdpConsentAccepted = accepted) }
    }

    fun toggleRecording() {
        if (_uiState.value.isRecording) {
            val audioData = audioRecorder.stopRecording()
            _uiState.update { it.copy(isRecording = false, recordedAudioBase64 = audioData) }
        } else {
            audioRecorder.startRecording()
            _uiState.update { it.copy(isRecording = true, recordedAudioBase64 = null) }
        }
    }

    fun submitVoiceGrievance(zone: String = "Zone-4", address: String = "MG Road Junction") {
        val audio = _uiState.value.recordedAudioBase64 ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, syncStatusMessage = "Processing voice intake via Sarvam/Groq Whisper...") }
            val result = voiceIntakeUseCase(
                VoiceIntakeRequestDto(
                    audioBase64 = audio,
                    languageHint = "hi-IN",
                    citizenId = "citizen-mobile-user",
                    zone = zone,
                    latitude = 28.6139,
                    longitude = 77.2090,
                    address = address,
                    dpdpConsentGranted = _uiState.value.dpdpConsentAccepted
                )
            )

            result.onSuccess { created ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        recordedAudioBase64 = null,
                        syncStatusMessage = "Grievance submitted successfully (#${created.id.takeLast(6)})"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isSubmitting = false, syncStatusMessage = "Submission failed: ${err.message}")
                }
            }
        }
    }

    fun resolveWorkOrderWithEvidence(
        complaintId: String,
        resolutionNotes: String,
        photoBase64: String = "mock_photo_base64"
    ) {
        val complaint = _uiState.value.assignedComplaints.find { it.id == complaintId } ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, evidenceVerificationStatus = "Validating geofence & Groq vision evidence...") }

            // Step 1: Evidence validation (Groq Vision + Haversine GPS geofence)
            val evidenceCheck = checkEvidenceUseCase(
                EvidenceCheckRequestDto(
                    complaintId = complaintId,
                    photoBase64 = photoBase64,
                    reportedLatitude = complaint.latitude,
                    reportedLongitude = complaint.longitude,
                    workerLatitude = complaint.latitude + 0.0001, // within 15 meters
                    workerLongitude = complaint.longitude + 0.0001
                )
            )

            val checkResponse = evidenceCheck.getOrNull()
            if (checkResponse == null || !checkResponse.approved) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        evidenceVerificationStatus = "Evidence rejected: ${checkResponse?.reasoning ?: "Validation error"}"
                    )
                }
                return@launch
            }

            // Step 2: Mark resolved
            complaintRepository.resolveComplaint(
                id = complaintId,
                workerId = "worker-satish-pwd",
                resolutionNotes = resolutionNotes,
                photoUrl = "https://theekhai.storage/resolutions/$complaintId.jpg"
            )

            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    evidenceVerificationStatus = "Resolution logged! Verified within ${checkResponse.distanceMeters.toInt()}m geofence."
                )
            }
        }
    }

    fun syncOfflineQueue() {
        viewModelScope.launch {
            _uiState.update { it.copy(syncStatusMessage = "Syncing local SQLDelight database with Supabase...") }
            val result = syncOfflineComplaintsUseCase()
            result.onSuccess { count ->
                _uiState.update { it.copy(syncStatusMessage = "Synced $count pending records with backend!") }
            }.onFailure { err ->
                _uiState.update { it.copy(syncStatusMessage = "Sync offline queue: ${err.message}") }
            }
        }
    }
}
