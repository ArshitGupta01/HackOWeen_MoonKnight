package com.example.theekhai.contract

import kotlinx.serialization.Serializable

/**
 * Wire format for a greeting.
 *
 * Lives in `:core` so the Ktor server and every client compile against the *same*
 * type: the HTTP contract can never silently drift between backend and apps.
 */
@Serializable
data class GreetingDto(
    val message: String,
    val createdAtEpochMillis: Long,
)
