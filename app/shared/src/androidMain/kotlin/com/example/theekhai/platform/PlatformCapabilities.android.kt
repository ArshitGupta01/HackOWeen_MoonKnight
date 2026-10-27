package com.example.theekhai.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Locale

@Composable
actual fun rememberLocationFetcher(
    onResult: (Result<GeotagResult>) -> Unit,
): () -> Unit {
    val context = LocalContext.current

    fun resolveDeviceLocation() {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!fineGranted && !coarseGranted) {
                onResult(Result.failure(IllegalStateException("Location permission was denied. Tap on the interactive Ward Map below to pin your location.")))
                return
            }

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
            val lastKnown = providers.firstNotNullOfOrNull { provider ->
                runCatching { locationManager?.getLastKnownLocation(provider) }.getOrNull()
            }

            if (lastKnown == null) {
                onResult(Result.failure(IllegalStateException("No recent device location is available. Try again outdoors or pin the location on the Ward Map.")))
                return
            }
            val lat = lastKnown.latitude
            val lon = lastKnown.longitude
            val sourceLabel = "ANDROID_GPS"

            val geocodedAddress = runCatching {
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
                val first = addresses?.firstOrNull()
                if (first != null) {
                    listOfNotNull(first.thoroughfare ?: first.featureName, first.subLocality ?: "Ward 4", first.locality)
                        .distinct()
                        .joinToString(", ")
                        .takeIf { it.length >= 5 }
                } else null
            }.getOrNull() ?: "Main Market Road near Ward 4 Primary School, Ward 4 (${String.format(Locale.US, "%.4f° N, %.4f° E", lat, lon)})"

            onResult(
                Result.success(
                    GeotagResult(
                        latitude = lat,
                        longitude = lon,
                        address = geocodedAddress,
                        ward = "Ward 4",
                        source = sourceLabel,
                    )
                )
            )
        } catch (e: Exception) {
            onResult(Result.failure(e))
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            resolveDeviceLocation()
        } else {
            onResult(Result.failure(IllegalStateException("Location permission was not granted. Use the Ward Map picker below to select your location.")))
        }
    }

    return remember(context) {
        {
            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (fineGranted || coarseGranted) {
                resolveDeviceLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                )
            }
        }
    }
}

@Composable
actual fun rememberVoiceRecorder(
    isRecording: Boolean,
    onRecordingStateChange: (Boolean) -> Unit,
    onResult: (Result<VoiceCaptureResult>) -> Unit,
): () -> Unit {
    val context = LocalContext.current

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        onRecordingStateChange(false)
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val matches = activityResult.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotBlank()) {
                val lower = spoken.lowercase()
                val category = when {
                    lower.contains("pothole") || lower.contains("road") || lower.contains("सड़क") -> "ROADS"
                    lower.contains("water") || lower.contains("drain") || lower.contains("sewage") || lower.contains("पानी") -> "WATER"
                    lower.contains("garbage") || lower.contains("waste") || lower.contains("कचरा") -> "WASTE"
                    lower.contains("light") || lower.contains("lamp") || lower.contains("लाइट") -> "LIGHTING"
                    else -> ""
                }
                val title = spoken.split(Regex("\\s+")).take(7).joinToString(" ").take(75)
                onResult(
                    Result.success(
                        VoiceCaptureResult(
                            transcript = spoken,
                            suggestedTitle = title,
                            suggestedCategory = category,
                            provider = "ANDROID_SPEECH_RECOGNIZER",
                        )
                    )
                )
            } else {
                onResult(Result.failure(IllegalStateException("No speech was detected. Please try speaking again.")))
            }
        } else {
            onResult(Result.failure(IllegalStateException("Voice dictation was cancelled.")))
        }
    }

    fun launchSpeechRecognizer() {
        try {
            onRecordingStateChange(true)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your civic issue (English or Hindi)…")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            onRecordingStateChange(false)
            onResult(Result.failure(IllegalStateException("Voice recognition is unavailable on this device.", e)))
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchSpeechRecognizer()
        } else {
            onRecordingStateChange(false)
            onResult(Result.failure(IllegalStateException("Microphone permission was denied.")))
        }
    }

    return remember(context, isRecording) {
        {
            if (isRecording) {
                onRecordingStateChange(false)
            } else {
                val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (hasMicPermission) {
                    launchSpeechRecognizer()
                } else {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }
}
