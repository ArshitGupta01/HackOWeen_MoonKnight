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
    private val syncOfflineComplaintsUseCase: SyncOfflineComplaintsUseCase
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
        _uiState.update {
            it.copy(isRecording = false, recordedAudioBase64 = null, syncStatusMessage = "Audio capture is not connected on this platform. No recording was made.")
        }
    }

    fun submitVoiceGrievance() {
        val message = if (!_uiState.value.dpdpConsentAccepted) {
            "Consent is required before audio can be processed."
        } else {
            "Voice intake is not connected on this platform. No report was sent."
        }
        _uiState.update { it.copy(isSubmitting = false, recordedAudioBase64 = null, syncStatusMessage = message) }
    }

    fun resolveWorkOrderWithEvidence(
        complaintId: String,
        resolutionNotes: String,
        photoBase64: String = ""
    ) {
        val status = if (photoBase64.isBlank()) {
            "A real site photo and device location are required. No resolution was submitted."
        } else {
            "Evidence upload and location verification are not connected on this platform. No resolution was submitted."
        }
        _uiState.update { it.copy(isSubmitting = false, evidenceVerificationStatus = status) }
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
