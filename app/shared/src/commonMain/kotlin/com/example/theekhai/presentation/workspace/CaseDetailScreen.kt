package com.example.theekhai.presentation.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.painterResource
import theekhai.app.shared.generated.resources.Res
import theekhai.app.shared.generated.resources.broken_streetlight
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.domain.model.ComplaintStatus
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors
import com.example.theekhai.presentation.nativeconnect.NativeConnectViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LiveCaseDetailScreen(caseId: String) {
    val viewModel = koinViewModel<NativeConnectViewModel>()
    val state by viewModel.state.collectAsState()
    val ext = LocalTheekHaiColors.current
    val navigator = LocalNavigator.current
    LaunchedEffect(caseId) { viewModel.loadCase(caseId) }
    val detail = state.caseDetail?.takeIf { it.complaint.id == caseId }

    if (detail == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (state.isLoading) "Loading report…" else state.error ?: "This report is not available to your account.", color = ext.ink)
            if (state.error != null) TheekHaiButton("Try again", onClick = { viewModel.loadCase(caseId) })
        }
        return
    }

    var note by remember(caseId) { mutableStateOf("") }
    var newTaskTitle by remember(caseId) { mutableStateOf("") }
    val userRole = state.user?.role ?: state.profile?.role ?: "CITIZEN"
    val isStaff = userRole in setOf("DEPARTMENT", "ADMIN")
    val isReviewer = userRole in setOf("REVIEWER", "ADMIN")
    val hasPendingChallenge = detail.events.any { it.action == "CHALLENGE" && it.at >= detail.complaint.updatedAtEpochMillis }

    fun formatEpoch(epoch: Long): String {
        if (epoch <= 0) return "Pending"
        val diffMs = System.currentTimeMillis() - epoch
        val diffMin = diffMs / 60_000
        val diffHours = diffMs / 3_600_000
        val diffDays = diffMs / 86_400_000
        return when {
            diffMs < 0 -> {
                val dueInHours = (-diffMs) / 3_600_000
                if (dueInHours > 24) "Due in ${dueInHours / 24}d" else "Due in ${dueInHours}h"
            }
            diffMin < 2 -> "Just now"
            diffMin < 60 -> "${diffMin}m ago"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays < 7 -> "${diffDays}d ago"
            else -> "${diffDays}d ago"
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = { navigator.navigateTo(Route.Workspace) }) { Text("← My workspace") }
        PanelCard(Modifier.fillMaxWidth()) {
            Eyebrow("${detail.complaint.category} · ${detail.complaint.id.take(8)}")
            Spacer(Modifier.height(8.dp))
            Text(detail.complaint.title, style = MaterialTheme.typography.headlineMedium, color = ext.ink, fontWeight = FontWeight.Bold)
            Text(detail.complaint.address, style = MaterialTheme.typography.bodyMedium, color = ext.muted)
            Spacer(Modifier.height(12.dp))
            StatusChip(detail.complaint.status.name.replace('_', ' '))
            Spacer(Modifier.height(12.dp))
            Text(detail.complaint.description, style = MaterialTheme.typography.bodyLarge, color = ext.ink)
            if (detail.complaint.latitude != 0.0 && detail.complaint.longitude != 0.0) {
                Spacer(Modifier.height(10.dp))
                Text("📍 Coordinates: ${detail.complaint.latitude}, ${detail.complaint.longitude}", style = MaterialTheme.typography.bodySmall, color = ext.muted)
            }
            Spacer(Modifier.height(6.dp))
            Text("⏱ Target Due Date: ${formatEpoch(detail.complaint.slaDueEpochMillis)}", style = MaterialTheme.typography.bodySmall, color = ext.muted)
        }

        PanelCard(Modifier.fillMaxWidth()) {
            Text("Work and evidence", style = MaterialTheme.typography.titleLarge, color = ext.ink)
            Spacer(Modifier.height(8.dp))
            if (detail.tasks.isEmpty()) Text("No work items have been assigned.", color = ext.muted)
            detail.tasks.forEach { task ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Checkbox(
                        checked = task.complete,
                        enabled = isStaff && !task.complete && detail.complaint.status.name == "IN_PROGRESS",
                        onCheckedChange = { if (!task.complete) viewModel.actOnCase(caseId, "COMPLETE_TASK", detail.complaint.version, "Task completed by assigned staff.", target = task.id) }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(task.title, color = ext.ink, style = MaterialTheme.typography.bodyMedium)
                        Text(task.department + if (task.required) " · Required" else "", color = ext.muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (isStaff && detail.complaint.status.name in setOf("ASSIGNED", "IN_PROGRESS", "REOPENED")) {
                Spacer(Modifier.height(12.dp))
                TheekHaiDivider()
                Spacer(Modifier.height(8.dp))
                Text("Add Required Task", style = MaterialTheme.typography.titleMedium, color = ext.ink)
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    label = { Text("Task description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TheekHaiButton(
                    "Add Task",
                    onClick = {
                        if (newTaskTitle.trim().length >= 3) {
                            viewModel.actOnCase(caseId, "ADD_TASK", detail.complaint.version, newTaskTitle.trim(), department = detail.complaint.zone)
                            newTaskTitle = ""
                        }
                    },
                    enabled = newTaskTitle.trim().length >= 3,
                    isSecondary = true
                )
            }
        }

        PanelCard(Modifier.fillMaxWidth()) {
            Text("Case history", style = MaterialTheme.typography.titleLarge, color = ext.ink)
            Spacer(Modifier.height(8.dp))
            if (detail.events.isEmpty()) Text("No history has been recorded yet.", color = ext.muted)
            detail.events.sortedByDescending { it.at }.forEach { event ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(event.action.replace('_', ' '), color = ext.green, style = MaterialTheme.typography.labelLarge)
                    Text(event.note, color = ext.ink, style = MaterialTheme.typography.bodyMedium)
                    Text(formatEpoch(event.at), color = ext.muted, style = MaterialTheme.typography.bodySmall)
                }
                TheekHaiDivider()
            }
        }

        PanelCard(Modifier.fillMaxWidth()) {
            Text("Next action", style = MaterialTheme.typography.titleLarge, color = ext.ink)
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Add a short reason") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.message?.let { Text(it, color = ext.green) }

            val actions = mutableListOf<Pair<String, String>>()
            if (isStaff && detail.complaint.status.name in setOf("ASSIGNED", "REOPENED")) {
                actions.add("START" to "Start work")
            }
            if (isStaff && detail.complaint.status.name == "IN_PROGRESS") {
                actions.add("REQUEST_CLOSURE" to "Request closure review")
            }
            if (isReviewer && detail.complaint.status.name == "PENDING_VERIFICATION") {
                actions.add("APPROVE_CLOSURE" to "Approve closure")
                actions.add("REJECT_CLOSURE" to "Return for more work")
            }
            if (detail.complaint.status.name == "CLOSED" && !hasPendingChallenge) {
                actions.add("CHALLENGE" to "Challenge closure")
            }
            if (isReviewer && detail.complaint.status.name == "CLOSED" && hasPendingChallenge) {
                actions.add("REOPEN" to "Uphold challenge & reopen")
            }

            actions.forEach { (action, label) ->
                Spacer(Modifier.height(8.dp))
                TheekHaiButton(
                    label,
                    onClick = { if (note.trim().length >= 3) { viewModel.actOnCase(caseId, action, detail.complaint.version, note.trim()); note = "" } },
                    enabled = !state.isLoading && note.trim().length >= 3,
                    modifier = Modifier.fillMaxWidth(),
                    isSecondary = action != actions.firstOrNull()?.first
                )
            }

            if (isStaff || isReviewer) {
                Spacer(Modifier.height(12.dp))
                TheekHaiButton(
                    "🌐 Verify Against Official Portal",
                    onClick = { viewModel.verifyCaseWithTinyFish(caseId) },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    isSecondary = true
                )
            }

            // ── Official Civic Email Dispatch (TinyFish contact discovery + Gmail API dispatch) ──
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ext.sidebarBg,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ext.line)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Official Civic Email Dispatch", style = MaterialTheme.typography.titleMedium, color = ext.ink, fontWeight = FontWeight.Bold)
                    Text("Verified Sender: arshitgupta34@gmail.com", style = MaterialTheme.typography.labelSmall, color = ext.muted)
                    Spacer(Modifier.height(8.dp))

                    val draft = state.emailDraft
                    if (draft == null || draft.caseId != caseId) {
                        TheekHaiButton(
                            "🤖 Discover Authority & Draft Email",
                            onClick = { viewModel.draftEmail(caseId) },
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            isSecondary = true
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = ext.paper,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ext.line)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text("To: ${draft.recipientEmail}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ext.ink)
                                Text("Authority: ${draft.recipientAuthority} (${draft.recipientDepartment})", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                                Text("Portal Source: ${draft.officialSourceUrl}", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                                Spacer(Modifier.height(6.dp))
                                Text("Subject: ${draft.subject}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = ext.ink)
                                Spacer(Modifier.height(4.dp))
                                Text(draft.body, style = MaterialTheme.typography.bodySmall, color = ext.muted, maxLines = 6)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        TheekHaiButton(
                            "🚀 Send Official Email via Gmail",
                            onClick = { viewModel.sendEmail(caseId, draft.recipientEmail, draft.subject, draft.body) },
                            enabled = !state.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun CaseDetailScreen(caseId: String) {
    LiveCaseDetailScreen(caseId = caseId)
}
