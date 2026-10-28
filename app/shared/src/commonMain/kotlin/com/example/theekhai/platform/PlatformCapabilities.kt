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

data class PhotoCaptureResult(
    val base64Data: String,
    val fileName: String = "evidence.jpg",
)

data class GrievanceEmailDraft(
    val recipientEmail: String,
    val authorityName: String,
    val subject: String,
    val body: String,
    val trackingId: String,
    val photoBase64: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = ""
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

/**
 * Returns a callback that triggers camera or photo selection to attach civic visual evidence.
 */
@Composable
expect fun rememberPhotoCapture(
    onResult: (Result<PhotoCaptureResult>) -> Unit,
): () -> Unit

/**
 * Android/Desktop exporter that generates a downloadable petition draft file and launches
 * Gmail/email app pre-filled with the official authority email address and geotagged evidence info.
 */
@Composable
expect fun rememberEmailDraftExporter(): (GrievanceEmailDraft) -> Unit


