package com.zenthek.mapper

import com.zenthek.model.FoodItem
import com.zenthek.model.FoodSource
import com.zenthek.model.NutritionInfo
import com.zenthek.model.ServingSize
import com.zenthek.upstream.fatsecret.dto.FatSecretFoodDetailDto
import com.zenthek.upstream.fatsecret.dto.FatSecretFoodSummaryDto
import com.zenthek.upstream.fatsecret.dto.FatSecretServingDto

object FatSecretMapper {

    fun mapDetail(detail: FatSecretFoodDetailDto, barcode: String? = null): FoodItem? {
        val name = detail.foodName.trim()
        if (name.isBlank()) return null

        val servings = detail.servings?.serving?.mapNotNull { mapServing(it) } ?: emptyList()
        if (servings.isEmpty()) return null

        return FoodItem(
            id = "FS_${detail.foodId}",
            name = name,
            brand = detail.brandName,
            barcode = barcode,
            source = FoodSource.FATSECRET,
            imageUrl = null, // Free tier doesn't have images
            servings = servings
        )
    }

    fun mapSummary(summary: FatSecretFoodSummaryDto): FoodItem? {
        val name = summary.foodName.trim()
        if (name.isBlank()) return null

        val nutrition = parseNutritionFromDescription(summary.foodDescription) ?: return null

        val servings = listOf(
            ServingSize(
                // In search results, description often says "Per 100g" or similar
                name = extractServingNameFromDescription(summary.foodDescription) ?: "1 serving",
                weightGrams = extractWeightFromDescription(summary.foodDescription) ?: 100f,
                nutrition = nutrition
            )
        )

        return FoodItem(
            id = "FS_${summary.foodId}",
            name = name,
            brand = summary.brandName,
            barcode = null, // Search results don't guarantee barcode match linkage unless fetched
            source = FoodSource.FATSECRET,
            imageUrl = null, // Free tier doesn't have images
            servings = servings
        )
    }

    private fun mapServing(dto: FatSecretServingDto): ServingSize? {
        val weight = dto.metricServingAmount?.toFloatOrNull() ?: return null

        return ServingSize(
            name = dto.servingDescription ?: "${weight}${dto.metricServingUnit ?: "g"}",
            weightGrams = weight,
            nutrition = NutritionInfo(
                caloriesKcal = dto.calories?.toFloatOrNull() ?: 0f,
                proteinG = dto.protein?.toFloatOrNull() ?: 0f,
                carbsG = dto.carbohydrate?.toFloatOrNull() ?: 0f,
                fatG = dto.fat?.toFloatOrNull() ?: 0f,
                fiberG = dto.fiber?.toFloatOrNull(),
                sodiumMg = dto.sodium?.toFloatOrNull(), // FatSecret sodium is already mg
                sugarG = dto.sugar?.toFloatOrNull(),
                saturatedFatG = dto.saturatedFat?.toFloatOrNull()
            )
        )
    }

    // Example description: "Per 100g - Calories: 61kcal | Fat: 3.25g | Carbs: 4.80g | Prot: 3.15g"
    private val descriptionRegex = Regex("""Calories: ([\d.]+)kcal\s*\|\s*Fat: ([\d.]+)g\s*\|\s*Carbs: ([\d.]+)g\s*\|\s*Prot: ([\d.]+)g""")
    
    // Example: "Per 100g - ..."
    private val perRegex = Regex("""Per\s+(.*?)\s+-""")
    
    private fun parseNutritionFromDescription(description: String): NutritionInfo? {
        val matchResult = descriptionRegex.find(description) ?: return null
        val (kcal, fat, carbs, prot) = matchResult.destructured
        
        return NutritionInfo(
            caloriesKcal = kcal.toFloatOrNull() ?: 0f,
            fatG = fat.toFloatOrNull() ?: 0f,
            carbsG = carbs.toFloatOrNull() ?: 0f,
            proteinG = prot.toFloatOrNull() ?: 0f,
            fiberG = null,
            sodiumMg = null,
            sugarG = null,
            saturatedFatG = null
        )
    }

    private fun extractServingNameFromDescription(description: String): String? {
        return perRegex.find(description)?.groupValues?.get(1)?.trim()
    }
    
    private fun extractWeightFromDescription(description: String): Float? {
        val name = extractServingNameFromDescription(description) ?: return null
        // Extract the number from standard formats like "100g", "1 serving (30g)"
        return Regex("""(\d+(?:\.\d+)?)""").find(name)?.groupValues?.get(1)?.toFloatOrNull()
    }
}
