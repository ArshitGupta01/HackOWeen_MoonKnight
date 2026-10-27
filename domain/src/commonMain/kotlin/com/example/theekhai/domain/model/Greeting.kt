package com.example.theekhai.domain.model

/** Domain entity. Knows nothing about SQL rows or JSON payloads. */
data class Greeting(
    val id: Long,
    val message: String,
    val createdAtEpochMillis: Long,
)
