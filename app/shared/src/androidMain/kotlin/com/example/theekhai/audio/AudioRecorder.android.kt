package com.example.theekhai.audio

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class AudioRecorder actual constructor() {
    private var recording = false

    actual fun startRecording() {
        recording = true
    }

    @OptIn(ExperimentalEncodingApi::class)
    actual fun stopRecording(): String? {
        recording = false
        // Simulates captured PCM audio on Android device encoded to Base64
        val simulatedPcm = ByteArray(2048) { (it % 127).toByte() }
        return Base64.encode(simulatedPcm)
    }

    actual fun isRecording(): Boolean = recording
}
