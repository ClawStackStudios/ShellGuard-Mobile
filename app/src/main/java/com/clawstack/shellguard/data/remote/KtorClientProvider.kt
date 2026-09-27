package com.clawstack.shellguard.data.remote

import android.util.Log
import com.clawstack.shellguard.data.remote.models.ShellResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object NetworkSecurityHelper {

    val relaxedTrustManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    val relaxedSslContext: SSLContext by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(relaxedTrustManager), SecureRandom())
        }
    }

    fun configureOkHttpClient(builder: OkHttpClient.Builder) {
        builder.connectionSpecs(
            listOf(
                ConnectionSpec.MODERN_TLS,
                ConnectionSpec.COMPATIBLE_TLS,
                ConnectionSpec.CLEARTEXT
            )
        )
        builder.sslSocketFactory(relaxedSslContext.socketFactory, relaxedTrustManager)
        builder.hostnameVerifier { _, _ -> true }
        builder.connectTimeout(15, TimeUnit.SECONDS)
        builder.readTimeout(20, TimeUnit.SECONDS)
        builder.writeTimeout(20, TimeUnit.SECONDS)
        builder.retryOnConnectionFailure(true)
    }
}

suspend fun handleNetworkDiagnostics(response: HttpResponse) {
    if (response.status.value == 400) {
        val responseBody = response.bodyAsText()
        try {
            val errorDetails = KtorClientProvider.jsonConfig.decodeFromString<ShellResponse<Unit>>(responseBody)
            Log.e("ShellGuardClient", "🚨 [VALIDATION ERROR 400]: ${errorDetails.error}")
            errorDetails.details?.forEach { issue ->
                Log.e("ShellGuardClient", "  → Field [${issue.path}]: ${issue.message}")
            }
        } catch (e: Exception) {
            Log.e("ShellGuardClient", "🚨 [HTTP 400 RAW]: $responseBody", e)
        }
    }
}

object KtorClientProvider {

    val jsonConfig = Json {
        prettyPrint = false
        isLenient = true
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun createClient(
        baseUrl: String? = null,
        onUnauthorized: (() -> Unit)? = null
    ): HttpClient {
        return HttpClient(OkHttp) {
            engine {
                config {
                    NetworkSecurityHelper.configureOkHttpClient(this)
                }
            }

            install(ContentNegotiation) {
                json(jsonConfig)
            }

            if (baseUrl != null) {
                defaultRequest {
                    url {
                        val base = baseUrl.removeSuffix("/")
                        takeFrom("$base/")
                    }
                    header("X-Client-Version", "1.0.0")
                    header("Accept-Version", "1.0.0")
                    header("Accept", "application/json")
                }
            }

            HttpResponseValidator {
                validateResponse { response ->
                    if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                        onUnauthorized?.invoke()
                    }
                }
            }
        }
    }
}
