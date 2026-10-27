package com.example.theekhai.data.mapper

import com.example.theekhai.data.db.GreetingEntity
import com.example.theekhai.domain.model.Greeting

fun GreetingEntity.toDomain(): Greeting = Greeting(
    id = id,
    message = message,
    createdAtEpochMillis = created_at,
)
