package com.example.theekhai

import com.example.theekhai.contract.GreetingDto
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
        assertEquals("Hello, Ktor!", response.bodyAsText())
    }

    @Test
    fun greetingEndpointReturnsSharedContract() = testApplication {
        application {
            module()
        }
        val response = client.get("/api/v1/greeting?platform=Android")
        assertEquals(HttpStatusCode.OK, response.status)
        val dto = Json.decodeFromString<GreetingDto>(response.bodyAsText())
        assertEquals("Hello, Android!", dto.message)
    }

    @Test
    fun greetingEndpointRejectsMissingPlatform() = testApplication {
        application {
            module()
        }
        assertEquals(HttpStatusCode.BadRequest, client.get("/api/v1/greeting").status)
    }
}
