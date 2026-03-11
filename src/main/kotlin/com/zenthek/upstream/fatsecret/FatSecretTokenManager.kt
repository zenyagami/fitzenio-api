package com.zenthek.upstream.fatsecret

import com.zenthek.config.ApiKeys
import com.zenthek.upstream.fatsecret.dto.FatSecretTokenResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FatSecretTokenManager(
    private val httpClient: HttpClient,
    private val apiKeys: ApiKeys
) {
    private val tokenUrl = "https://oauth.fatsecret.com/connect/token"
    private var currentToken: String? = null
    private var tokenExpiryMs: Long = 0
    private val mutex = Mutex()

    suspend fun getToken(): String = mutex.withLock {
        // Refresh token if it's missing or expires in less than 60 seconds
        val nowMs = System.currentTimeMillis()
        if (currentToken == null || tokenExpiryMs - nowMs < 60_000) {
            refreshToken()
        }
        return currentToken ?: error("Failed to acquire FatSecret token")
    }

    private suspend fun refreshToken() {
        val response = httpClient.post(tokenUrl) {
            header(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
            setBody(FormDataContent(Parameters.build {
                append("grant_type", "client_credentials")
                append("scope", "basic")
                append("client_id", apiKeys.fatSecretClientId)
                append("client_secret", apiKeys.fatSecretClientSecret)
            }))
        }

        if (response.status.isSuccess()) {
            val tokenResponse = response.body<FatSecretTokenResponse>()
            currentToken = tokenResponse.accessToken
            // expiresIn is in seconds. Convert to ms and store time of expiry
            tokenExpiryMs = System.currentTimeMillis() + (tokenResponse.expiresIn * 1000)
        } else {
            error("FatSecret OAuth failure: ${response.status} ${response.body<String>()}")
        }
    }
}
