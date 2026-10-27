package com.example.theekhai.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** The engine (OkHttp / Darwin) is picked per platform from the classpath. */
fun createHttpClient(token: (() -> String?)? = null): HttpClient = HttpClient {
    defaultRequest {
        header("ngrok-skip-browser-warning", "true")
        token?.invoke()?.let { header(io.ktor.http.HttpHeaders.Authorization, "Bearer $it") }
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 10_000
    }
}
