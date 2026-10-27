package com.example.theekhai.data.supabase

data class SupabaseConfig(
    val url: String = "https://theekhai-civic.supabase.co",
    val anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.theekhai_anon_key_mock",
    val serviceRoleKey: String = "",
    val realtimeEnabled: Boolean = true
)
