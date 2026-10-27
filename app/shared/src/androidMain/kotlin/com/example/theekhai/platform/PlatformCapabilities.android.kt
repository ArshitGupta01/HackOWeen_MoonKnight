package com.example.theekhai.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
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

    fun reportLocation(location: android.location.Location?) {
        if (location == null) {
            onResult(Result.failure(IllegalStateException("A current location could not be found. Move outdoors and try again, or choose a point on the map.")))
            return
        }
        onResult(
            Result.success(
                GeotagResult(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = "",
                    ward = "",
                    source = "DEVICE_LOCATION",
                )
            )
        )
    }

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
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            ).filter { provider -> runCatching { locationManager?.isProviderEnabled(provider) == true }.getOrDefault(false) }
            if (locationManager == null || providers.isEmpty()) {
                onResult(Result.failure(IllegalStateException("Location services are turned off. Enable them or choose a point on the map.")))
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(providers.first(), CancellationSignal(), context.mainExecutor) { reportLocation(it) }
            } else {
                @Suppress("DEPRECATION")
                val lastKnown = providers.mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
                    .filter { System.currentTimeMillis() - it.time <= 2 * 60 * 1000L }
                    .maxByOrNull { it.time }
                reportLocation(lastKnown)
            }
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

    return remember(context, permissionLauncher) {
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
