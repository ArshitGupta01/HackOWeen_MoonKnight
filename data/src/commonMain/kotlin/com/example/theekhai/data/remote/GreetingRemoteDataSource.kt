package com.example.theekhai.data.remote

import com.example.theekhai.contract.ApiRoutes
import com.example.theekhai.contract.GreetingDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

interface GreetingRemoteDataSource {
    suspend fun fetchGreeting(platformName: String): GreetingDto
}

/** Talks to the Ktor `:server` using the shared `:core` contract (routes + DTOs). */
class KtorGreetingRemoteDataSource(
    private val client: HttpClient,
    private val baseUrl: String,
) : GreetingRemoteDataSource {
    override suspend fun fetchGreeting(platformName: String): GreetingDto =
        client.get(baseUrl.trimEnd('/') + ApiRoutes.GREETING) {
            parameter(ApiRoutes.GREETING_PLATFORM_PARAM, platformName)
        }.body()
}
