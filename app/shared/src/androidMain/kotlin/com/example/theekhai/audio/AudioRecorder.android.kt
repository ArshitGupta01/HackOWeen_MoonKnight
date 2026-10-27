package com.example.theekhai.audio

actual class AudioRecorder actual constructor() {
    private var recording = false

    actual fun startRecording() {
        recording = false
    }

    actual fun stopRecording(): String? {
        recording = false
        return null
    }

    actual fun isRecording(): Boolean = recording
}
