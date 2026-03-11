package com.zenthek.upstream.fatsecret

import com.zenthek.mapper.FatSecretMapper
import com.zenthek.model.FoodItem
import com.zenthek.upstream.fatsecret.dto.FatSecretBarcodeResponse
import com.zenthek.upstream.fatsecret.dto.FatSecretFoodDetailResponse
import com.zenthek.upstream.fatsecret.dto.FatSecretSearchResponse
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

class FatSecretClient(
    private val httpClient: HttpClient,
    private val tokenManager: FatSecretTokenManager
) {
    private val baseUrl = "https://platform.fatsecret.com/rest/server.api"

    suspend fun getByBarcode(barcode: String): FoodItem? {
        val token = tokenManager.getToken()

        // 1. Get food_id from barcode
        val barcodeResponse = httpClient.post(baseUrl) {
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(FormDataContent(Parameters.build {
                append("method", "food.find_id_for_barcode")
                append("barcode", barcode)
                append("format", "json")
            }))
        }

        if (!barcodeResponse.status.isSuccess()) return null

        val barcodeDto = barcodeResponse.body<FatSecretBarcodeResponse>()
        val foodId = barcodeDto.food_id?.value ?: return null

        // 2. Get full details from food_id
        val detailResponse = httpClient.post(baseUrl) {
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(FormDataContent(Parameters.build {
                append("method", "food.get.v4")
                append("food_id", foodId)
                append("format", "json")
            }))
        }

        if (!detailResponse.status.isSuccess()) return null

        val detailDto = detailResponse.body<FatSecretFoodDetailResponse>()
        return detailDto.food?.let { FatSecretMapper.mapDetail(it, barcode) }
    }

    suspend fun search(query: String, page: Int, pageSize: Int): List<FoodItem> {
        val token = tokenManager.getToken()

        val response = httpClient.post(baseUrl) {
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(FormDataContent(Parameters.build {
                append("method", "foods.search")
                append("search_expression", query)
                append("page_number", page.toString())
                append("max_results", pageSize.toString())
                append("format", "json")
            }))
        }

        if (!response.status.isSuccess()) return emptyList()

        val dto = response.body<FatSecretSearchResponse>()
        return dto.foods.food.mapNotNull { FatSecretMapper.mapSummary(it) }
    }
}
