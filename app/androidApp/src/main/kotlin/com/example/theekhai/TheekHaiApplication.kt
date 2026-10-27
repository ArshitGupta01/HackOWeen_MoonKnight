package com.example.theekhai

import android.app.Application
import com.example.theekhai.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class TheekHaiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@TheekHaiApplication)
        }
    }
}
