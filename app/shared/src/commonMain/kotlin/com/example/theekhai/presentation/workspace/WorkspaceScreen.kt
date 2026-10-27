package com.example.theekhai.presentation.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors

@Composable
fun WorkspaceScreen(
    complaints: List<Complaint> = sampleComplaints()
) {
    val navigator = LocalNavigator.current
    val ext = LocalTheekHaiColors.current

    var searchQuery by remember { mutableStateOf("") }
    val filteredComplaints = remember(complaints, searchQuery) {
        if (searchQuery.isBlank()) complaints
        else complaints.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.address.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        // Page heading
        Column(modifier = Modifier.padding(bottom = 30.dp)) {
            Eyebrow("YOUR REPORTS")
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "My workspace.",
                style = MaterialTheme.typography.headlineLarge,
                color = ext.ink,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Follow the departments, tasks and deadlines connected to your reports.",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted,
            )
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            val totalCount = complaints.size
            val openCount = complaints.count { it.status != ComplaintStatus.VERIFIED && it.status != ComplaintStatus.RESOLVED }
            val resolvedCount = complaints.count { it.status == ComplaintStatus.RESOLVED || it.status == ComplaintStatus.VERIFIED }

            Surface(
                modifier = Modifier.weight(1f),
                color = ext.paper,
                shape = RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            ) {
                Column(modifier = Modifier.padding(25.dp)) {
                    Text("TOTAL", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp), color = ext.muted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(totalCount.toString(), fontSize = 38.sp, fontWeight = FontWeight.Normal, color = ext.ink)
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                color = ext.paper,
                shape = RoundedCornerShape(0.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            ) {
                Column(modifier = Modifier.padding(25.dp)) {
                    Text("OPEN", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp), color = ext.muted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(openCount.toString(), fontSize = 38.sp, fontWeight = FontWeight.Normal, color = Color(0xFF815D24))
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                color = ext.paper,
                shape = RoundedCornerShape(topEnd = 5.dp, bottomEnd = 5.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            ) {
                Column(modifier = Modifier.padding(25.dp)) {
                    Text("RESOLVED", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp), color = ext.muted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(resolvedCount.toString(), fontSize = 38.sp, fontWeight = FontWeight.Normal, color = Color(0xFF2F5F3E))
                }
            }
        }

        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reports",
                style = MaterialTheme.typography.headlineMedium,
                color = ext.ink,
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search reports...", fontSize = 12.sp, color = Color(0xFF839083)) },
                modifier = Modifier.widthIn(max = 280.dp),
                shape = RoundedCornerShape(3.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFCDD4C9),
                    unfocusedBorderColor = Color(0xFFCDD4C9),
                    focusedContainerColor = Color(0xFFFFFEF9),
                    unfocusedContainerColor = Color(0xFFFFFEF9),
                )
            )
        }

        // Case list
        TheekHaiDivider()
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(filteredComplaints) { complaint ->
                CaseRow(
                    complaint = complaint,
                    onClick = { navigator.navigateTo(Route.CaseDetail(complaint.id)) }
                )
                TheekHaiDivider()
            }

            if (filteredComplaints.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        color = Color(0xFFEFF0E7),
                        shape = RoundedCornerShape(5.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    ) {
                        Column(
                            modifier = Modifier.padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No reports yet", style = MaterialTheme.typography.titleMedium, color = ext.ink)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Submit a report to see it tracked here with tasks, deadlines, and AI audit trails.",
                                fontSize = 13.sp,
                                color = ext.muted,
                            )
                            Spacer(modifier = Modifier.height(25.dp))
                            TheekHaiButton(
                                text = "Report an issue  →",
                                onClick = { navigator.navigateTo(Route.Report) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaseRow(complaint: Complaint, onClick: () -> Unit) {
    val ext = LocalTheekHaiColors.current
    val statusColor = when (complaint.status) {
        ComplaintStatus.PENDING, ComplaintStatus.REOPENED -> TheekHaiColors.StatusPendingText
        ComplaintStatus.VERIFIED, ComplaintStatus.RESOLVED -> TheekHaiColors.StatusClosedText
        else -> TheekHaiColors.StatusDefaultText
    }
    val statusBg = when (complaint.status) {
        ComplaintStatus.PENDING, ComplaintStatus.REOPENED -> TheekHaiColors.StatusPending
        ComplaintStatus.VERIFIED, ComplaintStatus.RESOLVED -> TheekHaiColors.StatusClosed
        else -> TheekHaiColors.StatusDefault
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 23.dp, horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = complaint.title,
                style = MaterialTheme.typography.titleSmall,
                color = ext.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${complaint.category} · ${complaint.address}",
                style = MaterialTheme.typography.bodySmall,
                color = ext.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            StatusChip(complaint.status.name, containerColor = statusBg, contentColor = statusColor)
            Text(
                text = "Zone: ${complaint.zone}",
                fontSize = 11.sp,
                color = ext.muted,
            )
        }

        Spacer(modifier = Modifier.width(20.dp))
        Text("›", fontSize = 22.sp, color = ext.muted)
    }
}

/** Sample complaints for dev/demo rendering */
private fun sampleComplaints(): List<Complaint> = listOf(
    Complaint(
        id = "c-001-abc", title = "Broken streetlight near Central Park",
        description = "The streetlight at the junction has been non-functional for 2 weeks.",
        category = "LIGHTING", priority = ComplaintPriority.HIGH, status = ComplaintStatus.TRIAGED,
        latitude = 28.6139, longitude = 77.2090, address = "MG Road Junction",
        citizenId = "citizen-1", zone = "Zone-4", slaDueEpochMillis = 0L,
        createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L
    ),
    Complaint(
        id = "c-002-def", title = "Overflowing drain on Nehru Lane",
        description = "Sewage water overflowing onto the road since morning.",
        category = "WATER", priority = ComplaintPriority.CRITICAL, status = ComplaintStatus.PENDING,
        latitude = 28.6150, longitude = 77.2100, address = "Nehru Lane, Block B",
        citizenId = "citizen-1", zone = "Zone-2", slaDueEpochMillis = 0L,
        createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L
    ),
    Complaint(
        id = "c-003-ghi", title = "Pothole causing accidents on Ring Road",
        description = "Large pothole near the flyover exit. Two-wheelers at risk.",
        category = "ROADS", priority = ComplaintPriority.HIGH, status = ComplaintStatus.RESOLVED,
        latitude = 28.6200, longitude = 77.2200, address = "Ring Road, Flyover Exit 3",
        citizenId = "citizen-1", zone = "Zone-1", slaDueEpochMillis = 0L,
        createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L
    ),
    Complaint(
        id = "c-004-jkl", title = "Garbage not collected in Sector 5",
        description = "The garbage truck has not visited for the past 3 days.",
        category = "WASTE", priority = ComplaintPriority.MEDIUM, status = ComplaintStatus.VERIFIED,
        latitude = 28.6300, longitude = 77.2300, address = "Sector 5, Main Gate",
        citizenId = "citizen-1", zone = "Zone-3", slaDueEpochMillis = 0L,
        createdAtEpochMillis = 0L, updatedAtEpochMillis = 0L
    ),
)
