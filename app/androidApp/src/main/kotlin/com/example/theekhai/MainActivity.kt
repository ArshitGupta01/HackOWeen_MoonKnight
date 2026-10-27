package com.example.theekhai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.theekhai.presentation.fieldworker.FieldWorkerScreen
import com.example.theekhai.presentation.fieldworker.FieldWorkerUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    MaterialTheme {
        FieldWorkerScreen(
            state = FieldWorkerUiState(),
            onToggleRecording = {},
            onConsentChanged = {},
            onSubmitVoiceGrievance = {},
            onSelectComplaint = {},
            onResolveComplaint = { _, _ -> },
            onSyncOffline = {}
        )
    }
}
