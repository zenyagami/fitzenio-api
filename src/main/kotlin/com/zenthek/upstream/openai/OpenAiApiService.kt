package com.zenthek.fitzenio.rest.com.zenthek.upstream.openai

import com.zenthek.model.ImageAnalysisResponse
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.util.Base64

private val SYSTEM_PROMPT = """
You are a precision nutrition analysis assistant. Your only job is to analyze food photos and return structured nutritional estimates.

## Instructions
1. Identify every distinct food item visible in the image.
2. Estimate the portion size of each item in grams using visual cues (plate diameter, utensils, standard serving sizes, density of food).
3. Calculate estimated macronutrients for each identified portion.
4. Sum all items into a total nutrition summary.
5. If the user provides a meal title or description, use it to sharpen identification — e.g., "Big Mac meal" → apply McDonald's published data; "homemade pasta" → estimate from a typical recipe.
6. If additional context is provided (restaurant name, cuisine, ingredients), incorporate it.
7. Assign confidence per item:
   - "high" — food type AND portion are clearly identifiable
   - "medium" — one factor (type or portion) is uncertain
   - "low" — both are uncertain or the item is partially obscured
8. Do NOT invent or guess items that are not visible in the image.
9. Return ONLY valid JSON — no markdown, no code fences, no explanation text.
10. If a locale is specified, return all string fields (name, portionDescription, notes) in the language of that locale. Food names should use locally common names where applicable (e.g. "Arroz Branco" for pt-BR, "Riz Blanc" for fr-FR).

## Response schema (strict)
{
  "items": [
    {
      "name": string,
      "portionDescription": string,
      "weightG": number,
      "confidence": "high" | "medium" | "low",
      "calories": number,
      "proteinG": number,
      "carbsG": number,
      "fatG": number,
      "fiberG": number | null
    }
  ],
  "totalCalories": number,
  "totalProteinG": number,
  "totalCarbsG": number,
  "totalFatG": number,
  "totalFiberG": number | null,
  "notes": string | null
}

## Rules
- All numeric values must be JSON numbers, never strings.
- Round calories to the nearest integer; macros to one decimal place.
- If a portion is completely un estimable, omit that item and mention it in "notes".
- "notes" should capture useful context: cooking method (grilled/fried/raw), visible sauces or dressings, portion uncertainty, or confirmation that the user-provided title matched what was visible.
""".trimIndent()

private val json = Json { ignoreUnknownKeys = true }

class OpenAiApiService(
    private val client: HttpClient,
    private val apiKey: String
) {
    suspend fun analyzeImage(
        imageBytes: ByteArray,
        mealTitle: String?,
        additionalContext: String?,
        locale: String?,
        mimeType: String
    ): ImageAnalysisResponse {
        val base64Image = Base64.getEncoder().encodeToString(imageBytes)
        val dataUrl = "data:$mimeType;base64,$base64Image"

        val userText = buildString {
            if (!locale.isNullOrBlank()) append("Locale: $locale — return all text fields in the language of this locale.\n")
            if (!mealTitle.isNullOrBlank()) append("The user described this meal as: \"$mealTitle\"\n")
            if (!additionalContext.isNullOrBlank()) append("Additional context: \"$additionalContext\"\n")
            append("Analyze the food in this image.")
        }

        val requestBody = buildJsonObject {
            put("model", "gpt-4o-mini")
            put("temperature", 0.2)
            put("max_tokens", 800)
            putJsonObject("response_format") { put("type", "json_object") }
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                }
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        addJsonObject {
                            put("type", "text")
                            put("text", userText)
                        }
                        addJsonObject {
                            put("type", "image_url")
                            putJsonObject("image_url") {
                                put("url", dataUrl)
                                put("detail", "low")
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

        val responseText = response.bodyAsText()
        val responseJson = json.parseToJsonElement(responseText).jsonObject
        val content = responseJson["choices"]
            ?.jsonArray?.get(0)
            ?.jsonObject?.get("message")
            ?.jsonObject?.get("content")
            ?.jsonPrimitive?.content
            ?: error("Unexpected OpenAI response structure")

        return json.decodeFromString(ImageAnalysisResponse.serializer(), content)
    }
}
