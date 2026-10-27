package com.example.theekhai.data.di

import app.cash.sqldelight.db.SqlDriver
import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.data.local.ComplaintLocalDataSource
import com.example.theekhai.data.remote.ComplaintRemoteDataSource
import com.example.theekhai.data.remote.KtorComplaintRemoteDataSource
import com.example.theekhai.data.remote.OpenRouterApiClient
import com.example.theekhai.data.remote.SarvamApiClient
import com.example.theekhai.data.remote.TinyFishApiClient
import com.example.theekhai.data.remote.createHttpClient
import com.example.theekhai.data.repository.AuditRepositoryImpl
import com.example.theekhai.data.repository.ComplaintRepositoryImpl
import com.example.theekhai.data.repository.VerificationRepositoryImpl
import com.example.theekhai.data.supabase.SupabaseConfig
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository
import com.example.theekhai.domain.repository.VerificationRepository
import com.example.theekhai.domain.usecase.TriageAiClient
import com.example.theekhai.domain.usecase.VoiceSpeechToTextClient
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

object DataQualifiers {
    val BackendBaseUrl = named("backendBaseUrl")
}

expect val platformDataModule: Module

val localDataModule: Module = module {
    single { AppDatabase(get<SqlDriver>()) }
    single { ComplaintLocalDataSource(get(), Dispatchers.Default) }
}

val remoteDataModule: Module = module {
    single { createHttpClient() }
    single<ComplaintRemoteDataSource> {
        KtorComplaintRemoteDataSource(client = get(), baseUrl = get<String>(DataQualifiers.BackendBaseUrl))
    }
    single { SupabaseConfig() }
    single<TriageAiClient> { OpenRouterApiClient(client = get(), backendBaseUrl = get<String>(DataQualifiers.BackendBaseUrl)) }
    single<VoiceSpeechToTextClient> { SarvamApiClient() }
    single { TinyFishApiClient() }
}

val dataModule: Module = module {
    includes(platformDataModule, localDataModule, remoteDataModule)
    single<ComplaintRepository> { ComplaintRepositoryImpl(local = get(), remote = get()) }
    single<VerificationRepository> { VerificationRepositoryImpl(remote = get()) }
    single<AuditRepository> { AuditRepositoryImpl(local = get(), remote = get()) }
}
