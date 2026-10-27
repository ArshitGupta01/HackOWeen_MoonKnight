package com.example.theekhai.presentation.nativeconnect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theekhai.domain.model.SignedInUser
import com.example.theekhai.domain.model.SubmittedReport
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
    val message: String? = null
)

class NativeConnectViewModel(
    private val signInUseCase: SignInUseCase,
    private val registerUseCase: RegisterCitizenUseCase,
    private val submitReportUseCase: SubmitCivicReportUseCase
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
