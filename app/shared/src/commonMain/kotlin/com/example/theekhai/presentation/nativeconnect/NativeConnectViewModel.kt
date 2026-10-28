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
    val triage: CivicTriageSuggestion? = null,
    val monitors: List<com.example.theekhai.contract.LegalMonitorDto> = emptyList(),
    val pendingSources: List<CivicGuidanceSource> = emptyList(),
    val verificationResult: com.example.theekhai.contract.VerificationResultDto? = null,
    val emailDraft: com.example.theekhai.contract.EmailDraftDto? = null,
    val emailSendResult: com.example.theekhai.contract.SendEmailResponseDto? = null,
    val leaderboard: com.example.theekhai.contract.LeaderboardResponseDto? = null,
    val dummyUsers: List<com.example.theekhai.contract.DummyUserCredentialDto> = emptyList(),
    val loginCodeSent: Boolean = false,
    val pendingLoginEmail: String? = null,
    val loginDevCode: String? = null,
    val signupVerificationSent: Boolean = false,
    val pendingSignupEmail: String? = null,
    val signupDevToken: String? = null,
    val signupDevLink: String? = null,
    val backendUrl: String = "https://bless-stinger-plow.ngrok-free.dev"
)

class NativeConnectViewModel(
    private val signInUseCase: SignInUseCase,
    private val registerUseCase: RegisterCitizenUseCase,
    private val submitReportUseCase: SubmitCivicReportUseCase,
    private val backend: CivicBackendUseCases
) : ViewModel() {
    private val _state = MutableStateFlow(NativeConnectState(backendUrl = backend.getBackendUrl()))
    val state = _state.asStateFlow()

    fun updateBackendUrl(newUrl: String) {
        backend.setBackendUrl(newUrl)
        _state.update { it.copy(backendUrl = backend.getBackendUrl(), message = "Connected to $newUrl") }
    }

    fun signIn(email: String, password: String) = runRequest {
        val user = signInUseCase(email, password)
        _state.update { it.copy(user = user, message = "Signed in successfully.") }
    }

    fun requestLoginCode(email: String, password: String? = null) = runRequest {
        val res = backend.requestLoginCode(email, password)
        _state.update {
            it.copy(
                loginCodeSent = true,
                pendingLoginEmail = res.email,
                loginDevCode = res.devCode,
                message = res.message
            )
        }
    }

    fun verifyLoginCode(email: String, code: String) = runRequest {
        val user = backend.verifyLoginCode(email, code)
        _state.update {
            it.copy(
                user = user,
                loginCodeSent = false,
                pendingLoginEmail = null,
                loginDevCode = null,
                message = "Signed in successfully with verified email."
            )
        }
    }

    fun resetLoginStep() {
        _state.update { it.copy(loginCodeSent = false, pendingLoginEmail = null, loginDevCode = null) }
    }

    fun register(email: String, password: String, fullName: String, ward: String) = runRequest {
        val user = registerUseCase(email, password, fullName, ward)
        _state.update { it.copy(user = user, message = "Account created successfully.") }
    }

    fun verifyEmail(token: String, email: String? = null) = runRequest {
        val user = backend.verifyEmail(token, email)
        _state.update {
            it.copy(
                user = user,
                signupVerificationSent = false,
                pendingSignupEmail = null,
                signupDevToken = null,
                signupDevLink = null,
                message = "Email successfully verified. Welcome to TheekHai!"
            )
        }
    }

    fun resendVerification(email: String) = runRequest {
        val msg = backend.resendVerification(email)
        _state.update { it.copy(message = msg) }
    }

    fun submitReport(
        title: String,
        description: String,
        category: String,
        address: String,
        latitude: Double?,
        longitude: Double?,
        consent: Boolean,
        photoUrl: String? = null
    ) = runRequest {
        val report = submitReportUseCase(title, description, category, address, latitude, longitude, consent, photoUrl)
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
    fun verifyCaseWithTinyFish(complaintId: String, portalUrl: String? = null) = runRequest {
        val result = backend.verifyCaseWithTinyFish(complaintId, portalUrl)
        _state.update { it.copy(verificationResult = result, message = "TinyFish verified [${result.state}]: ${result.explanation}") }
    }
    fun loadOpsData() = runRequest {
        val monitors = backend.getMonitors()
        val pending = backend.getPendingSources()
        _state.update { it.copy(monitors = monitors, pendingSources = pending) }
    }
    fun createMonitor(name: String, monitorType: String, target: String, jurisdiction: String) = runRequest {
        backend.createMonitor(name, monitorType, target, jurisdiction)
        val monitors = backend.getMonitors()
        _state.update { it.copy(monitors = monitors, message = "Monitor created.") }
    }
    fun runMonitor(id: String) = runRequest {
        val res = backend.runMonitor(id)
        val monitors = backend.getMonitors()
        _state.update { it.copy(monitors = monitors, message = "Monitor run (${res.providerMode}): ${res.queuedSources.size} source(s) queued.") }
    }
    fun discoverSources(query: String, url: String, jurisdiction: String) = runRequest {
        val queued = backend.discoverSources(query, url, jurisdiction)
        val pending = backend.getPendingSources()
        _state.update { it.copy(pendingSources = pending, message = "Queued ${queued.size} source(s) for review.") }
    }
    fun reviewSource(id: String, approve: Boolean) = runRequest {
        backend.reviewSource(id, approve)
        val pending = backend.getPendingSources()
        _state.update { it.copy(pendingSources = pending, message = if (approve) "Source approved." else "Source rejected.") }
    }
    fun draftEmail(caseId: String) = runRequest {
        val draft = backend.draftComplaintEmail(caseId)
        _state.update { it.copy(emailDraft = draft, message = "Official authority email drafted for ${draft.recipientAuthority} (${draft.recipientEmail})") }
    }
    fun sendEmail(caseId: String, recipient: String, subject: String, body: String) = runRequest {
        val result = backend.sendComplaintEmail(caseId, recipient, subject, body)
        _state.update { it.copy(emailSendResult = result, message = "✓ ${result.message}") }
        loadCase(caseId)
    }
    fun instantSignIn(email: String) = runRequest {
        val user = backend.instantSignIn(email)
        val profile = runCatching { backend.profile() }.getOrNull()
        _state.update { it.copy(user = user, profile = profile, message = "Signed in as ${user.fullName}.") }
    }
    fun loadLeaderboard(roleFilter: String = "ALL") = runRequest {
        val board = backend.leaderboard(roleFilter)
        _state.update { it.copy(leaderboard = board) }
    }
    fun loadDummyUsers() = runRequest {
        val users = backend.dummyUsers()
        _state.update { it.copy(dummyUsers = users) }
    }
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
