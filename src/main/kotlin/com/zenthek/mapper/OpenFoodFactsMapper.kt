package com.zenthek.mapper

import com.zenthek.model.FoodItem
import com.zenthek.model.FoodSource
import com.zenthek.model.NutritionInfo
import com.zenthek.model.ServingSize
import com.zenthek.upstream.openfoodfacts.dto.OpenFoodFactsProduct

object OpenFoodFactsMapper {

    fun map(product: OpenFoodFactsProduct): FoodItem? {
        val name = product.productName?.trim()
        if (name.isNullOrBlank()) return null

        val nutritionPer100g = extractNutrition(product)
        
        // Skip product if no nutrition data is available at all
        if (nutritionPer100g.caloriesKcal == 0f && nutritionPer100g.proteinG == 0f && nutritionPer100g.fatG == 0f) {
            return null
        }

        val code = product.code ?: return null
        
        val brand = product.brands?.split(",")?.firstOrNull()?.trim()?.ifBlank { null }
        
        val servings = buildServings(product, nutritionPer100g)

        return FoodItem(
            id = "OFF_$code",
            name = name,
            brand = brand,
            barcode = code,
            source = FoodSource.OPEN_FOOD_FACTS,
            imageUrl = product.imageUrl,
            servings = servings
        )
    }

    private fun extractNutrition(product: OpenFoodFactsProduct): NutritionInfo {
        val nutriments = product.nutriments
        return NutritionInfo(
            caloriesKcal = nutriments?.energyKcal100g ?: 0f,
            proteinG = nutriments?.proteins100g ?: 0f,
            carbsG = nutriments?.carbohydrates100g ?: 0f,
            fatG = nutriments?.fat100g ?: 0f,
            fiberG = nutriments?.fiber100g,
            sodiumMg = nutriments?.sodium100g?.let { it * 1000f }, // convert g to mg
            sugarG = nutriments?.sugars100g,
            saturatedFatG = nutriments?.saturatedFat100g
        )
    }

    private fun buildServings(product: OpenFoodFactsProduct, nutritionPer100g: NutritionInfo): List<ServingSize> {
        val servings = mutableListOf<ServingSize>()
        
        // Always add 100g serving
        servings.add(
            ServingSize(
                name = "100g",
                weightGrams = 100f,
                nutrition = nutritionPer100g
            )
        )

        // Add additional serving if serving_quantity specified
        val servingQuantity = product.servingQuantity
        if (servingQuantity != null && servingQuantity > 0f && servingQuantity != 100f) {
            servings.add(
                ServingSize(
                    name = product.servingSize ?: "${servingQuantity}g",
                    weightGrams = servingQuantity,
                    nutrition = scaleNutrition(nutritionPer100g, servingQuantity / 100f)
                )
            )
        }

        return servings
    }

    private fun scaleNutrition(nutrition: NutritionInfo, scaleFactor: Float): NutritionInfo {
        return NutritionInfo(
            caloriesKcal = nutrition.caloriesKcal * scaleFactor,
            proteinG = nutrition.proteinG * scaleFactor,
            carbsG = nutrition.carbsG * scaleFactor,
            fatG = nutrition.fatG * scaleFactor,
            fiberG = nutrition.fiberG?.let { it * scaleFactor },
            sodiumMg = nutrition.sodiumMg?.let { it * scaleFactor },
            sugarG = nutrition.sugarG?.let { it * scaleFactor },
            saturatedFatG = nutrition.saturatedFatG?.let { it * scaleFactor }
        )
    }
}
