package com.example.theekhai.presentation.greeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.theekhai.Platform
import com.example.theekhai.domain.usecase.GreetUseCase
import com.example.theekhai.domain.usecase.ObserveGreetingHistoryUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel: talks to use cases only (never to repositories, SQL or HTTP directly). */
class GreetingViewModel(
    private val greet: GreetUseCase,
    observeHistory: ObserveGreetingHistoryUseCase,
    private val platform: Platform,
) : ViewModel() {

    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GreetingUiState> =
        combine(observeHistory(), isLoading, errorMessage) { history, loading, error ->
            GreetingUiState(history = history, isLoading = loading, errorMessage = error)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GreetingUiState(),
        )

    fun onGreetClicked() {
        if (isLoading.value) return
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                greet(platform.name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessage.value = e.message ?: "Something went wrong"
            } finally {
                isLoading.value = false
            }
        }
    }
}
