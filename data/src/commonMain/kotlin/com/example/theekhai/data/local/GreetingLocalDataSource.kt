package com.example.theekhai.data.local

import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.data.db.GreetingEntity
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Thin wrapper over the generated SQLDelight queries. Shared by clients and the server. */
class GreetingLocalDataSource(
    private val database: AppDatabase,
    private val dispatcher: CoroutineDispatcher,
) {
    fun observeAll(): Flow<List<GreetingEntity>> =
        database.greetingQueries.selectAll().asFlow().mapToList(dispatcher)

    /** Inserts a row and returns its generated id. */
    suspend fun insert(message: String, createdAtEpochMillis: Long): Long = withContext(dispatcher) {
        database.transactionWithResult {
            database.greetingQueries.insert(message, createdAtEpochMillis)
            database.greetingQueries.lastInsertedId().executeAsOne()
        }
    }
}
