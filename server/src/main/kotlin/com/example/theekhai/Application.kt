package com.example.theekhai

import com.example.theekhai.di.serverModule
import com.example.theekhai.domain.usecase.GreetUseCase
import com.example.theekhai.routes.greetingRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    val databasePath = System.getenv("THEEKHAI_DB_PATH") ?: "theekhai-server.db"
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module(databasePath)
    }.start(wait = true)
}

/**
 * @param databasePath SQLite file for the backend; `null` keeps the database in memory (tests).
 */
fun Application.module(databasePath: String? = null) {
    install(Koin) {
        slf4jLogger()
        modules(serverModule(databasePath))
    }
    install(ContentNegotiation) {
        json()
    }

    val greetUseCase: GreetUseCase by inject()

    routing {
        get("/") {
            call.respondText(sayHello("Ktor"))
        }
        greetingRoutes(greetUseCase)
    }
}
