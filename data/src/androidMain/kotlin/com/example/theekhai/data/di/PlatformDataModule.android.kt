package com.example.theekhai.data.di

import android.content.Context
import android.content.pm.PackageManager
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.theekhai.data.db.AppDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<SqlDriver> { AndroidSqliteDriver(AppDatabase.Schema, get<Context>(), "theekhai.db") }
    single(DataQualifiers.BackendBaseUrl) {
        val context = get<Context>()
        @Suppress("DEPRECATION")
        val metadata = context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA,
        ).metaData
        metadata?.getString("com.example.theekhai.BACKEND_BASE_URL")
            ?.trim()
            ?.trimEnd('/')
            ?.takeIf(String::isNotBlank)
            ?: error("Backend URL is missing. Set THEEKHAI_BACKEND_URL when building the Android app.")
    }
}
