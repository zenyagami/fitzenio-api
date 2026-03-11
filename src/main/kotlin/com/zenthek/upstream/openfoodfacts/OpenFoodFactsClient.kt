package com.zenthek.upstream.openfoodfacts

import com.zenthek.model.FoodItem
import com.zenthek.upstream.openfoodfacts.dto.OpenFoodFactsProductResponse
import com.zenthek.upstream.openfoodfacts.dto.OpenFoodFactsSearchResponse
import com.zenthek.mapper.OpenFoodFactsMapper
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

class OpenFoodFactsClient(private val httpClient: HttpClient) {
    private val baseUrl = "https://world.openfoodfacts.org"

    suspend fun getByBarcode(barcode: String): FoodItem? {
        val response = httpClient.get("$baseUrl/api/v2/product/$barcode") {
            header(HttpHeaders.UserAgent, "Fitzenio/1.0 (Android/iOS app; contact@fitzenio.com)")
            parameter("fields", "code,product_name,brands,serving_size,serving_quantity,image_url,nutriments")
        }

        if (!response.status.isSuccess()) {
            return null
        }

        val dto = response.body<OpenFoodFactsProductResponse>()
        if (dto.status != 1 || dto.product == null) {
            return null
        }

        return OpenFoodFactsMapper.map(dto.product)
    }

    suspend fun search(query: String, page: Int, pageSize: Int): List<FoodItem> {
        val response = httpClient.get("$baseUrl/api/v2/search") {
            header(HttpHeaders.UserAgent, "Fitzenio/1.0 (Android/iOS app; contact@fitzenio.com)")
            parameter("search_terms", query)
            parameter("page", page + 1) // OFF is 1-indexed
            parameter("page_size", pageSize)
            parameter("fields", "code,product_name,brands,serving_size,serving_quantity,image_url,nutriments")
            parameter("sort_by", "unique_scans_n")
        }

        if (!response.status.isSuccess()) {
            return emptyList()
        }

        val dto = response.body<OpenFoodFactsSearchResponse>()
        return dto.products.mapNotNull { OpenFoodFactsMapper.map(it) }
    }
}
