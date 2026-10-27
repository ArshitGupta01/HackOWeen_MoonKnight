package com.example.theekhai.data.remote

import com.example.theekhai.contract.CaseDto
import com.example.theekhai.contract.ApiErrorDto
import com.example.theekhai.contract.IdentityDto
import com.example.theekhai.contract.LoginDto
import com.example.theekhai.contract.RegisterDto
import com.example.theekhai.contract.ReportDto
import com.example.theekhai.data.session.NativeSessionToken
import com.example.theekhai.domain.model.SignedInUser
import com.example.theekhai.domain.model.SubmittedReport
import com.example.theekhai.domain.repository.NativeCivicRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlin.uuid.Uuid

class NativeCivicRemoteRepository(
    private val client: HttpClient,
    private val baseUrl: String,
    private val session: NativeSessionToken
) : NativeCivicRepository {
    private fun url(path: String) = "${baseUrl.trimEnd('/')}/api/v1$path"

    override suspend fun signIn(email: String, password: String): SignedInUser {
        val httpResponse = client.post(url("/auth/login")) {
            contentType(ContentType.Application.Json); setBody(LoginDto(email, password))
        }
        requireSuccess(httpResponse)
        val response: Map<String, String> = httpResponse.body()
        session.value = response["accessToken"] ?: error("The server did not return a sign-in token.")
        return currentUser()
    }

    override suspend fun register(email: String, password: String, fullName: String, ward: String): SignedInUser {
        val httpResponse = client.post(url("/auth/register")) {
            contentType(ContentType.Application.Json)
            setBody(RegisterDto(email, password, fullName, ward = ward))
        }
        requireSuccess(httpResponse)
        val response: Map<String, String> = httpResponse.body()
        session.value = response["accessToken"] ?: error("The server did not return a sign-in token.")
        return currentUser()
    }

    private suspend fun currentUser(): SignedInUser {
        val response = client.get(url("/auth/me"))
        requireSuccess(response)
        val identity: IdentityDto = response.body()
        return SignedInUser(identity.id, identity.email, identity.fullName, identity.role)
    }

    override suspend fun submitReport(
        title: String, description: String, category: String, address: String, latitude: Double?, longitude: Double?, consent: Boolean
    ): SubmittedReport {
        val response = client.post(url("/complaints")) {
            contentType(ContentType.Application.Json)
            header("Idempotency-Key", Uuid.random().toString())
            setBody(ReportDto(title, description, category, address, latitude, longitude, consent))
        }
        requireSuccess(response)
        val created: CaseDto = response.body()
        return SubmittedReport(created.id, created.title, created.status)
    }

    private suspend fun requireSuccess(response: HttpResponse) {
        if (response.status.isSuccess()) return
        val message = runCatching { response.body<ApiErrorDto>().message }.getOrNull()
        throw IllegalStateException(message ?: "Request failed (${response.status.value}).")
    }
}
