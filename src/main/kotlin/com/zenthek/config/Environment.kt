package com.zenthek.config

import io.github.cdimascio.dotenv.dotenv

private val dotenv = dotenv { ignoreIfMissing = true }
private fun env(key: String): String? = dotenv[key]

data class AppConfig(
    val environment: AppEnvironment,
    val apiKeys: ApiKeys,
    val useGemini: Boolean,
    val geminiApiKey: String,
)

data class ApiKeys(
    val fatSecretClientId: String,
    val fatSecretClientSecret: String,
    val usdaApiKey: String,
    val openAiApiKey: String
)


enum class AppEnvironment {
    DEVELOPMENT,
    PRODUCTION;

    fun isDebug() = this == DEVELOPMENT

    companion object {
        fun fromString(env: String?): AppEnvironment {
            return when (env?.uppercase()) {
                "PRODUCTION", "PROD" -> PRODUCTION
                else -> DEVELOPMENT
            }
        }
    }
}

object ConfigLoader {
    fun loadConfig(): AppConfig {
        val environment = AppEnvironment.fromString(env("APP_ENVIRONMENT"))

        return when (environment) {
            AppEnvironment.DEVELOPMENT -> createDevelopmentConfig()
            AppEnvironment.PRODUCTION -> createProductionConfig()
        }
    }

    private fun createDevelopmentConfig(): AppConfig {
        return AppConfig(
            environment = AppEnvironment.DEVELOPMENT,
            apiKeys = ApiKeys(
                fatSecretClientId = env("FATSECRET_CLIENT_ID") ?: error("Missing FATSECRET_CLIENT_ID"),
                fatSecretClientSecret = env("FATSECRET_CLIENT_SECRET") ?: error("Missing FATSECRET_CLIENT_SECRET"),
                usdaApiKey = env("USDA_API_KEY") ?: error("Missing USDA_API_KEY"),
                openAiApiKey = env("OPENAI_API_KEY") ?: error("Missing OPENAI_API_KEY")
            ),
            useGemini = true,
            geminiApiKey = env("GEMINI_API_KEY") ?: error("Missing GEMINI_API_KEY"),
        )
    }

    private fun createProductionConfig(): AppConfig {
        return AppConfig(
            environment = AppEnvironment.PRODUCTION,
            apiKeys = ApiKeys(
                fatSecretClientId = env("FATSECRET_CLIENT_ID") ?: error("Missing FATSECRET_CLIENT_ID"),
                fatSecretClientSecret = env("FATSECRET_CLIENT_SECRET") ?: error("Missing FATSECRET_CLIENT_SECRET"),
                usdaApiKey = env("USDA_API_KEY") ?: error("Missing USDA_API_KEY"),
                openAiApiKey = env("OPENAI_API_KEY") ?: error("Missing OPENAI_API_KEY")
            ),
            useGemini = true,
            geminiApiKey = env("GEMINI_API_KEY") ?: error("Missing GEMINI_API_KEY")
        )
    }
}