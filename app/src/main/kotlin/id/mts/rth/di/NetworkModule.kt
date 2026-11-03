package id.mts.rth.di

import id.mts.rth.data.remote.api.AuthApiService
import id.mts.rth.data.remote.api.SyncApiService
import id.mts.rth.data.remote.interceptor.LanSecurityInterceptor
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule = module {
    // Ktor HttpClient dengan OkHttp Engine dan Interceptor Keamanan LAN
    single {
        HttpClient(OkHttp) {
            engine {
                addInterceptor(LanSecurityInterceptor())
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000
                connectTimeoutMillis = 5_000
                socketTimeoutMillis = 15_000
            }
            install(Logging) {
                level = LogLevel.INFO
            }
        }
    }

    // Remote API Services
    single { AuthApiService(get()) }
    single { SyncApiService(get()) }
}
