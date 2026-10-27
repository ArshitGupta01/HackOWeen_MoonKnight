package com.example.theekhai.presentation.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.domain.model.CivicGuidanceSource
import com.example.theekhai.presentation.nativeconnect.NativeConnectViewModel
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors
import org.koin.compose.viewmodel.koinViewModel

data class LegalCitation(
    val id: String,
    val title: String,
    val jurisdiction: String,
    val language: String,
    val snippet: String,
    val url: String,
    var isSaved: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val viewModel = koinViewModel<NativeConnectViewModel>()
    val state by viewModel.state.collectAsState()
    val ext = LocalTheekHaiColors.current

    var query by remember { mutableStateOf("") }
    var jurisdiction by remember { mutableStateOf("HARYANA") }
    var jurisExpanded by remember { mutableStateOf(false) }

    val isSearching = state.isLoading
    val synthesizedAnswer = state.guidanceAnswer?.answer ?: if (state.guidanceSources.isNotEmpty()) "Approved references matching your question are listed below." else null
    val citations = (state.guidanceAnswer?.citations ?: state.guidanceSources).map { source ->
        LegalCitation(source.id, source.title, source.jurisdiction, source.language, source.content, source.url, state.savedSources.any { saved -> saved.sourceUrl == source.url })
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 850.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isWide) 48.dp else 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Eyebrow("CIVIC REFERENCE")
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Know your next step.",
                style = MaterialTheme.typography.headlineLarge,
                color = ext.ink,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Official civic regulations, service level benchmarks, and citizen redressal rights.",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted
            )

            state.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Query Panel
            PanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "What would you like to understand?",
                    style = MaterialTheme.typography.titleMedium,
                    color = ext.ink
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("e.g. How does the Haryana Auto Appeal System (AAS) handle overdue services?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = jurisExpanded,
                            onExpandedChange = { jurisExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = if (jurisdiction == "HARYANA") "Haryana + central guidance" else "Central guidance",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = jurisExpanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(4.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = jurisExpanded,
                                onDismissRequest = { jurisExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Haryana + central guidance") },
                                    onClick = {
                                        jurisdiction = "HARYANA"
                                        jurisExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Central guidance") },
                                    onClick = {
                                        jurisdiction = "CENTRAL"
                                        jurisExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    TheekHaiButton(
                        text = if (isSearching) "Searching..." else "Find guidance",
                        onClick = { if (state.user != null) viewModel.askGuidance(query, jurisdiction) else viewModel.searchGuidance(query, jurisdiction) },
                        enabled = !isSearching && query.trim().length >= 3
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Search approved civic references and check each source directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    fontSize = 11.sp
                )
            }

            // Results Section
            if (synthesizedAnswer != null || citations.isNotEmpty()) {
                val ans = synthesizedAnswer ?: "Approved source excerpts are listed below."
                Spacer(modifier = Modifier.height(24.dp))
                PanelCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = TheekHaiColors.GreenSurface,
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = "OFFICIAL GUIDANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = TheekHaiColors.Green,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text("Approved reference results", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = ans,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ext.ink,
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = state.guidanceAnswer?.limitation ?: "Open the source links to check the original guidance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Citations list
                Text("Official Source Citations", style = MaterialTheme.typography.titleMedium, color = ext.ink)
                Spacer(modifier = Modifier.height(12.dp))

                citations.forEach { cit ->
                    PanelCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = TheekHaiColors.StatusDefault,
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = "${cit.jurisdiction} · ${cit.language}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TheekHaiColors.StatusDefaultText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            TheekHaiButton(
                                text = if (cit.isSaved) "Saved ✓" else "Save to account",
                                onClick = { if (!cit.isSaved) viewModel.saveGuidance(CivicGuidanceSource(cit.id, cit.title, cit.url, cit.jurisdiction, cit.language, cit.snippet, 0L)) },
                                isSecondary = !cit.isSaved
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(cit.title, style = MaterialTheme.typography.titleSmall, color = ext.green, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(cit.snippet, style = MaterialTheme.typography.bodyMedium, color = ext.muted, lineHeight = 20.sp)
                    }
                }
            }

        }
    }
}
