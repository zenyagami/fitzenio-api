package com.zenthek.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.util.Base64

class OpenAiApiService(
    private val client: HttpClient,
    private val apiKey: String
) {
    suspend fun analyzeImage(imageBytes: ByteArray, textPrompt: String?, mimeType: String): String {
        val base64Image = Base64.getEncoder().encodeToString(imageBytes)
        val dataUrl = "data:$mimeType;base64,$base64Image"
        
        val prompt = if (textPrompt.isNullOrBlank()) {
            "What food is in this image? Provide nutritional estimation."
        } else {
            textPrompt
        }

        val requestBody = buildJsonObject {
            put("model", "gpt-4o-mini")
            put("max_tokens", 500)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        }
                        addJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", dataUrl)
                            }
                        }
                    }
                }
            }
        }

        val response = client.post("https://api.openai.com/v1/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        return response.bodyAsText()
    }
}
