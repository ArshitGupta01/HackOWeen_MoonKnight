package com.example.theekhai.data.repository

import com.example.theekhai.contract.GreetingDto
import com.example.theekhai.data.local.GreetingLocalDataSource
import com.example.theekhai.data.mapper.toDomain
import com.example.theekhai.data.remote.GreetingRemoteDataSource
import com.example.theekhai.domain.model.Greeting
import com.example.theekhai.domain.repository.GreetingRepository
import com.example.theekhai.sayHello
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/**
 * Offline-first: SQLite is the single source of truth for the UI.
 * A greeting is requested from the backend; if the backend is unreachable we fall back to
 * generating it locally, then persist the result either way.
 */
class GreetingRepositoryImpl(
    private val local: GreetingLocalDataSource,
    private val remote: GreetingRemoteDataSource,
    private val clock: Clock = Clock.System,
) : GreetingRepository {

    override fun observeHistory(): Flow<List<Greeting>> =
        local.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun greet(platformName: String): Greeting {
        val dto = try {
            remote.fetchGreeting(platformName)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            GreetingDto(
                message = sayHello(platformName),
                createdAtEpochMillis = clock.now().toEpochMilliseconds(),
            )
        }
        val id = local.insert(dto.message, dto.createdAtEpochMillis)
        return Greeting(id = id, message = dto.message, createdAtEpochMillis = dto.createdAtEpochMillis)
    }
}
