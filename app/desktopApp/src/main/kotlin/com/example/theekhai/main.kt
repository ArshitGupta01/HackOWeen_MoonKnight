package com.example.theekhai

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.theekhai.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "TheekHai",
        ) {
            App()
        }
    }
}
