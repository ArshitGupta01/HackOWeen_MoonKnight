package com.example.theekhai.data.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.theekhai.data.db.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<SqlDriver> { AndroidSqliteDriver(AppDatabase.Schema, get<Context>(), "theekhai.db") }
    // Uses the live unified HTTPS ngrok tunnel so physical Android phones and emulators work anywhere.
    single(DataQualifiers.BackendBaseUrl) {
        System.getProperty("THEEKHAI_BACKEND_URL")
            ?: System.getenv("THEEKHAI_BACKEND_URL")
            ?: "https://bless-stinger-plow.ngrok-free.dev"
    }
}
