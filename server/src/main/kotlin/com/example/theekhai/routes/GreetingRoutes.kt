package com.example.theekhai.routes

import com.example.theekhai.contract.ApiRoutes
import com.example.theekhai.contract.GreetingDto
import com.example.theekhai.domain.usecase.GreetUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** Presentation layer of the backend: HTTP in/out only, business rules live in the use case. */
fun Route.greetingRoutes(greet: GreetUseCase) {
    get(ApiRoutes.GREETING) {
        val platform = call.request.queryParameters[ApiRoutes.GREETING_PLATFORM_PARAM]
        if (platform.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, "Missing '${ApiRoutes.GREETING_PLATFORM_PARAM}' query parameter")
            return@get
        }
        val greeting = greet(platform)
        call.respond(GreetingDto(greeting.message, greeting.createdAtEpochMillis))
    }
}
