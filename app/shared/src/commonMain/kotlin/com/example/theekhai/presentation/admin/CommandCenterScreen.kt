package com.example.theekhai.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.domain.model.AuditLog
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus

@Composable
fun CommandCenterRoute(
    viewModel: CommandCenterViewModel
) {
    val state by viewModel.uiState.collectAsState()
    CommandCenterScreen(
        state = state,
        onSelectComplaint = viewModel::selectComplaint,
        onFilterSelected = viewModel::filterBy,
        onTriggerTriage = viewModel::triggerAiTriage,
        onTriggerVerification = viewModel::triggerTinyFishVerification,
        onTriggerEscalation = { id -> viewModel.triggerEscalation(id, "SLA deadline breached without field proof") }
    )
}

@Composable
fun CommandCenterScreen(
    state: CommandCenterUiState,
    onSelectComplaint: (Complaint) -> Unit,
    onFilterSelected: (String) -> Unit,
    onTriggerTriage: (String) -> Unit,
    onTriggerVerification: (String) -> Unit,
    onTriggerEscalation: (String) -> Unit
) {
    val bgDark = Color(0xFF0F172A)
    val cardBg = Color(0xFF1E293B)
    val accentCyan = Color(0xFF06B6D4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "THEEKHAI • MUNICIPAL COMMAND CENTER",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Central Dispatch, SLA Monitoring & Verified Redressal Operations",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Realtime Sync Badge
            Surface(
                color = Color(0xFF064E3B),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF10B981)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Live Network Synced", color = Color(0xFF6EE7B7), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Metrics Banner
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard("Total Incidents", state.metrics.total.toString(), Color(0xFF38BDF8), Modifier.weight(1f))
            MetricCard("Awaiting Triage", state.metrics.pendingTriage.toString(), Color(0xFFFBBF24), Modifier.weight(1f))
            MetricCard("In Progress", state.metrics.inProgress.toString(), Color(0xFF818CF8), Modifier.weight(1f))
            MetricCard("Field Verified", state.metrics.verified.toString(), Color(0xFF34D399), Modifier.weight(1f))
            MetricCard("SLA Escalated", state.metrics.escalated.toString(), Color(0xFFF87171), Modifier.weight(1f))
        }

        // Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL", "PENDING", "TRIAGED", "RESOLVED", "VERIFIED", "ESCALATED").forEach { filter ->
                val selected = state.activeFilter == filter
                Button(
                    onClick = { onFilterSelected(filter) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) accentCyan else cardBg,
                        contentColor = if (selected) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(filter, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Action Status Toast
        if (state.actionMessage != null) {
            Surface(
                color = Color(0xFF334155),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = state.actionMessage,
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        // Dual Pane Workspace
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Pane: Complaints List
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "INCIDENT FEED (${state.filteredComplaints.size})",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.filteredComplaints) { complaint ->
                            val isSelected = complaint.id == state.selectedComplaint?.id
                            ComplaintItemRow(
                                complaint = complaint,
                                isSelected = isSelected,
                                onClick = { onSelectComplaint(complaint) }
                            )
                        }
                    }
                }
            }

            // Right Pane: Detail & AI Audit Inspector
            Card(
                modifier = Modifier.weight(1.4f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                val selected = state.selectedComplaint
                if (selected == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a complaint from the feed to view actions and explainable AI audit trail.", color = Color(0xFF64748B))
                    }
                } else {
                    DetailInspector(
                        complaint = selected,
                        auditLogs = state.auditLogs,
                        isActionInProgress = state.isActionInProgress,
                        onTriggerTriage = { onTriggerTriage(selected.id) },
                        onTriggerVerification = { onTriggerVerification(selected.id) },
                        onTriggerEscalation = { onTriggerEscalation(selected.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, count: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ComplaintItemRow(complaint: Complaint, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Color(0xFF06B6D4) else Color.Transparent
    val priorityColor = when (complaint.priority) {
        ComplaintPriority.CRITICAL -> Color(0xFFEF4444)
        ComplaintPriority.HIGH -> Color(0xFFF97316)
        ComplaintPriority.MEDIUM -> Color(0xFFF59E0B)
        ComplaintPriority.LOW -> Color(0xFF10B981)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp)),
        color = if (isSelected) Color(0xFF334155) else Color(0xFF0F172A),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${complaint.id.takeLast(6)}",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Surface(
                    color = priorityColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = complaint.priority.name,
                        color = priorityColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = complaint.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = complaint.category, color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(text = complaint.status.name, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailInspector(
    complaint: Complaint,
    auditLogs: List<AuditLog>,
    isActionInProgress: Boolean,
    onTriggerTriage: () -> Unit,
    onTriggerVerification: () -> Unit,
    onTriggerEscalation: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Complaint Metadata Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = complaint.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(text = "Zone: ${complaint.zone} • ${complaint.address}", fontSize = 12.sp, color = Color(0xFF94A3B8))
                }
                Surface(
                    color = Color(0xFF0369A1),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "STATUS: ${complaint.status}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        item {
            // Description & GPS
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Grievance Narrative", color = Color(0xFF64748B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = complaint.description, color = Color(0xFFE2E8F0), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "GPS Coordinates: ${complaint.latitude}, ${complaint.longitude} | Cluster: ${complaint.clusterId ?: "Unassigned"}",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        item {
            // Action Buttons
            Text(text = "AUTONOMOUS AGENT ORCHESTRATION", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onTriggerTriage,
                    enabled = !isActionInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🤖 AI Triage & Cluster (Gemini)", fontSize = 12.sp)
                }

                Button(
                    onClick = onTriggerVerification,
                    enabled = !isActionInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("🌐 Verify with official sources", fontSize = 12.sp)
                }

                Button(
                    onClick = onTriggerEscalation,
                    enabled = !isActionInProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("⚡ Escalate SLA", fontSize = 12.sp)
                }
            }
        }

        item {
            // Explainable AI Audit Trail
            Text(
                text = "EXPLAINABLE AI & DPDP 2026 AUDIT TRAIL",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(auditLogs) { log ->
            Surface(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.actorType.name,
                            color = Color(0xFFA78BFA),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = log.action,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reasoning: ${log.reasoning}",
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )

                    val payload = log.decisionPayloadJson
                    if (payload != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = payload,
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
