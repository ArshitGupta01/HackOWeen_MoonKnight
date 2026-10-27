package com.example.theekhai.di

import com.example.theekhai.Platform
import com.example.theekhai.audio.AudioRecorder
import com.example.theekhai.data.di.dataModule
import com.example.theekhai.domain.usecase.CheckEvidenceUseCase
import com.example.theekhai.domain.usecase.EscalateComplaintUseCase
import com.example.theekhai.domain.usecase.ObserveComplaintsUseCase
import com.example.theekhai.domain.usecase.SyncOfflineComplaintsUseCase
import com.example.theekhai.domain.usecase.TriageComplaintUseCase
import com.example.theekhai.domain.usecase.VerifyResolutionUseCase
import com.example.theekhai.domain.usecase.VisionEvidenceClient
import com.example.theekhai.domain.usecase.VoiceIntakeUseCase
import com.example.theekhai.domain.usecase.SignInUseCase
import com.example.theekhai.domain.usecase.RegisterCitizenUseCase
import com.example.theekhai.domain.usecase.SubmitCivicReportUseCase
import com.example.theekhai.domain.usecase.CivicBackendUseCases
import com.example.theekhai.presentation.nativeconnect.NativeConnectViewModel
import com.example.theekhai.getPlatform
import com.example.theekhai.presentation.admin.CommandCenterViewModel
import com.example.theekhai.presentation.fieldworker.FieldWorkerViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

val domainModule: Module = module {
    single<VisionEvidenceClient> {
        object : VisionEvidenceClient {
            override suspend fun analyzeImageEvidence(
                photoBase64: String,
                expectedCategory: String
            ): Pair<List<String>, Double> {
                return listOf("Fresh Asphalt Pavement", "Clean Road Surface", "No Obstruction") to 0.94
            }
        }
    }
    factory { ObserveComplaintsUseCase(get()) }
    factory { TriageComplaintUseCase(get(), get(), get()) }
    factory { VerifyResolutionUseCase(get(), get(), get()) }
    factory { EscalateComplaintUseCase(get(), get()) }
    factory { VoiceIntakeUseCase(get(), get(), get()) }
    factory { CheckEvidenceUseCase(get(), get(), get()) }
    factory { SyncOfflineComplaintsUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { RegisterCitizenUseCase(get()) }
    factory { SubmitCivicReportUseCase(get()) }
    factory { CivicBackendUseCases(get()) }
}

val presentationModule: Module = module {
    single<Platform> { getPlatform() }
    single { AudioRecorder() }
    viewModelOf(::CommandCenterViewModel)
    viewModelOf(::FieldWorkerViewModel)
    viewModelOf(::NativeConnectViewModel)
}

fun appModules(): List<Module> = listOf(dataModule, domainModule, presentationModule)

fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(appModules())
    }
