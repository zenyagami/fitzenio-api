package com.zenthek.routes

import com.zenthek.model.AnalyzeImageRequest
import com.zenthek.model.ImageAnalysisResponse
import com.zenthek.model.ImageAnalyzer
import com.zenthek.model.SearchResponse
import com.zenthek.service.FoodService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json

private val sseJson = Json { ignoreUnknownKeys = true }

private suspend fun ByteWriteChannel.sendSseEvent(event: String, data: String) {
    writeFully("event: $event\ndata: $data\n\n".toByteArray(Charsets.UTF_8))
    flush()
}

fun Application.configureRouting(
    foodService: FoodService,
    imageAnalyzer: ImageAnalyzer,
) {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }

        route("/api/food") {

            get("/autocomplete") {
                val query = call.request.queryParameters["q"]?.trim()
                    ?: throw IllegalArgumentException("Missing required parameter: q")
                if (query.isBlank()) throw IllegalArgumentException("Parameter 'q' must not be blank")

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
                if (limit > 25) throw IllegalArgumentException("limit cannot exceed 25")

                val suggestions = foodService.autocomplete(query, limit)
                call.respond(HttpStatusCode.OK, mapOf("suggestions" to suggestions))
            }

            get("/search") {
                val query = call.request.queryParameters["q"]?.trim()
                    ?: throw IllegalArgumentException("Missing required parameter: q")
                if (query.isBlank()) throw IllegalArgumentException("Parameter 'q' must not be blank")

                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 25
                if (pageSize > 50) throw IllegalArgumentException("pageSize cannot exceed 50")

                val results = foodService.search(query, page, pageSize)

                call.respond(HttpStatusCode.OK, SearchResponse(results, results.size, page, pageSize))
            }

            get("/barcode/{barcode}") {
                val barcode = call.parameters["barcode"]?.trim()
                    ?: throw IllegalArgumentException("Missing barcode path parameter")
                if (barcode.isBlank() || !barcode.all { it.isDigit() }) {
                    throw IllegalArgumentException("Barcode must contain only digits")
                }

                val result = foodService.getByBarcode(barcode)

                call.respond(
                    HttpStatusCode.OK,
                    mapOf("result" to result)
                )
            }

            post("/analyze-image") {
                val body = call.receive<AnalyzeImageRequest>()
                val imageBytes = java.util.Base64.getDecoder().decode(body.image)
                val result = imageAnalyzer.analyzeImage(
                    imageBytes,
                    body.mealTitle,
                    body.additionalContext,
                    body.locale,
                    "image/jpeg"
                )
                call.respond(HttpStatusCode.OK, result)
            }

            post("/analyze-image-stream") {
                val body = call.receive<AnalyzeImageRequest>()
                val imageBytes = java.util.Base64.getDecoder().decode(body.image)
                call.response.cacheControl(CacheControl.NoCache(null))
                call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
                    sendSseEvent("status", """{"phase":"analyzing"}""")
                    try {
                        val result = imageAnalyzer.analyzeImage(
                            imageBytes,
                            body.mealTitle,
                            body.additionalContext,
                            body.locale,
                            "image/jpeg"
                        )
                        sendSseEvent("result", sseJson.encodeToString(ImageAnalysisResponse.serializer(), result))
                    } catch (e: Exception) {
                        application.log.error("SSE analyze-image-stream failed", e)
                        sendSseEvent("error", """{"message":"Analysis failed"}""")
                    }
                }
            }
        }
    }
}
