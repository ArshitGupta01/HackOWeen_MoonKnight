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
