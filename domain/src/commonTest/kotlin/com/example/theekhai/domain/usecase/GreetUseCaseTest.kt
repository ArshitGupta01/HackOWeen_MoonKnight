package com.example.theekhai.domain.usecase

import com.example.theekhai.domain.model.Greeting
import com.example.theekhai.domain.repository.GreetingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeGreetingRepository : GreetingRepository {
    private val history = MutableStateFlow<List<Greeting>>(emptyList())

    override fun observeHistory(): Flow<List<Greeting>> = history

    override suspend fun greet(platformName: String): Greeting {
        val greeting = Greeting(id = history.value.size + 1L, message = "Hi $platformName", createdAtEpochMillis = 0L)
        history.value = listOf(greeting) + history.value
        return greeting
    }
}

class GreetUseCaseTest {
    private val repository = FakeGreetingRepository()

    @Test
    fun trimsPlatformNameAndStoresGreeting() = runTest {
        val greeting = GreetUseCase(repository)("  Android  ")

        assertEquals("Hi Android", greeting.message)
        assertEquals(listOf(greeting), ObserveGreetingHistoryUseCase(repository)().first())
    }

    @Test
    fun rejectsBlankPlatformName() = runTest {
        assertFailsWith<IllegalArgumentException> { GreetUseCase(repository)("   ") }
    }
}
