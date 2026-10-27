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
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors

data class LegalCitation(
    val id: String,
    val title: String,
    val jurisdiction: String,
    val language: String,
    val snippet: String,
    val url: String,
    var isSaved: Boolean = false
)

data class TinyFishMonitorItem(
    val id: String,
    val name: String,
    val type: String,
    val target: String,
    val status: String
)

data class PendingReviewSource(
    val id: String,
    val title: String,
    val jurisdiction: String,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen() {
    val ext = LocalTheekHaiColors.current

    var query by remember { mutableStateOf("") }
    var jurisdiction by remember { mutableStateOf("HARYANA") }
    var jurisExpanded by remember { mutableStateOf(false) }

    var isSearching by remember { mutableStateOf(false) }
    var answerMode by remember { mutableStateOf<String?>(null) }
    var synthesizedAnswer by remember { mutableStateOf<String?>(null) }

    var citations by remember {
        mutableStateOf<List<LegalCitation>>(emptyList())
    }

    var activeMonitors by remember {
        mutableStateOf(
            listOf(
                TinyFishMonitorItem("mon-1", "Haryana RTSC Service Notification Watcher", "FETCH", "https://haryana-rtsc.gov.in/notifications", "HEALTHY (Checked 1h ago)"),
                TinyFishMonitorItem("mon-2", "AAS Auto Appeal Escalation SOP Monitor", "SEARCH", "Auto Appeal System SLA Haryana Municipal", "ACTIVE (Hourly)")
            )
        )
    }

    var pendingSources by remember {
        mutableStateOf(
            listOf(
                PendingReviewSource("rev-1", "Notification No. 12/2026: Municipal Solid Waste Grievance Timelines", "HARYANA", "https://ulbharyana.gov.in/rules-2026.pdf"),
                PendingReviewSource("rev-2", "MoHUA Urban Drainage Maintenance Standard Benchmark v3.2", "CENTRAL", "https://mohua.gov.in/drainage-benchmark.pdf")
            )
        )
    }

    var discoveryQuery by remember { mutableStateOf("") }
    var discoveryFeedback by remember { mutableStateOf<String?>(null) }

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
                        onClick = {
                            isSearching = true
                            answerMode = "TINYFISH_AGENT"
                            synthesizedAnswer = "Under the Haryana Right to Service Act, if a notified service (such as streetlight repair or drain clearance) is not resolved within the designated SLA (typically 48 hours for municipal electrical failures), the ticket automatically elevates via AAS (Auto Appeal System) to the First Grievance Redressal Authority without requiring citizen re-filing. The authority has 30 days to order corrective action or summon the designated officer."
                            citations = listOf(
                                LegalCitation(
                                    id = "cit-1",
                                    title = "Haryana Right to Service Commission – Auto Appeal System (AAS) Standard Operating Procedure",
                                    jurisdiction = "HARYANA",
                                    language = "English",
                                    snippet = "Section 7(1): In case of non-delivery of service within the timeline prescribed under the Schedule, an appeal shall be automatically triggered to the First Appellate Authority. Designated officers failing without sufficient cause face statutory penalties under Section 17.",
                                    url = "https://haryana-rtsc.gov.in/aas-sop"
                                ),
                                LegalCitation(
                                    id = "cit-2",
                                    title = "The Right to Information & Public Service Guarantee Rules, 2014",
                                    jurisdiction = "CENTRAL",
                                    language = "English",
                                    snippet = "Citizen service charters require municipal bodies to publish designated officers, time limits, and first appeal authorities for all notified municipal civic services.",
                                    url = "https://darpg.gov.in"
                                )
                            )
                            isSearching = false
                        },
                        enabled = !isSearching
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Searches verified municipal service charters, Right to Service notifications, and statutory appeal procedures.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    fontSize = 11.sp
                )
            }

            // Results Section
            synthesizedAnswer?.let { ans ->
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

                        Text("Verified Gazette Reference", style = MaterialTheme.typography.bodySmall, color = ext.muted)
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
                        text = "Pilot service reference. Synthesized from approved gazette and service commission documentation.",
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
                                onClick = {
                                    citations = citations.map {
                                        if (it.id == cit.id) it.copy(isSaved = !it.isSaved) else it
                                    }
                                },
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

            Spacer(modifier = Modifier.height(36.dp))

            // TinyFish Ops, Discovery & Monitors Section (For reviewer/admin)
            PanelCard(modifier = Modifier.fillMaxWidth()) {
                Eyebrow("TINYFISH AGENT & MONITOR + TAVILY")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Source discovery, monitors & review queue", style = MaterialTheme.typography.headlineSmall, color = ext.ink)
                Spacer(modifier = Modifier.height(16.dp))

                // Run Discovery Form
                Text("Autonomous Source Discovery", style = MaterialTheme.typography.titleSmall, color = ext.ink)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = discoveryQuery,
                        onValueChange = { discoveryQuery = it },
                        placeholder = { Text("Tavily query or TinyFish target goal (e.g. Haryana RTSC guidelines)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    TheekHaiButton(
                        text = "Run Discovery",
                        onClick = {
                            discoveryFeedback = "TinyFish autonomous agent queued 2 official documents for verification."
                        },
                        isSecondary = true
                    )
                }

                discoveryFeedback?.let { fb ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(fb, color = TheekHaiColors.Green, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(24.dp))
                TheekHaiDivider()
                Spacer(modifier = Modifier.height(20.dp))

                // Active Monitors
                Text("Active TinyFish Monitors", style = MaterialTheme.typography.titleSmall, color = ext.ink)
                Spacer(modifier = Modifier.height(10.dp))

                activeMonitors.forEach { mon ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = TheekHaiColors.GreenSurface, shape = RoundedCornerShape(3.dp)) {
                                    Text(mon.type, style = MaterialTheme.typography.labelSmall, color = TheekHaiColors.Green, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Text(mon.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = ext.ink)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(mon.status, style = MaterialTheme.typography.bodySmall, color = ext.muted)
                        }

                        TheekHaiButton(
                            text = "Run check",
                            onClick = {},
                            isSecondary = true
                        )
                    }
                    TheekHaiDivider()
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Pending Review Queue
                Text("Pending Source Review Queue", style = MaterialTheme.typography.titleSmall, color = ext.ink)
                Spacer(modifier = Modifier.height(10.dp))

                if (pendingSources.isEmpty()) {
                    Text("No pending sources in review queue.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                } else {
                    pendingSources.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(color = TheekHaiColors.StatusDefault, shape = RoundedCornerShape(3.dp)) {
                                    Text(p.jurisdiction, style = MaterialTheme.typography.labelSmall, color = TheekHaiColors.StatusDefaultText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(p.title, style = MaterialTheme.typography.bodyMedium, color = ext.ink)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TheekHaiButton(
                                    text = "Approve",
                                    onClick = {
                                        pendingSources = pendingSources.filter { it.id != p.id }
                                    }
                                )
                                TheekHaiButton(
                                    text = "Reject",
                                    onClick = {
                                        pendingSources = pendingSources.filter { it.id != p.id }
                                    },
                                    isSecondary = true
                                )
                            }
                        }
                        TheekHaiDivider()
                    }
                }
            }
        }
    }
}
