package com.example.theekhai.di
import app.cash.sqldelight.db.SqlDriver
import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.data.jvm.createJvmSqliteDriver
import com.example.theekhai.data.workflow.SqlWorkflowStore
import com.example.theekhai.domain.workflow.Workflow
import com.example.theekhai.domain.workflow.WorkflowStore
import org.koin.dsl.module
import java.util.UUID

fun serverModule(databasePath: String?) = module {
    single<SqlDriver> { createJvmSqliteDriver(databasePath) }
    single { AppDatabase(get()) }
    single<WorkflowStore> { SqlWorkflowStore(get()) }
    single { Workflow(get(), System::currentTimeMillis) { UUID.randomUUID().toString() } }
}
