package com.example.theekhai.repository

import com.example.theekhai.data.local.GreetingLocalDataSource
import com.example.theekhai.data.mapper.toDomain
import com.example.theekhai.domain.model.Greeting
import com.example.theekhai.domain.repository.GreetingRepository
import com.example.theekhai.sayHello
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

/** The backend is the authority that creates greetings; it audits each one in SQLite. */
class ServerGreetingRepository(
    private val local: GreetingLocalDataSource,
    private val clock: Clock = Clock.System,
) : GreetingRepository {

    override fun observeHistory(): Flow<List<Greeting>> =
        local.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun greet(platformName: String): Greeting {
        val message = sayHello(platformName)
        val createdAt = clock.now().toEpochMilliseconds()
        val id = local.insert(message, createdAt)
        return Greeting(id = id, message = message, createdAtEpochMillis = createdAt)
    }
}
