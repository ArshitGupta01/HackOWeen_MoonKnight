package com.example.theekhai.audio

expect class AudioRecorder() {
    fun startRecording()
    fun stopRecording(): String?
    fun isRecording(): Boolean
}
