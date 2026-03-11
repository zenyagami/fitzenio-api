package com.zenthek.upstream.usda.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsdaSearchResponse(
    val totalHits: Int,
    val currentPage: Int,
    val totalPages: Int,
    val foods: List<UsdaSearchFoodDto>
)

@Serializable
data class UsdaSearchFoodDto(
    val fdcId: Long,
    val description: String,
    val dataType: String? = null,
    val gtinUpc: String? = null,
    val brandOwner: String? = null,
    val brandName: String? = null,
    val servingSize: Float? = null,
    val servingSizeUnit: String? = null,
    val foodNutrients: List<UsdaSearchNutrientDto> = emptyList()
)

@Serializable
data class UsdaSearchNutrientDto(
    val nutrientId: Int,
    val nutrientName: String? = null,
    val unitName: String? = null,
    val value: Float
)
