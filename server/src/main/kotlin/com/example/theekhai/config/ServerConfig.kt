package com.example.theekhai.config

data class ServerConfig(
    val groqApiKey: String = System.getenv("GROQ_API_KEY") ?: "",
    val openRouterApiKey: String = System.getenv("OPENROUTER_API_KEY") ?: "",
    val geminiApiKey: String = System.getenv("GEMINI_API_KEY") ?: "",
    val tinyFishApiKey: String = System.getenv("TINYFISH_API_KEY") ?: "",
    val supabaseUrl: String = System.getenv("SUPABASE_URL") ?: "https://theekhai-civic.supabase.co",
    val supabaseKey: String = System.getenv("SUPABASE_KEY") ?: "theekhai_service_role_key_mock",
    val supabaseJwtSecret: String = System.getenv("SUPABASE_JWT_SECRET") ?: "super-secret-jwt-token-with-at-least-32-characters-long"
) {
    val isGroqReady: Boolean get() = groqApiKey.isNotBlank()
    val isOpenRouterReady: Boolean get() = openRouterApiKey.isNotBlank()
    val isGeminiReady: Boolean get() = geminiApiKey.isNotBlank()
    val isTinyFishReady: Boolean get() = tinyFishApiKey.isNotBlank()
}
