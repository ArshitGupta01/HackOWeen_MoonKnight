package com.example.theekhai.contract

/**
 * Single source of truth for backend HTTP and WebSocket endpoints,
 * shared by the Ktor server and multiplatform clients (Android, Desktop, Web).
 */
object ApiRoutes {
    const val VERSION_PREFIX = "/api/v1"

    // Complaints
    const val COMPLAINTS = "$VERSION_PREFIX/complaints"
    const val COMPLAINT_BY_ID = "$VERSION_PREFIX/complaints/{id}"
    const val COMPLAINT_TRIAGE = "$VERSION_PREFIX/complaints/{id}/triage"
    const val COMPLAINT_RESOLVE = "$VERSION_PREFIX/complaints/{id}/resolve"
    const val COMPLAINT_ESCALATE = "$VERSION_PREFIX/complaints/{id}/escalate"

    // Autonomous Verification (TinyFish agent)
    const val VERIFICATION = "$VERSION_PREFIX/verification"
    const val VERIFY_COMPLAINT = "$VERSION_PREFIX/complaints/{id}/verify"

    // Multilingual Voice Intake (Groq Whisper / Sarvam STT)
    const val VOICE_INTAKE = "$VERSION_PREFIX/voice/intake"

    // Evidence Verification (Groq Vision + Geofencing)
    const val EVIDENCE_CHECK = "$VERSION_PREFIX/evidence/verify"

    // Audit Logs (DPDP 2026 Explainable AI trail)
    const val AUDIT_LOGS = "$VERSION_PREFIX/audit/logs"

    // Offline Sync
    const val SYNC_BATCH = "$VERSION_PREFIX/sync/batch"

    // Leaderboard & Dummy Instant Auth
    const val LEADERBOARD = "$VERSION_PREFIX/leaderboard"
    const val DUMMY_USERS = "$VERSION_PREFIX/auth/dummy-users"
    const val INSTANT_LOGIN = "$VERSION_PREFIX/auth/instant-login"

    // Health
    const val HEALTH = "/health"
}
