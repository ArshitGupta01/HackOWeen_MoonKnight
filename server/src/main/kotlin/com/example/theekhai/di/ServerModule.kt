package com.example.theekhai.di

import app.cash.sqldelight.db.SqlDriver
import com.example.theekhai.ai.BackendAiService
import com.example.theekhai.config.ServerConfig
import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.data.di.localDataModule
import com.example.theekhai.data.jvm.createJvmSqliteDriver
import com.example.theekhai.data.remote.createHttpClient
import com.example.theekhai.data.repository.AuditRepositoryImpl
import com.example.theekhai.data.repository.ComplaintRepositoryImpl
import com.example.theekhai.data.repository.VerificationRepositoryImpl
import com.example.theekhai.data.workflow.SqlWorkflowStore
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository
import com.example.theekhai.domain.repository.VerificationRepository
import com.example.theekhai.domain.workflow.Workflow
import com.example.theekhai.domain.workflow.WorkflowStore
import org.koin.dsl.module
import java.util.UUID

fun serverModule(databasePath: String?) = module {
    includes(localDataModule)
    single<SqlDriver> { createJvmSqliteDriver(databasePath) }
    single { ServerConfig() }
    single { createHttpClient() }
    single { BackendAiService(client = get(), config = get()) }

    single<com.example.theekhai.data.remote.ComplaintRemoteDataSource> {
        com.example.theekhai.data.remote.KtorComplaintRemoteDataSource(client = get(), baseUrl = "http://localhost:8080")
    }
    single<ComplaintRepository> { ComplaintRepositoryImpl(local = get(), remote = get()) }
    single<VerificationRepository> { VerificationRepositoryImpl(remote = get()) }
    single<AuditRepository> { AuditRepositoryImpl(local = get(), remote = get()) }

    single<WorkflowStore> { SqlWorkflowStore(get()) }
    single { Workflow(get(), System::currentTimeMillis) { UUID.randomUUID().toString() } }
}
