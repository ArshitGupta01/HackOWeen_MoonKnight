package com.example.theekhai.data.jvm

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.example.theekhai.data.db.AppDatabase

/**
 * Creates the SQLite driver used by Desktop and by the Ktor server, creating or migrating the
 * schema through `PRAGMA user_version`. Pass `null` for an in-memory database (tests).
 */
fun createJvmSqliteDriver(databasePath: String?): SqlDriver {
    val url = if (databasePath == null) JdbcSqliteDriver.IN_MEMORY else JdbcSqliteDriver.IN_MEMORY + databasePath
    val driver = JdbcSqliteDriver(url)

    val currentVersion = driver.executeQuery(
        identifier = null,
        sql = "PRAGMA user_version",
        mapper = { cursor ->
            QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
        },
        parameters = 0,
    ).value ?: 0L
    val targetVersion = AppDatabase.Schema.version

    if (currentVersion == 0L) {
        AppDatabase.Schema.create(driver)
        driver.execute(null, "PRAGMA user_version = $targetVersion", 0)
    } else if (currentVersion < targetVersion) {
        AppDatabase.Schema.migrate(driver, currentVersion, targetVersion)
        driver.execute(null, "PRAGMA user_version = $targetVersion", 0)
    }
    return driver
}
