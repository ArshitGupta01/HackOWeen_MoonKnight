package com.example.theekhai.di

import app.cash.sqldelight.db.SqlDriver
import com.example.theekhai.data.di.localDataModule
import com.example.theekhai.data.jvm.createJvmSqliteDriver
import com.example.theekhai.domain.repository.GreetingRepository
import com.example.theekhai.domain.usecase.GreetUseCase
import com.example.theekhai.repository.ServerGreetingRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/** Backend composition root: same `:domain` use cases and `:data` SQLite schema as the apps. */
fun serverModule(databasePath: String?): Module = module {
    includes(localDataModule)
    single<SqlDriver> { createJvmSqliteDriver(databasePath) }
    single<GreetingRepository> { ServerGreetingRepository(local = get()) }
    factory { GreetUseCase(get()) }
}
