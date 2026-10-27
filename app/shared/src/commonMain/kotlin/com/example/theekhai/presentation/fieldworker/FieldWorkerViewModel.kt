package com.example.theekhai.presentation.fieldworker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.domain.usecase.ObserveComplaintsUseCase
import com.example.theekhai.domain.usecase.SyncOfflineComplaintsUseCase
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
    val dpdpConsentAccepted: Boolean = false,
    val isSubmitting: Boolean = false,
    val syncStatusMessage: String? = null,
    val evidenceVerificationStatus: String? = null,
    val unsyncedCount: Int = 0
)

class FieldWorkerViewModel(
    private val observeComplaintsUseCase: ObserveComplaintsUseCase,
    private val syncOfflineComplaintsUseCase: SyncOfflineComplaintsUseCase,
    private val complaintRepository: com.example.theekhai.domain.repository.ComplaintRepository? = null,
    private val checkEvidenceUseCase: com.example.theekhai.domain.usecase.CheckEvidenceUseCase? = null,
    private val voiceIntakeUseCase: com.example.theekhai.domain.usecase.VoiceIntakeUseCase? = null
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
                    it.status == ComplaintStatus.PENDING ||
                    it.status == ComplaintStatus.ASSIGNED
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
        _uiState.update { it.copy(isRecording = !it.isRecording) }
    }

    fun setRecordedAudio(base64: String?) {
        _uiState.update { it.copy(recordedAudioBase64 = base64, isRecording = false) }
    }

    fun submitVoiceGrievance() {
        val state = _uiState.value
        if (!state.dpdpConsentAccepted) {
            _uiState.update { it.copy(syncStatusMessage = "Consent is required before audio can be processed.") }
            return
        }
        val audioB64 = state.recordedAudioBase64
        if (audioB64.isNullOrBlank()) {
            _uiState.update { it.copy(syncStatusMessage = "Record a voice note first.") }
            return
        }
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            if (voiceIntakeUseCase != null) {
                val res = voiceIntakeUseCase(
                    com.example.theekhai.contract.VoiceIntakeRequestDto(
                        audioBase64 = audioB64,
                        mimeType = "audio/wav",
                        languageHint = "hi-IN",
                        citizenId = "citizen-fieldworker",
                        zone = "Ward 4",
                        latitude = 28.4595,
                        longitude = 77.0266,
                        address = "Ward 4 Inspection Area",
                        dpdpConsentGranted = true
                    )
                )
                res.onSuccess { complaint ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            recordedAudioBase64 = null,
                            syncStatusMessage = "Voice intake processed [${complaint.category}]: ${complaint.title}"
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { it.copy(isSubmitting = false, syncStatusMessage = "Voice intake error: ${err.message}") }
                }
            } else {
                _uiState.update { it.copy(isSubmitting = false, syncStatusMessage = "Voice intake recorded locally.") }
            }
        }
    }

    fun resolveWorkOrderWithEvidence(
        complaintId: String,
        resolutionNotes: String,
        photoBase64: String = "",
        workerLat: Double = 28.4595,
        workerLon: Double = 77.0266
    ) {
        val target = _uiState.value.assignedComplaints.find { it.id == complaintId }
        val repLat = target?.latitude ?: workerLat
        val repLon = target?.longitude ?: workerLon

        _uiState.update { it.copy(isSubmitting = true, evidenceVerificationStatus = "Validating geofence & resolution evidence...") }
        viewModelScope.launch {
            if (checkEvidenceUseCase != null) {
                val check = checkEvidenceUseCase(
                    com.example.theekhai.contract.EvidenceCheckRequestDto(
                        complaintId = complaintId,
                        photoBase64 = photoBase64,
                        reportedLatitude = repLat,
                        reportedLongitude = repLon,
                        workerLatitude = workerLat,
                        workerLongitude = workerLon
                    )
                )
                check.onSuccess { ev ->
                    if (complaintRepository != null) {
                        complaintRepository.resolveComplaint(complaintId, "worker-1", resolutionNotes, photoBase64)
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            evidenceVerificationStatus = "Verified (${if (ev.isGpsMatched) "GPS Matched" else "Warning: Distance ${ev.distanceMeters.toInt()}m"}): ${ev.reasoning}"
                        )
                    }
                }.onFailure { err ->
                    _uiState.update { it.copy(isSubmitting = false, evidenceVerificationStatus = "Evidence validation error: ${err.message}") }
                }
            } else {
                if (complaintRepository != null) {
                    complaintRepository.resolveComplaint(complaintId, "worker-1", resolutionNotes, photoBase64)
                }
                _uiState.update { it.copy(isSubmitting = false, evidenceVerificationStatus = "Work order resolved successfully.") }
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
