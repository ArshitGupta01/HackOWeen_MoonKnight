package com.example.theekhai

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform