package com.example.theekhai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.theekhai.presentation.greeting.GreetingScreen
import com.example.theekhai.presentation.greeting.GreetingUiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

// Previews use the stateless screen: `App()` needs Koin, which only exists at runtime.
@Preview
@Composable
fun AppAndroidPreview() {
    MaterialTheme {
        GreetingScreen(state = GreetingUiState(), onGreetClick = {})
    }
}
