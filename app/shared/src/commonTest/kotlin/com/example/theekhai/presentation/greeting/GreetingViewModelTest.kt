package com.example.theekhai.presentation.greeting

import com.example.theekhai.Platform
import com.example.theekhai.domain.model.Greeting
import com.example.theekhai.domain.repository.GreetingRepository
import com.example.theekhai.domain.usecase.GreetUseCase
import com.example.theekhai.domain.usecase.ObserveGreetingHistoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeRepository(private val failure: Exception? = null) : GreetingRepository {
    private val history = MutableStateFlow<List<Greeting>>(emptyList())

    override fun observeHistory(): Flow<List<Greeting>> = history

    override suspend fun greet(platformName: String): Greeting {
        failure?.let { throw it }
        val greeting = Greeting(history.value.size + 1L, "Hello, $platformName!", 0L)
        history.value = listOf(greeting) + history.value
        return greeting
    }
}

private object FakePlatform : Platform {
    override val name: String = "Test"
}

@OptIn(ExperimentalCoroutinesApi::class)
class GreetingViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: GreetingRepository) =
        GreetingViewModel(GreetUseCase(repository), ObserveGreetingHistoryUseCase(repository), FakePlatform)

    @Test
    fun clickAddsGreetingToUiState() = runTest {
        val viewModel = viewModel(FakeRepository())
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onGreetClicked()

        assertEquals(listOf("Hello, Test!"), viewModel.uiState.value.history.map { it.message })
        assertNull(viewModel.uiState.value.errorMessage)
        collector.cancel()
    }

    @Test
    fun failureIsExposedAsErrorMessage() = runTest {
        val viewModel = viewModel(FakeRepository(failure = IllegalStateException("boom")))
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onGreetClicked()

        assertEquals("boom", viewModel.uiState.value.errorMessage)
        assertEquals(false, viewModel.uiState.value.isLoading)
        collector.cancel()
    }
}
