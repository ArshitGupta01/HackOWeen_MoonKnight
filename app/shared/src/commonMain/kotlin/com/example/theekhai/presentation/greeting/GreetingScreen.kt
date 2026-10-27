package com.example.theekhai.presentation.greeting

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.theekhai.domain.model.Greeting
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import theekhai.app.shared.generated.resources.Res
import theekhai.app.shared.generated.resources.compose_multiplatform

/** Stateful entry point: binds the Koin-provided ViewModel to the stateless screen. */
@Composable
fun GreetingRoute(viewModel: GreetingViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GreetingScreen(state = state, onGreetClick = viewModel::onGreetClicked)
}

/** Stateless UI: easy to preview and to test. */
@Composable
fun GreetingScreen(
    state: GreetingUiState,
    onGreetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(painterResource(Res.drawable.compose_multiplatform), null, Modifier.size(96.dp))
        Button(onClick = onGreetClick, enabled = !state.isLoading) {
            Text(if (state.isLoading) "Loading..." else "Click me!")
        }
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(state.history, key = { it.id }) { greeting ->
                Text("Compose: ${greeting.message}")
            }
        }
    }
}

@Preview
@Composable
private fun GreetingScreenPreview() {
    MaterialTheme {
        GreetingScreen(
            state = GreetingUiState(history = listOf(Greeting(1, "Hello, Preview!", 0L))),
            onGreetClick = {},
        )
    }
}
