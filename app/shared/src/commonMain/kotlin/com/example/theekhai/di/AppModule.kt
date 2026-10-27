package com.example.theekhai.di

import com.example.theekhai.Platform
import com.example.theekhai.data.di.dataModule
import com.example.theekhai.domain.usecase.GreetUseCase
import com.example.theekhai.domain.usecase.ObserveGreetingHistoryUseCase
import com.example.theekhai.getPlatform
import com.example.theekhai.presentation.greeting.GreetingViewModel
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Domain use cases. The `:domain` module is Koin-free, so they are registered here. */
val domainModule: Module = module {
    factory { GreetUseCase(get()) }
    factory { ObserveGreetingHistoryUseCase(get()) }
}

val presentationModule: Module = module {
    single<Platform> { getPlatform() }
    viewModelOf(::GreetingViewModel)
}

fun appModules(): List<Module> = listOf(dataModule, domainModule, presentationModule)

/** Single entry point used by every client app (Android, iOS, Desktop). */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        modules(appModules())
    }
