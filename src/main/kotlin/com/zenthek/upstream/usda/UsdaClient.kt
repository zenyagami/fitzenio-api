package com.zenthek.upstream.usda

import com.zenthek.mapper.UsdaMapper
import com.zenthek.model.FoodItem
import com.zenthek.upstream.usda.dto.UsdaSearchResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class UsdaClient(
    private val httpClient: HttpClient,
    private val apiKey: String
) {
    private val baseUrl = "https://api.nal.usda.gov/fdc/v1"

    suspend fun getByBarcode(barcode: String): FoodItem? {
        val response = httpClient.get("$baseUrl/foods/search") {
            parameter("api_key", apiKey)
            parameter("query", barcode)
            parameter("dataType", "Branded")
        }

        if (!response.status.isSuccess()) return null

        val dto = response.body<UsdaSearchResponse>()
        val exactMatch = dto.foods.firstOrNull { it.gtinUpc == barcode } ?: return null
        
        return UsdaMapper.mapSearchItem(exactMatch)
    }

    suspend fun search(query: String, page: Int, pageSize: Int): List<FoodItem> {
        val response = httpClient.get("$baseUrl/foods/search") {
            parameter("api_key", apiKey)
            parameter("query", query)
            parameter("pageNumber", page + 1) // USDA is 1-indexed
            parameter("pageSize", pageSize)
            parameter("dataType", "Branded,Foundation,SR Legacy")
        }

        if (!response.status.isSuccess()) return emptyList()

        val dto = response.body<UsdaSearchResponse>()
        return dto.foods.mapNotNull { UsdaMapper.mapSearchItem(it) }
    }
}
