package com.example.theekhai

import com.example.theekhai.ai.BackendAiService
import com.example.theekhai.contract.ApiRoutes
import com.example.theekhai.di.serverModule
import com.example.theekhai.domain.repository.AuditRepository
import com.example.theekhai.domain.repository.ComplaintRepository
import com.example.theekhai.routes.auditRoutes
import com.example.theekhai.routes.complaintRoutes
import com.example.theekhai.routes.evidenceRoutes
import com.example.theekhai.routes.voiceIntakeRoutes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    val databasePath = System.getenv("THEEKHAI_DB_PATH") ?: "theekhai-server.db"
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module(databasePath)
    }.start(wait = true)
}

fun Application.module(databasePath: String? = null) {
    install(Koin) {
        slf4jLogger()
        modules(serverModule(databasePath))
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        })
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Delete)
    }

    val complaintRepository: ComplaintRepository by inject()
    val auditRepository: AuditRepository by inject()
    val aiService: BackendAiService by inject()

    routing {
        get("/") {
            call.respondText("THEEKHAI Smart Cities Backend Server • Active")
        }

        get(ApiRoutes.HEALTH) {
            call.respond(
                mapOf(
                    "status" to "HEALTHY",
                    "timestamp" to System.currentTimeMillis(),
                    "version" to "1.0.0-hackathon",
                    "dpdpStandard" to "DPDP Act Oct 2026 Ready",
                    "aiProviders" to mapOf(
                        "gemini" to "gemini-3.8-flash",
                        "whisper" to "groq-whisper-large-v3",
                        "vision" to "groq-vision",
                        "autonomousScraper" to "TinyFish"
                    )
                )
            )
        }

        complaintRoutes(complaintRepository, auditRepository, aiService)
        voiceIntakeRoutes(complaintRepository, auditRepository, aiService)
        evidenceRoutes(auditRepository, aiService)
        auditRoutes(auditRepository, complaintRepository)
    }
}
