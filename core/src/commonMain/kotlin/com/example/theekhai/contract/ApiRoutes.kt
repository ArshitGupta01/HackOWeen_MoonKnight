package com.example.theekhai.contract

/** Single source of truth for backend paths, shared by the Ktor server and Ktor clients. */
object ApiRoutes {
    const val VERSION_PREFIX = "/api/v1"
    const val GREETING = "$VERSION_PREFIX/greeting"
    const val GREETING_PLATFORM_PARAM = "platform"
}
