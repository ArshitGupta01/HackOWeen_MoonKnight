package com.example.theekhai.presentation.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
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
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors

@Composable
fun WorkspaceScreen(
    complaints: List<Complaint> = emptyList()
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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 850.dp
        val horizontalPadding = if (isWide) 48.dp else 16.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding, vertical = 20.dp)
            ) {
                // Page Heading
                Column(modifier = Modifier.padding(bottom = 18.dp)) {
                    Eyebrow("CITIZEN CASE TRACKING")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "My workspace",
                        style = if (isWide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ext.ink,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Track the departments, field workers and deadlines assigned to your reports.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.muted,
                    )
                }

                // Stats Row
                val totalCount = complaints.size
                val openCount = complaints.count { it.status != ComplaintStatus.VERIFIED && it.status != ComplaintStatus.RESOLVED }
                val resolvedCount = complaints.count { it.status == ComplaintStatus.RESOLVED || it.status == ComplaintStatus.VERIFIED }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    val cardPad = if (isWide) 20.dp else 12.dp
                    val numSize = if (isWide) 36.sp else 26.sp

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = ext.paper,
                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    ) {
                        Column(modifier = Modifier.padding(cardPad)) {
                            Text("TOTAL", style = MaterialTheme.typography.labelSmall, color = ext.muted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(totalCount.toString(), fontSize = numSize, fontWeight = FontWeight.Bold, color = ext.ink)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = ext.paper,
                        shape = RoundedCornerShape(0.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    ) {
                        Column(modifier = Modifier.padding(cardPad)) {
                            Text("OPEN", style = MaterialTheme.typography.labelSmall, color = ext.muted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(openCount.toString(), fontSize = numSize, fontWeight = FontWeight.Bold, color = Color(0xFF815D24))
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = ext.paper,
                        shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                    ) {
                        Column(modifier = Modifier.padding(cardPad)) {
                            Text("RESOLVED", style = MaterialTheme.typography.labelSmall, color = ext.muted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(resolvedCount.toString(), fontSize = numSize, fontWeight = FontWeight.Bold, color = Color(0xFF2F5F3E))
                        }
                    }
                }

                // Toolbar: Title & Search
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reports",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ext.ink,
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search reports...", fontSize = 12.sp, color = ext.muted) },
                            modifier = Modifier.widthIn(max = 280.dp),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ext.muted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ext.green,
                                unfocusedBorderColor = ext.inputBorder,
                                focusedContainerColor = ext.inputBg,
                                unfocusedContainerColor = ext.inputBg,
                            )
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                        Text(
                            text = "Reports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ext.ink,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by title, location or category...", fontSize = 12.sp, color = ext.muted) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ext.muted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ext.green,
                                unfocusedBorderColor = ext.inputBorder,
                                focusedContainerColor = ext.inputBg,
                                unfocusedContainerColor = ext.inputBg,
                            )
                        )
                    }
                }

                // Case list
                TheekHaiDivider()
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
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
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                color = ext.sidebarBg,
                                shape = RoundedCornerShape(6.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("No reports found", style = MaterialTheme.typography.titleMedium, color = ext.ink)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Submit a report to track its progress and timeline here.",
                                        fontSize = 12.sp,
                                        color = ext.muted,
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
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

            // Floating Action Button on Mobile
            if (!isWide) {
                FloatingActionButton(
                    onClick = { navigator.navigateTo(Route.Report) },
                    containerColor = ext.green,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Report issue")
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
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = complaint.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = ext.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${complaint.category} • ${complaint.address}",
                style = MaterialTheme.typography.bodySmall,
                color = ext.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatusChip(complaint.status.name, containerColor = statusBg, contentColor = statusColor)
            Text(
                text = complaint.zone,
                fontSize = 10.sp,
                color = ext.muted,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
        Text("›", fontSize = 20.sp, color = ext.muted)
    }
}
