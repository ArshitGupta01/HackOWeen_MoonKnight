package com.example.theekhai.presentation.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.theme.LocalTheekHaiColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen() {
    val navigator = LocalNavigator.current
    val ext = LocalTheekHaiColors.current
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ROADS") }
    var address by remember { mutableStateOf("") }
    var consentGranted by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var hasRecording by remember { mutableStateOf(false) }

    val categories = listOf(
        "ROADS" to "Roads & footpaths",
        "WATER" to "Water & drains",
        "WASTE" to "Waste & sanitation",
        "LIGHTING" to "Street lighting",
        "OTHER" to "Something else"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        // Page heading
        Column(modifier = Modifier.padding(bottom = 30.dp)) {
            Eyebrow("YOUR VOICE MATTERS")
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Let's make it right.",
                style = MaterialTheme.typography.headlineLarge,
                color = ext.ink,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "A few details help the right team understand the problem.",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(60.dp)
        ) {
            // Left: Report form
            Column(modifier = Modifier.weight(1.6f)) {
                PanelCard {
                    // Title
                    Text("Give the issue a short title", style = MaterialTheme.typography.labelMedium, color = ext.ink)
                    Spacer(modifier = Modifier.height(9.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("e.g. Streetlight out near the crossing", fontSize = 12.sp, color = Color(0xFF839083)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(3.dp),
                        singleLine = true,
                        colors = reportFieldColors(ext)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Category dropdown
                    Text("Type of issue", style = MaterialTheme.typography.labelMedium, color = ext.ink)
                    Spacer(modifier = Modifier.height(9.dp))
                    var categoryExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = categories.find { it.first == selectedCategory }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            shape = RoundedCornerShape(3.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            colors = reportFieldColors(ext)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        selectedCategory = value
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Description
                    Text("What is happening?", style = MaterialTheme.typography.labelMedium, color = ext.ink)
                    Spacer(modifier = Modifier.height(9.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Describe the issue and how it affects the neighbourhood.", fontSize = 12.sp, color = Color(0xFF839083)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        shape = RoundedCornerShape(3.dp),
                        maxLines = 8,
                        colors = reportFieldColors(ext)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Audio Recording Panel
                    Surface(
                        color = Color(0xFFEEF1E9),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Record button
                                TheekHaiButton(
                                    text = if (isRecording) "⏹ Stop recording" else "🎙 Record a voice note",
                                    onClick = {
                                        if (isRecording) {
                                            isRecording = false
                                            hasRecording = true
                                        } else {
                                            isRecording = true
                                            hasRecording = false
                                        }
                                    },
                                    isSecondary = true,
                                )

                                if (hasRecording) {
                                    TextButton(onClick = {
                                        // TODO: Wire transcription to Groq Whisper
                                        statusMessage = "Transcription sent to Groq Whisper..."
                                    }) {
                                        Text("Transcribe", fontSize = 12.sp, color = ext.green)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when {
                                    isRecording -> "Microphone is recording. Stop when finished."
                                    hasRecording -> "Recording ready. Transcribe it or continue with typed text."
                                    else -> "Prefer speaking? Record, then transcribe and edit before submitting."
                                },
                                fontSize = 11.sp,
                                color = ext.muted,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Address
                    Text("Address or landmark", style = MaterialTheme.typography.labelMedium, color = ext.ink)
                    Spacer(modifier = Modifier.height(9.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("e.g. MG Road Junction, Near Central Park", fontSize = 12.sp, color = Color(0xFF839083)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(3.dp),
                        singleLine = true,
                        colors = reportFieldColors(ext)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TheekHaiButton(
                        text = "📍 Use my location",
                        onClick = { /* TODO: get GPS */ },
                        isSecondary = true,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Location is optional. You can describe the address instead.",
                        fontSize = 11.sp,
                        color = ext.muted,
                    )

                    Spacer(modifier = Modifier.height(25.dp))

                    // Consent checkbox
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Checkbox(
                            checked = consentGranted,
                            onCheckedChange = { consentGranted = it },
                            colors = CheckboxDefaults.colors(checkedColor = ext.green)
                        )
                        Text(
                            text = "I agree to share these details with the teams handling this report.",
                            fontSize = 12.sp,
                            lineHeight = 20.sp,
                            color = ext.ink,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status message
                    AnimatedVisibility(visible = statusMessage != null) {
                        Surface(
                            color = if (isError) Color(0xFFF9EDE1) else Color(0xFFE7EEE1),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp)
                        ) {
                            Text(
                                text = statusMessage ?: "",
                                color = if (isError) Color(0xFF8A3D24) else ext.ink,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(15.dp)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TheekHaiButton(
                            text = "Review & submit report  ↗",
                            onClick = {
                                if (!consentGranted) {
                                    statusMessage = "Please consent to sharing before submitting."
                                    isError = true
                                    return@TheekHaiButton
                                }
                                isSubmitting = true
                                statusMessage = "Submitting report for AI triage..."
                                isError = false
                                // TODO: Wire to API
                                navigator.navigateTo(Route.Workspace)
                            },
                            enabled = title.isNotBlank() && description.isNotBlank() && !isSubmitting,
                        )
                        TextButton(onClick = {
                            statusMessage = "Draft saved on this device."
                            isError = false
                        }) {
                            Text("Save draft on this device", fontSize = 12.sp, color = ext.green)
                        }
                    }
                }
            }

            // Right: Aside info
            Column(modifier = Modifier.weight(1f).padding(top = 30.dp)) {
                Eyebrow("WHAT HAPPENS NEXT")
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "A report with\na way forward.",
                    style = MaterialTheme.typography.headlineLarge,
                    color = ext.ink,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your report goes to an accountable department. You can follow tasks, due dates and the final review from your workspace.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    lineHeight = 20.sp,
                )

                Spacer(modifier = Modifier.height(22.dp))
                TheekHaiDivider()
                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "This is a civic pilot. It is not an emergency service.",
                    fontSize = 12.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF9A5B18),
                )

                Spacer(modifier = Modifier.height(22.dp))
                TheekHaiDivider()
                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "How we use your information →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.W600,
                    color = ext.green,
                )
            }
        }
    }
}

@Composable
private fun reportFieldColors(ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFFCDD4C9),
        unfocusedBorderColor = Color(0xFFCDD4C9),
        focusedContainerColor = Color(0xFFFFFEF9),
        unfocusedContainerColor = Color(0xFFFFFEF9),
        focusedTextColor = ext.ink,
        unfocusedTextColor = ext.ink,
    )
