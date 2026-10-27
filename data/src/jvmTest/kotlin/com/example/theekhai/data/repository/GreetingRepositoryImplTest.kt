package com.example.theekhai.data.repository

import com.example.theekhai.contract.GreetingDto
import com.example.theekhai.data.db.AppDatabase
import com.example.theekhai.data.jvm.createJvmSqliteDriver
import com.example.theekhai.data.local.GreetingLocalDataSource
import com.example.theekhai.data.remote.GreetingRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingRepositoryImplTest {

    private fun repository(remote: GreetingRemoteDataSource): GreetingRepositoryImpl {
        val database = AppDatabase(createJvmSqliteDriver(null))
        return GreetingRepositoryImpl(GreetingLocalDataSource(database, Dispatchers.Default), remote)
    }

    @Test
    fun persistsRemoteGreeting() = runTest {
        val repository = repository(object : GreetingRemoteDataSource {
            override suspend fun fetchGreeting(platformName: String) = GreetingDto("remote $platformName", 42L)
        })

        val greeting = repository.greet("Desktop")

        assertEquals("remote Desktop", greeting.message)
        assertEquals(listOf(greeting), repository.observeHistory().first())
    }

    @Test
    fun fallsBackToLocalGreetingWhenBackendIsDown() = runTest {
        val repository = repository(object : GreetingRemoteDataSource {
            override suspend fun fetchGreeting(platformName: String): GreetingDto = error("offline")
        })

        val greeting = repository.greet("Desktop")

        assertEquals("Hello, Desktop!", greeting.message)
        assertEquals(1, repository.observeHistory().first().size)
    }
}
