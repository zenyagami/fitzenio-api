package com.zenthek.fitzenio.rest

import com.zenthek.model.ImageAnalyzer
import com.zenthek.routes.configureRouting
import com.zenthek.service.FoodService
import com.zenthek.upstream.openai.OpenAiApiService
import com.zenthek.upstream.fatsecret.FatSecretClient
import com.zenthek.upstream.gemini.GeminiApiService
import com.zenthek.upstream.fatsecret.FatSecretTokenManager
import com.zenthek.upstream.openfoodfacts.OpenFoodFactsClient
import com.zenthek.upstream.usda.UsdaClient
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import com.zenthek.config.ConfigLoader
import io.ktor.http.HttpStatusCode
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    // Load environment configuration
    val config = ConfigLoader.loadConfig()

    log.info("Starting Fitzenio API in ${config.environment} mode")

    val httpClient = buildHttpClient()

    val offClient = OpenFoodFactsClient(httpClient)
    val fsTokenManager = FatSecretTokenManager(httpClient, config.apiKeys)
    val fsClient = FatSecretClient(httpClient, fsTokenManager)
    val usdaClient = UsdaClient(httpClient, config.apiKeys.usdaApiKey)
    val openAiClient = OpenAiApiService(httpClient, config.apiKeys.openAiApiKey)
    val imageAnalyzer: ImageAnalyzer = if (config.useGemini) {
        log.info("Image analysis backend: Gemini Flash")
        GeminiApiService(httpClient, config.geminiApiKey)
    } else {
        log.info("Image analysis backend: GPT-5-mini")
        openAiClient
    }

    val foodService = FoodService(offClient, fsClient, usdaClient)

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to "Internal server error",
                    "message" to (cause.message ?: "Unknown error")
                )
            )
        }
    }
    configureRouting(foodService, imageAnalyzer, openAiClient)
}

fun buildHttpClient(): HttpClient = HttpClient(CIO) {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 10_000
        connectTimeoutMillis = 5_000
    }
}
