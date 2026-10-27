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
        // Desktop mock file upload simulation
        val mockWav = "UklGRiQAAABXQVZFZm10IBAAAAABAAEARKwAAIhYAQACABAAZGF0YQAAAAA=".encodeToByteArray()
        return Base64.encode(mockWav)
    }

    actual fun isRecording(): Boolean = recording
}
