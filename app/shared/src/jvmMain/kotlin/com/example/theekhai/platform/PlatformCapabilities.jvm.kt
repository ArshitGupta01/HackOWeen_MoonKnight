package com.example.theekhai.platform

import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64
import java.util.Locale
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

private val desktopJson = Json { ignoreUnknownKeys = true }

@Composable
actual fun rememberLocationFetcher(
    onResult: (Result<GeotagResult>) -> Unit,
): () -> Unit {
    return remember(onResult) {
        { onResult(Result.failure(IllegalStateException("This desktop does not provide device location. Choose a point on the Murthal map instead."))) }
    }
}

@Composable
actual fun rememberVoiceRecorder(
    isRecording: Boolean,
    onRecordingStateChange: (Boolean) -> Unit,
    onResult: (Result<VoiceCaptureResult>) -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    val lineHolder = remember { mutableStateOf<TargetDataLine?>(null) }
    val bufferHolder = remember { mutableStateOf<ByteArrayOutputStream?>(null) }
    val format = remember { AudioFormat(16000f, 16, 1, true, false) }

    return remember(isRecording) {
        {
            if (!isRecording) {
                // Start recording from desktop microphone if available
                onRecordingStateChange(true)
                scope.launch(Dispatchers.IO) {
                    try {
                        val info = DataLine.Info(TargetDataLine::class.java, format)
                        if (AudioSystem.isLineSupported(info)) {
                            val line = AudioSystem.getLine(info) as TargetDataLine
                            line.open(format)
                            line.start()
                            val out = ByteArrayOutputStream()
                            lineHolder.value = line
                            bufferHolder.value = out
                            val buf = ByteArray(4096)
                            while (line.isOpen) {
                                val read = line.read(buf, 0, buf.size)
                                if (read > 0) out.write(buf, 0, read) else break
                            }
                        }
                    } catch (_: Exception) {
                        // Microphone hardware optional on desktop; handled when stopping
                    }
                }
            } else {
                // Stop recording and send to /api/v1/voice/transcribe
                onRecordingStateChange(false)
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching {
                            val line = lineHolder.value
                            val pcmOut = bufferHolder.value
                            line?.stop()
                            line?.close()
                            lineHolder.value = null
                            bufferHolder.value = null

                            val wavBytes = if (pcmOut != null && pcmOut.size() > 64) {
                                val rawPcm = pcmOut.toByteArray()
                                val wavStream = ByteArrayOutputStream()
                                val ais = AudioInputStream(
                                    ByteArrayInputStream(rawPcm),
                                    format,
                                    rawPcm.size.toLong() / format.frameSize,
                                )
                                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavStream)
                                wavStream.toByteArray()
                            } else {
                                throw IllegalStateException("No audio was captured. Check the desktop microphone and try again.")
                            }

                            val b64 = Base64.getEncoder().encodeToString(wavBytes)
                            val backendUrl = (System.getProperty("THEEKHAI_BACKEND_URL")
                                ?: System.getenv("THEEKHAI_BACKEND_URL")
                                ?: "http://127.0.0.1:8080").trimEnd('/')
                            val conn = (URI("$backendUrl/api/v1/voice/transcribe").toURL().openConnection() as HttpURLConnection).apply {
                                requestMethod = "POST"
                                doOutput = true
                                setRequestProperty("Content-Type", "application/json")
                                setRequestProperty("ngrok-skip-browser-warning", "true")
                                connectTimeout = 10000
                                readTimeout = 15000
                            }
                            val payload = buildJsonObject {
                                put("audioBase64", b64)
                                put("mimeType", "audio/wav")
                                put("language", "en")
                            }.toString()
                            conn.outputStream.use { it.write(payload.toByteArray()) }

                            if (conn.responseCode in 200..299) {
                                val respText = conn.inputStream.bufferedReader().readText()
                                val obj = desktopJson.parseToJsonElement(respText).jsonObject
                                val text = obj["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                val title = obj["suggestedTitle"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                val cat = obj["suggestedCategory"]?.jsonPrimitive?.contentOrNull.orEmpty()
                                val prov = obj["provider"]?.jsonPrimitive?.contentOrNull ?: "OPENROUTER_AI"
                                if (text.isBlank()) throw IllegalStateException("The transcription service returned no speech. Please try again or type your report.")
                                VoiceCaptureResult(
                                    transcript = text,
                                    suggestedTitle = title,
                                    suggestedCategory = cat.ifBlank { "ROADS" },
                                    provider = prov,
                                )
                            } else {
                                throw IllegalStateException("Transcription failed with HTTP ${conn.responseCode}. Try again or enter the report manually.")
                            }
                        }
                    }
                    onResult(result)
                }
            }
        }
    }
}

