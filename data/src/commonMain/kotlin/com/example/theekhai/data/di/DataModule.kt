package com.example.theekhai.data.di

import app.cash.sqldelight.db.SqlDriver
import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.data.local.GreetingLocalDataSource
import com.example.theekhai.data.remote.GreetingRemoteDataSource
import com.example.theekhai.data.remote.KtorGreetingRemoteDataSource
import com.example.theekhai.data.remote.createHttpClient
import com.example.theekhai.data.repository.GreetingRepositoryImpl
import com.example.theekhai.domain.repository.GreetingRepository
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

object DataQualifiers {
    /** String binding: base URL of the Ktor backend (differs per platform). */
    val BackendBaseUrl = named("backendBaseUrl")
}

/**
 * Per-platform bindings: the `SqlDriver` (Android / iOS / JVM) and the `BackendBaseUrl` string.
 * Implemented in androidMain, iosMain and jvmMain.
 */
expect val platformDataModule: Module

/** SQLite + local data source. Reused by the Ktor server (which supplies its own `SqlDriver`). */
val localDataModule: Module = module {
    single { AppDatabase(get<SqlDriver>()) }
    single { GreetingLocalDataSource(get(), Dispatchers.Default) }
}

val remoteDataModule: Module = module {
    single { createHttpClient() }
    single<GreetingRemoteDataSource> {
        KtorGreetingRemoteDataSource(client = get(), baseUrl = get<String>(DataQualifiers.BackendBaseUrl))
    }
}

/** Everything a client app (Android / iOS / Desktop) needs from the data layer. */
val dataModule: Module = module {
    includes(platformDataModule, localDataModule, remoteDataModule)
    single<GreetingRepository> { GreetingRepositoryImpl(local = get(), remote = get()) }
}
