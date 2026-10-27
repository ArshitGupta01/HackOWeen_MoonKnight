package com.example.theekhai.presentation.greeting

import com.example.theekhai.domain.model.Greeting

/** Immutable snapshot rendered by the screen (MVVM: View observes, never mutates). */
data class GreetingUiState(
    val history: List<Greeting> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