@Composable
actual fun rememberPhotoCapture(
    onResult: (Result<PhotoCaptureResult>) -> Unit,
): () -> Unit {
    val scope = rememberCoroutineScope()
    return remember {
        {
            scope.launch(Dispatchers.IO) {
                try {
                    val chooser = javax.swing.JFileChooser().apply {
                        dialogTitle = "Select Civic Visual Evidence (Photo / Capture)"
                        fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                            "Image Files (*.jpg, *.jpeg, *.png, *.webp)", "jpg", "jpeg", "png", "webp"
                        )
                        isMultiSelectionEnabled = false
                    }
                    val userSelection = chooser.showOpenDialog(null)
                    if (userSelection == javax.swing.JFileChooser.APPROVE_OPTION) {
                        val file = chooser.selectedFile
                        if (file != null && file.exists()) {
                            val img = javax.imageio.ImageIO.read(file)
                            if (img != null) {
                                val maxDim = 1024
                                val w = img.width
                                val h = img.height
                                val scaled = if (w > maxDim || h > maxDim) {
                                    val ratio = w.toDouble() / h.toDouble()
                                    val nw = if (w > h) maxDim else (maxDim * ratio).toInt()
                                    val nh = if (h > w) maxDim else (maxDim / ratio).toInt()
                                    val resized = java.awt.image.BufferedImage(nw, nh, java.awt.image.BufferedImage.TYPE_INT_RGB)
                                    val g = resized.createGraphics()
                                    g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                                    g.drawImage(img, 0, 0, nw, nh, null)
                                    g.dispose()
                                    resized
                                } else {
                                    img
                                }
                                val baos = java.io.ByteArrayOutputStream()
                                javax.imageio.ImageIO.write(scaled, "jpg", baos)
                                val b64 = java.util.Base64.getEncoder().encodeToString(baos.toByteArray())
                                val dataUrl = "data:image/jpeg;base64,$b64"
                                withContext(Dispatchers.Main) {
                                    onResult(Result.success(PhotoCaptureResult(dataUrl, file.name)))
                                }
                            } else {
                                val bytes = file.readBytes()
                                val b64 = java.util.Base64.getEncoder().encodeToString(bytes)
                                val mime = if (file.name.endsWith(".png", true)) "image/png" else "image/jpeg"
                                withContext(Dispatchers.Main) {
                                    onResult(Result.success(PhotoCaptureResult("data:$mime;base64,$b64", file.name)))
                                }
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onResult(Result.failure(IllegalStateException("No photo was selected.")))
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onResult(Result.failure(IllegalStateException("Failed to load photo: ${e.message}", e)))
                    }
                }
            }
        }
    }
}

@Composable
actual fun rememberEmailDraftExporter(): (GrievanceEmailDraft) -> Unit {
    val scope = rememberCoroutineScope()

    return remember {
        { draft ->
            scope.launch(Dispatchers.IO) {
                try {
                    val userHome = System.getProperty("user.home")
                    val downloadsDir = java.io.File(userHome, "Downloads").takeIf { it.exists() }
                        ?: java.io.File(userHome, ".theekhai").apply { mkdirs() }
                    val draftFile = java.io.File(downloadsDir, "TheekHai_Grievance_${draft.trackingId.take(8)}.txt")
                    val draftContent = buildString {
                        append("=================================================================\n")
                        append("THEEKHAI CIVIC GRIEVANCE PETITION (HARYANA RIGHT TO SERVICE ACT)\n")
                        append("=================================================================\n\n")
                        append("TO OFFICIAL AUTHORITY: ${draft.authorityName}\n")
                        append("OFFICIAL EMAIL: ${draft.recipientEmail}\n")
                        append("CASE TRACKING ID: #${draft.trackingId}\n\n")
                        append("SUBJECT:\n${draft.subject}\n\n")
                        append("LOCATION DETAILS:\n")
                        append("Address: ${draft.address}\n")
                        if (draft.latitude != null && draft.longitude != null) {
                            append("GPS Coordinates: ${draft.latitude}° N, ${draft.longitude}° E (Ward 4 Murthal)\n")
                        }
                        append("\nPETITION TEXT:\n")
                        append(draft.body)
                        append("\n\n-----------------------------------------------------------------\n")
                        append("INSTRUCTIONS FOR CITIZEN:\n")
                        append("1. Send this petition to: ${draft.recipientEmail}\n")
                        append("2. Attach your captured geotagged photo evidence\n")
                        append("=================================================================\n")
                    }
                    draftFile.writeText(draftContent, Charsets.UTF_8)

                    if (!draft.photoBase64.isNullOrBlank()) {
                        try {
                            val clean = draft.photoBase64.substringAfter("base64,")
                            val bytes = java.util.Base64.getDecoder().decode(clean)
                            val photoFile = java.io.File(downloadsDir, "Evidence_${draft.trackingId.take(8)}.jpg")
                            photoFile.writeBytes(bytes)
                        } catch (_: Exception) {}
                    }

                    try {
                        if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.MAIL)) {
                            val mailto = "mailto:${draft.recipientEmail}?subject=${java.net.URLEncoder.encode(draft.subject, "UTF-8")}&body=${java.net.URLEncoder.encode(draft.body, "UTF-8")}"
                            java.awt.Desktop.getDesktop().mail(java.net.URI(mailto))
                        } else if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                            java.awt.Desktop.getDesktop().open(draftFile)
                        }
                    } catch (_: Exception) {}
                } catch (_: Exception) {}
            }
        }
    }
}


