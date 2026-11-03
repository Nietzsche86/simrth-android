package id.mts.rth.data.remote.api

import id.mts.rth.data.remote.dto.HealthResponse
import id.mts.rth.data.remote.dto.SyncPullResponse
import id.mts.rth.data.remote.dto.SyncPushRequest
import id.mts.rth.data.remote.dto.SyncPushResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

/**
 * Layanan API Sinkronisasi Manual (Health Check, Push Outbox, Pull Delta, Download Foto)
 * ke Server Lokal Wi-Fi MTs Roudhotut Thowalib.
 */
class SyncApiService(private val client: HttpClient) {

    /**
     * Memeriksa kesehatan server lokal dan mengambil waktu server (timeout 3000 ms).
     */
    suspend fun checkHealth(baseUrl: String): HealthResponse {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        return client.get("$cleanBaseUrl/api/v1/health") {
            timeout {
                requestTimeoutMillis = 3000
                connectTimeoutMillis = 3000
            }
        }.body()
    }

    /**
     * Mengirim batch mutasi outbox lokal ke server lokal (Push).
     */
    suspend fun pushMutations(
        baseUrl: String,
        token: String,
        request: SyncPushRequest
    ): SyncPushResponse {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        return client.post("$cleanBaseUrl/api/v1/sync/push") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    /**
     * Mengambil delta pembaruan data akademik dan nilai dari server lokal (Pull).
     */
    suspend fun pullDelta(
        baseUrl: String,
        token: String,
        sinceTimestamp: Long
    ): SyncPullResponse {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        return client.get("$cleanBaseUrl/api/v1/sync/pull") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("since", sinceTimestamp)
        }.body()
    }

    /**
     * Mengunduh foto siswa berformat WebP dari server lokal madrasah.
     */
    suspend fun downloadStudentPhoto(
        baseUrl: String,
        token: String,
        fileName: String
    ): ByteArray? {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val response = client.get("$cleanBaseUrl/api/v1/media/students/$fileName") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return if (response.status == HttpStatusCode.OK) {
            response.bodyAsBytes()
        } else {
            null
        }
    }
}
