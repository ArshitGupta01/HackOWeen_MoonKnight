package com.example.theekhai.data.di

import app.cash.sqldelight.db.SqlDriver
import com.example.theekhai.data.jvm.createJvmSqliteDriver
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File

actual val platformDataModule: Module = module {
    single<SqlDriver> { createJvmSqliteDriver(desktopDatabasePath()) }
    single(DataQualifiers.BackendBaseUrl) {
        System.getProperty("THEEKHAI_BACKEND_URL")
            ?: System.getenv("THEEKHAI_BACKEND_URL")
            ?: runCatching {
                File("web/ngrok-url.txt").takeIf { it.exists() }?.readText()?.trim()
            }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: "https://bless-stinger-plow.ngrok-free.dev"
    }
}

/** `~/.theekhai/theekhai.db` - created on first launch. */
private fun desktopDatabasePath(): String {
    val directory = File(System.getProperty("user.home"), ".theekhai").apply { mkdirs() }
    return File(directory, "theekhai.db").absolutePath
}
