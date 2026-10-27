package com.example.theekhai.data.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.theekhai.data.db.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<SqlDriver> { AndroidSqliteDriver(AppDatabase.Schema, get<Context>(), "theekhai.db") }
    // 10.0.2.2 is the host machine as seen from the Android emulator.
    single(DataQualifiers.BackendBaseUrl) { "http://10.0.2.2:8080" }
}
