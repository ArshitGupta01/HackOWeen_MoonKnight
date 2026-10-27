package com.example.theekhai

import com.example.theekhai.contract.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("THEEKHAI Smart Cities Backend Server"))
    }

    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            module()
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("HEALTHY"))
    }

    @Test
    fun testKmpClientComplaintsListReturnsJsonArray() = testApplication {
        application {
            module()
        }
        val response = client.get("/api/v1/complaints") {
            header("X-Client-Platform", "kmp")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().trimStart().startsWith("["))
    }

    @Test
    fun testVoiceIntakeDpdpConsentEnforced() = testApplication {
        application {
            module()
        }
        val deniedPayload = """
            {
                "audioBase64": "UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA=",
                "mimeType": "audio/wav",
                "citizenId": "cit-edge-01",
                "dpdpConsentGranted": false,
                "dpdpConsentText": "I do not consent"
            }
        """.trimIndent()

        val response = client.post("/api/v1/voice/intake") {
            contentType(ContentType.Application.Json)
            setBody(deniedPayload)
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("DPDP Act 2026 Violation"))
    }

    @Test
    fun testComplaintNotFoundForKmpClient() = testApplication {
        application {
            module()
        }
        val response = client.get("/api/v1/complaints/non-existent-ticket-999") {
            header("X-Client-Platform", "kmp")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
