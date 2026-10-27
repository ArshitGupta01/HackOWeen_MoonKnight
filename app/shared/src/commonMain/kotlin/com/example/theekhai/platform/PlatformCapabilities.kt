package com.example.theekhai.platform

import androidx.compose.runtime.Composable

data class GeotagResult(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val ward: String = "Ward 4",
    val source: String = "GPS",
)

data class VoiceCaptureResult(
    val transcript: String,
    val suggestedTitle: String = "",
    val suggestedCategory: String = "",
    val provider: String = "SPEECH_RECOGNIZER",
)

/**
 * Returns a callback that requests device/platform GPS coordinates and reverse-geocodes them
 * into a street/landmark address.
 */
@Composable
expect fun rememberLocationFetcher(
    onResult: (Result<GeotagResult>) -> Unit,
): () -> Unit

/**
 * Returns a callback that starts/stops voice grievance capture and transcribes it into text.
 */
@Composable
expect fun rememberVoiceRecorder(
    isRecording: Boolean,
    onRecordingStateChange: (Boolean) -> Unit,
    onResult: (Result<VoiceCaptureResult>) -> Unit,
): () -> Unit
