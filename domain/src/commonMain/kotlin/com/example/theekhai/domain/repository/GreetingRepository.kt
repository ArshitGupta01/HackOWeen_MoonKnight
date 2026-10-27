package com.example.theekhai.domain.repository

import com.example.theekhai.domain.model.Greeting
import kotlinx.coroutines.flow.Flow

/** Port implemented by the data layer. The domain only ever sees this interface. */
interface GreetingRepository {
    /** Emits the persisted greeting history (newest first) and re-emits on every change. */
    fun observeHistory(): Flow<List<Greeting>>

    /** Obtains a greeting for [platformName], persists it, and returns the stored entity. */
    suspend fun greet(platformName: String): Greeting
}
