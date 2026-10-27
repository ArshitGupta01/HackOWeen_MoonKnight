package com.example.theekhai.domain.usecase

import com.example.theekhai.domain.model.Greeting
import com.example.theekhai.domain.repository.GreetingRepository

/** Produces and stores a new greeting for the given platform. */
class GreetUseCase(private val repository: GreetingRepository) {
    suspend operator fun invoke(platformName: String): Greeting {
        require(platformName.isNotBlank()) { "platformName must not be blank" }
        return repository.greet(platformName.trim())
    }
}
