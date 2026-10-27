package com.example.theekhai.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.usecase.EscalateComplaintUseCase
import com.example.theekhai.domain.usecase.ObserveComplaintsUseCase
import com.example.theekhai.domain.usecase.TriageComplaintUseCase
import com.example.theekhai.domain.usecase.VerifyResolutionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardMetrics(
    val total: Int = 0,
    val pendingTriage: Int = 0,
    val inProgress: Int = 0,
    val escalated: Int = 0,
    val verified: Int = 0
)

data class CommandCenterUiState(
    val complaints: List<Complaint> = emptyList(),
    val filteredComplaints: List<Complaint> = emptyList(),
    val selectedComplaint: Complaint? = null,
    val auditLogs: List<AuditLog> = emptyList(),
    val activeFilter: String = "ALL",
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val actionMessage: String? = null,
    val metrics: DashboardMetrics = DashboardMetrics()
)

class CommandCenterViewModel(
    private val observeComplaintsUseCase: ObserveComplaintsUseCase,
    private val triageComplaintUseCase: TriageComplaintUseCase,
    private val verifyResolutionUseCase: VerifyResolutionUseCase,
    private val escalateComplaintUseCase: EscalateComplaintUseCase,
    private val auditRepository: AuditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommandCenterUiState())
    val uiState: StateFlow<CommandCenterUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            observeComplaintsUseCase().collect { list ->
                val metrics = DashboardMetrics(
                    total = list.size,
                    pendingTriage = list.count { it.status == ComplaintStatus.PENDING },
                    inProgress = list.count { it.status == ComplaintStatus.IN_PROGRESS || it.status == ComplaintStatus.TRIAGED },
                    escalated = list.count { it.status == ComplaintStatus.ESCALATED || it.priority == ComplaintPriority.CRITICAL },
                    verified = list.count { it.status == ComplaintStatus.VERIFIED }
                )

                _uiState.update { current ->
                    val filtered = applyFilter(list, current.activeFilter)
                    current.copy(
                        complaints = list,
                        filteredComplaints = filtered,
                        selectedComplaint = current.selectedComplaint ?: list.firstOrNull(),
                        metrics = metrics
                    )
                }

                _uiState.value.selectedComplaint?.let { selected ->
                    loadAuditLogs(selected.id)
                }
            }
        }
    }

    fun selectComplaint(complaint: Complaint) {
        _uiState.update { it.copy(selectedComplaint = complaint) }
        loadAuditLogs(complaint.id)
    }

    fun filterBy(statusFilter: String) {
        _uiState.update { current ->
            current.copy(
                activeFilter = statusFilter,
                filteredComplaints = applyFilter(current.complaints, statusFilter)
            )
        }
    }

    private fun applyFilter(list: List<Complaint>, filter: String): List<Complaint> {
        return when (filter) {
            "PENDING" -> list.filter { it.status == ComplaintStatus.PENDING }
            "TRIAGED" -> list.filter { it.status == ComplaintStatus.TRIAGED }
            "RESOLVED" -> list.filter { it.status == ComplaintStatus.RESOLVED }
            "VERIFIED" -> list.filter { it.status == ComplaintStatus.VERIFIED }
            "ESCALATED" -> list.filter { it.status == ComplaintStatus.ESCALATED }
            else -> list
        }
    }

    fun triggerAiTriage(complaintId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, actionMessage = "Running Gemini 3.8 Flash triage & clustering...") }
            val result = triageComplaintUseCase(complaintId)
            result.onSuccess { triage ->
                _uiState.update {
                    it.copy(
                        isActionInProgress = false,
                        actionMessage = "AI Triage Complete: Assigned to ${triage.assignedDepartment} (${triage.priority})"
                    )
                }
                loadAuditLogs(complaintId)
            }.onFailure { err ->
                _uiState.update {
                    it.copy(isActionInProgress = false, actionMessage = "Triage Error: ${err.message}")
                }
            }
        }
    }

    fun triggerTinyFishVerification(complaintId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, actionMessage = "Deploying TinyFish autonomous web verification agent...") }
            val result = verifyResolutionUseCase(complaintId)
            result.onSuccess { verif ->
                val msg = if (verif.verified && !verif.contradictionDetected) {
                    "TinyFish Verified Resolution on Municipal Portal!"
                } else {
                    "Contradiction Detected by TinyFish! Complaint auto-reopened & escalated."
                }
                _uiState.update { it.copy(isActionInProgress = false, actionMessage = msg) }
                loadAuditLogs(complaintId)
            }.onFailure { err ->
                _uiState.update { it.copy(isActionInProgress = false, actionMessage = "Verification failed: ${err.message}") }
            }
        }
    }

    fun triggerEscalation(complaintId: String, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true, actionMessage = "Escalating ticket to Municipal Commissioner...") }
            escalateComplaintUseCase(complaintId, reason)
            _uiState.update { it.copy(isActionInProgress = false, actionMessage = "Ticket escalated to CRITICAL priority.") }
            loadAuditLogs(complaintId)
        }
    }

    private fun loadAuditLogs(complaintId: String) {
        viewModelScope.launch {
            val logs = auditRepository.getAuditLogs(complaintId)
            _uiState.update { it.copy(auditLogs = logs) }
        }
    }
}
