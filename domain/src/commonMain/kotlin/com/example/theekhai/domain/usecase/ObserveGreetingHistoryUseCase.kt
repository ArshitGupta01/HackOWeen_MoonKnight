package com.example.theekhai.domain.usecase

import com.example.theekhai.domain.model.Greeting
import com.example.theekhai.domain.repository.GreetingRepository
import kotlinx.coroutines.flow.Flow

/** Streams the stored greeting history. */
class ObserveGreetingHistoryUseCase(private val repository: GreetingRepository) {
    operator fun invoke(): Flow<List<Greeting>> = repository.observeHistory()
}
