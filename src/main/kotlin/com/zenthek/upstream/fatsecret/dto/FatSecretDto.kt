package com.zenthek.upstream.fatsecret.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
data class FatSecretTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Long
)

// The search endpoint returns lists that can be either object or array
private object FoodSearchListSerializer :
    JsonTransformingSerializer<List<FatSecretFoodSummaryDto>>(
        ListSerializer(FatSecretFoodSummaryDto.serializer())
    ) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonArray) element else JsonArray(listOf(element))
}

@Serializable
data class FatSecretSearchResponse(
    val foods: FatSecretFoodsWrapper
)

@Serializable
data class FatSecretFoodsWrapper(
    @SerialName("max_results") val maxResults: String? = null,
    @SerialName("total_results") val totalResults: String? = null,
    @SerialName("page_number") val pageNumber: String? = null,
    @Serializable(with = FoodSearchListSerializer::class)
    val food: List<FatSecretFoodSummaryDto> = emptyList()
)

@Serializable
data class FatSecretFoodSummaryDto(
    @SerialName("food_id") val foodId: String,
    @SerialName("food_name") val foodName: String,
    @SerialName("food_type") val foodType: String? = null,
    @SerialName("brand_name") val brandName: String? = null,
    @SerialName("food_url") val foodUrl: String? = null,
    @SerialName("food_description") val foodDescription: String
)

@Serializable
data class FatSecretBarcodeResponse(
    val food_id: FatSecretFoodIdDto? = null,  // null = barcode not in FatSecret
)

@Serializable
data class FatSecretFoodIdDto(val value: String = "")

// Serving Lists in details can be JSON object or Array
private object ServingListSerializer :
    JsonTransformingSerializer<List<FatSecretServingDto>>(
        ListSerializer(FatSecretServingDto.serializer())
    ) {
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonArray) element else JsonArray(listOf(element))
}


@Serializable
data class FatSecretFoodDetailResponse(
    val food: FatSecretFoodDetailDto? = null
)

@Serializable
data class FatSecretFoodDetailDto(
    @SerialName("food_id") val foodId: String,
    @SerialName("food_name") val foodName: String,
    @SerialName("food_type") val foodType: String? = null,
    @SerialName("brand_name") val brandName: String? = null,
    val servings: FatSecretServingsWrapper? = null
)

@Serializable
data class FatSecretServingsWrapper(
    @Serializable(with = ServingListSerializer::class)
    val serving: List<FatSecretServingDto> = emptyList()
)

@Serializable
data class FatSecretServingDto(
    @SerialName("serving_id") val servingId: String? = null,
    @SerialName("serving_description") val servingDescription: String? = null,
    @SerialName("metric_serving_amount") val metricServingAmount: String? = null,
    @SerialName("metric_serving_unit") val metricServingUnit: String? = null,
    val calories: String? = null,
    val carbohydrate: String? = null,
    val protein: String? = null,
    val fat: String? = null,
    @SerialName("saturated_fat") val saturatedFat: String? = null,
    val fiber: String? = null,
    val sodium: String? = null,
    val sugar: String? = null
)
