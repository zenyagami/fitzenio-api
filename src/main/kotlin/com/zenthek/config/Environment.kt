package com.zenthek.config

data class AppConfig(
    val environment: AppEnvironment,
    val apiKeys: ApiKeys,
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
        val environment = AppEnvironment.fromString(System.getenv("APP_ENVIRONMENT"))

        return when (environment) {
            AppEnvironment.DEVELOPMENT -> createDevelopmentConfig()
            AppEnvironment.PRODUCTION -> createProductionConfig()
        }
    }

    private fun createDevelopmentConfig(): AppConfig {
        return AppConfig(
            environment = AppEnvironment.DEVELOPMENT,
            apiKeys = ApiKeys(
                fatSecretClientId = System.getenv("FATSECRET_CLIENT_ID")
                    ?: "mock_fatsecret_client_id",
                fatSecretClientSecret = System.getenv("FATSECRET_CLIENT_SECRET")
                    ?: "mock_fatsecret_client_secret",
                usdaApiKey = System.getenv("USDA_API_KEY")
                    ?: "mock_usda_api_key",
                openAiApiKey = System.getenv("OPENAI_API_KEY")
                    ?: "mock_openai_api_key"
            )
        )
    }

    private fun createProductionConfig(): AppConfig {
        return AppConfig(
            environment = AppEnvironment.PRODUCTION,
            apiKeys = ApiKeys(
                fatSecretClientId = System.getenv("FATSECRET_CLIENT_ID")
                    ?: "mock_fatsecret_client_id",
                fatSecretClientSecret = System.getenv("FATSECRET_CLIENT_SECRET")
                    ?: "mock_fatsecret_client_secret",
                usdaApiKey = System.getenv("USDA_API_KEY")
                    ?: "mock_usda_api_key",
                openAiApiKey = System.getenv("OPENAI_API_KEY")
                    ?: "mock_openai_api_key"
            )
        )
    }
}