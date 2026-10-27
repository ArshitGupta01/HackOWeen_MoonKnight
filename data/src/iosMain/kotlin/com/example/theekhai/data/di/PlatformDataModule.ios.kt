package com.example.theekhai.data.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.theekhai.data.db.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<SqlDriver> { NativeSqliteDriver(AppDatabase.Schema, "theekhai.db") }
    single(DataQualifiers.BackendBaseUrl) { "http://localhost:8080" }
}
