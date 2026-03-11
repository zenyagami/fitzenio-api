package com.zenthek.upstream.openfoodfacts.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Used for GET /api/v3/product/{code} — brands is a comma-separated String
@Serializable
data class OpenFoodFactsProductResponse(
    val code: String?,
    val status: String,
    val product: OpenFoodFactsProduct? = null
)

@Serializable
data class OpenFoodFactsProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("serving_quantity") val servingQuantity: Float? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val nutriments: OpenFoodFactsNutriments? = null
)

// Used for GET https://search.openfoodfacts.org/search — brands is a List<String>
@Serializable
data class OpenFoodFactsV3SearchResponse(
    val hits: List<OpenFoodFactsV3SearchProduct> = emptyList(),
    val count: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 25
)

@Serializable
data class OpenFoodFactsV3SearchProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val brands: List<String>? = null,
    @SerialName("serving_size") val servingSize: String? = null,
    @SerialName("serving_quantity") val servingQuantity: Float? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val nutriments: OpenFoodFactsNutriments? = null
)

@Serializable
data class OpenFoodFactsNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Float? = null,
    @SerialName("proteins_100g") val proteins100g: Float? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Float? = null,
    @SerialName("sugars_100g") val sugars100g: Float? = null,
    @SerialName("fat_100g") val fat100g: Float? = null,
    @SerialName("saturated-fat_100g") val saturatedFat100g: Float? = null,
    @SerialName("fiber_100g") val fiber100g: Float? = null,
    @SerialName("sodium_100g") val sodium100g: Float? = null
)
