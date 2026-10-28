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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
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

@Composable
actual fun rememberPhotoCapture(
    onResult: (Result<PhotoCaptureResult>) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            onResult(Result.failure(IllegalStateException("No image selected.")))
            return@rememberLauncherForActivityResult
        }
        scope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (originalBitmap != null) {
                    val maxDim = 1024
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val scaled = if (width > maxDim || height > maxDim) {
                        val ratio = width.toFloat() / height.toFloat()
                        val newWidth = if (width > height) maxDim else (maxDim * ratio).toInt()
                        val newHeight = if (height > width) maxDim else (maxDim / ratio).toInt()
                        Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                    } else {
                        originalBitmap
                    }
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 80, out)
                    val bytes = out.toByteArray()
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val dataUrl = "data:image/jpeg;base64,$b64"
                    withContext(Dispatchers.Main) {
                        onResult(Result.success(PhotoCaptureResult(dataUrl, "evidence_${System.currentTimeMillis()}.jpg")))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(Result.failure(IllegalStateException("Failed to decode selected image.")))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(IllegalStateException("Image processing error: ${e.message}", e)))
                }
            }
        }
    }

    return remember(galleryLauncher) {
        {
            try {
                galleryLauncher.launch("image/*")
            } catch (e: Exception) {
                onResult(Result.failure(IllegalStateException("Unable to open photo picker on this device.", e)))
            }
        }
    }
}

@Composable
actual fun rememberEmailDraftExporter(): (GrievanceEmailDraft) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    return remember(context) {
        { draft ->
            scope.launch(Dispatchers.IO) {
                try {
                    val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                        ?: context.filesDir
                    val draftFile = java.io.File(downloadsDir, "TheekHai_Grievance_${draft.trackingId.take(8)}.txt")
                    val draftContent = buildString {
                        append("=================================================================\n")
                        append("THEEKHAI CIVIC GRIEVANCE PETITION (HARYANA RIGHT TO SERVICE ACT)\n")
                        append("=================================================================\n\n")
                        append("TO OFFICIAL AUTHORITY: ${draft.authorityName}\n")
                        append("OFFICIAL EMAIL: ${draft.recipientEmail}\n")
                        append("CASE TRACKING ID: #${draft.trackingId}\n")
                        append("DATE & TIME: ${java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(java.util.Date())}\n\n")
                        append("SUBJECT:\n${draft.subject}\n\n")
                        append("LOCATION DETAILS:\n")
                        append("Address: ${draft.address}\n")
                        if (draft.latitude != null && draft.longitude != null) {
                            append("GPS Coordinates: ${draft.latitude}° N, ${draft.longitude}° E (Ward 4 Murthal)\n")
                            append("Map Radar Link: https://www.google.com/maps?q=${draft.latitude},${draft.longitude}\n")
                        }
                        append("\nPETITION TEXT:\n")
                        append(draft.body)
                        append("\n\n-----------------------------------------------------------------\n")
                        append("INSTRUCTIONS FOR CITIZEN:\n")
                        append("1. Send this petition to: ${draft.recipientEmail}\n")
                        append("2. Attach your captured geotagged photo evidence\n")
                        append("3. Under Haryana Right to Service Act, this enforces time-bound statutory SLA.\n")
                        append("=================================================================\n")
                    }
                    draftFile.writeText(draftContent, Charsets.UTF_8)

                    if (!draft.photoBase64.isNullOrBlank()) {
                        try {
                            val cleanB64 = draft.photoBase64.substringAfter("base64,")
                            val imageBytes = Base64.decode(cleanB64, Base64.DEFAULT)
                            val photoFile = java.io.File(downloadsDir, "Evidence_${draft.trackingId.take(8)}.jpg")
                            photoFile.writeBytes(imageBytes)
                        } catch (_: Exception) {}
                    }

                    withContext(Dispatchers.Main) {
                        try {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:" + Uri.encode(draft.recipientEmail))
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(draft.recipientEmail))
                                putExtra(Intent.EXTRA_SUBJECT, draft.subject)
                                putExtra(Intent.EXTRA_TEXT, draft.body + "\n\n[GPS Geotag: ${draft.latitude ?: 28.4595}° N, ${draft.longitude ?: 77.0266}° E | Case #${draft.trackingId}]\n(Please attach the captured evidence photo)")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            val chooser = Intent.createChooser(emailIntent, "Send Grievance via Gmail").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(chooser)
                            android.widget.Toast.makeText(
                                context,
                                "Draft saved to Downloads! Opening Gmail for ${draft.recipientEmail}. Remember to attach your photo.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } catch (_: Exception) {
                            android.widget.Toast.makeText(
                                context,
                                "Draft saved: ${draftFile.name}. Send to ${draft.recipientEmail}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Draft export note: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}


