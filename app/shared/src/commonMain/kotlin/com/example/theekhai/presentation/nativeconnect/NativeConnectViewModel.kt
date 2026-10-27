package com.example.theekhai.presentation.nativeconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theekhai.domain.model.SignedInUser
import com.example.theekhai.domain.model.SubmittedReport
import com.example.theekhai.domain.model.*
import com.example.theekhai.domain.usecase.CivicBackendUseCases
import com.example.theekhai.domain.usecase.RegisterCitizenUseCase
import com.example.theekhai.domain.usecase.SignInUseCase
import com.example.theekhai.domain.usecase.SubmitCivicReportUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NativeConnectState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val user: SignedInUser? = null,
    val report: SubmittedReport? = null,
    val message: String? = null,
    val profile: CivicProfile? = null,
    val savedSources: List<SavedCivicSource> = emptyList(),
    val cases: List<Complaint> = emptyList(),
    val caseDetail: CivicCaseDetail? = null,
    val guidanceSources: List<CivicGuidanceSource> = emptyList(),
    val guidanceAnswer: CivicGuidanceAnswer? = null,
    val location: CivicLocation? = null,
    val triage: CivicTriageSuggestion? = null
)

class NativeConnectViewModel(
    private val signInUseCase: SignInUseCase,
    private val registerUseCase: RegisterCitizenUseCase,
    private val submitReportUseCase: SubmitCivicReportUseCase,
    private val backend: CivicBackendUseCases
) : ViewModel() {
    private val _state = MutableStateFlow(NativeConnectState())
    val state = _state.asStateFlow()

    fun signIn(email: String, password: String) = runRequest {
        val user = signInUseCase(email, password)
        _state.update { it.copy(user = user, message = "Signed in successfully.") }
    }

    fun register(email: String, password: String, fullName: String, ward: String) = runRequest {
        val user = registerUseCase(email, password, fullName, ward)
        _state.update { it.copy(user = user, message = "Account created successfully.") }
    }

    fun submitReport(title: String, description: String, category: String, address: String, latitude: Double?, longitude: Double?, consent: Boolean) = runRequest {
        val report = submitReportUseCase(title, description, category, address, latitude, longitude, consent)
        _state.update { it.copy(report = report, message = "Report submitted. Tracking ID: ${report.id}") }
    }

    fun loadAccount() = runRequest { val profile = backend.profile(); val saved = backend.savedSources(); _state.update { it.copy(profile = profile, savedSources = saved) } }
    fun saveProfile(profile: CivicProfile) = runRequest { val saved = backend.saveProfile(profile); _state.update { it.copy(profile = saved, message = "Profile saved.") } }
    fun removeSavedSource(id: String) = runRequest { backend.deleteSavedSource(id); _state.update { it.copy(savedSources = it.savedSources.filterNot { source -> source.id == id }, message = "Reference removed.") } }
    fun loadCases() = runRequest { _state.update { it.copy(cases = backend.cases(), message = null) } }
    fun loadCase(id: String) = runRequest { _state.update { it.copy(caseDetail = backend.caseDetail(id), message = null) } }
    fun actOnCase(id: String, action: String, version: Long, note: String = "", target: String = "", department: String = "") = runRequest {
        val detail = backend.caseAction(id, action, version, note, target, department)
        val cases = if (_state.value.cases.isEmpty()) emptyList() else backend.cases()
        _state.update { it.copy(caseDetail = detail, cases = cases, message = "Case updated.") }
    }
    fun searchGuidance(query: String, jurisdiction: String) = runRequest { _state.update { it.copy(guidanceSources = backend.searchGuidance(query, jurisdiction), guidanceAnswer = null) } }
    fun askGuidance(query: String, jurisdiction: String) = runRequest { val answer = backend.askGuidance(query, jurisdiction); _state.update { it.copy(guidanceAnswer = answer, guidanceSources = answer.citations) } }
    fun saveGuidance(source: CivicGuidanceSource) = runRequest { val saved = backend.saveSource(source); _state.update { it.copy(savedSources = it.savedSources.filterNot { old -> old.sourceUrl == saved.sourceUrl } + saved, message = "Reference saved.") } }
    fun reverseGeocode(latitude: Double, longitude: Double, language: String = "en") = runRequest { _state.update { it.copy(location = backend.reverseGeocode(latitude, longitude, language)) } }
    fun triage(title: String, description: String, address: String, language: String = "en") = runRequest { _state.update { it.copy(triage = backend.triage(title, description, address, language)) } }
    fun signOut() = runRequest { backend.signOut(); _state.update { NativeConnectState(message = "Signed out.") } }

    private fun runRequest(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, message = null) }
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                _state.update { it.copy(error = failure.message ?: "The request failed. Please try again.") }
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
