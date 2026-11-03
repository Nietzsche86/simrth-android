package id.mts.rth.data.remote.api

import id.mts.rth.data.remote.dto.LoginRequest
import id.mts.rth.data.remote.dto.LoginResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * Layanan API Autentikasi dan Initial Seed Snapshot ke Server Lokal Madrasah.
 */
class AuthApiService(private val client: HttpClient) {

    suspend fun login(baseUrl: String, request: LoginRequest): LoginResponse {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        return client.post("$cleanBaseUrl/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}
