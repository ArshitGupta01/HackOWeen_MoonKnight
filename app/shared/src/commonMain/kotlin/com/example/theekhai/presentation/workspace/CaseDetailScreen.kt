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
        return if (epoch > 0) "#$epoch" else "Pending"
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

data class CaseTask(
    val id: String,
    val title: String,
    val department: String,
    val isComplete: Boolean
)

data class CaseTimelineEvent(
    val action: String,
    val note: String,
    val timestamp: String
)

@Composable
fun CaseDetailScreen(caseId: String) {
    val navigator = LocalNavigator.current
    val ext = LocalTheekHaiColors.current

    // Sample data fallback matching web app
    var complaint by remember(caseId) {
        mutableStateOf(
            Complaint(
                id = caseId,
                citizenId = "citizen_001",
                title = if (caseId.contains("002")) "Drainage overflow near Community Center" else "Broken Streetlight on 5th Cross Road",
                description = "Streetlight pole #42 has been flickering and completely shut off for 4 days, causing safety concerns for night commuters and pedestrians.",
                category = if (caseId.contains("002")) "Sanitation" else "Electrical services",
                priority = ComplaintPriority.HIGH,
                status = ComplaintStatus.IN_PROGRESS,
                latitude = 28.4595,
                longitude = 77.0266,
                address = "Sector 14, Main Market, Ward 4",
                zone = "Zone 4 - Gurugram Central",
                photoUrl = null,
                assignedWorkerId = "PWD Electrical Crew #12",
                slaDueEpochMillis = 1718086400000L,
                createdAtEpochMillis = 1718000000000L,
                updatedAtEpochMillis = 1718000000000L
            )
        )
    }

    var tasks by remember {
        mutableStateOf(
            listOf(
                CaseTask("task-1", "Inspect line voltage & circuit breaker", "Electrical services", true),
                CaseTask("task-2", "Replace damaged LED fixture and secure wiring", "Electrical services", false),
                CaseTask("task-3", "Independent illumination audit", "Civic triage", false)
            )
        )
    }

    var timeline by remember {
        mutableStateOf(
            listOf(
                CaseTimelineEvent("COMPLAINT_FILED", "Sample timeline event — illustrative only.", "Yesterday 14:20"),
                CaseTimelineEvent("INTAKE_REVIEW", "Categorized and prioritized under civic standards.", "Yesterday 14:21"),
                CaseTimelineEvent("WORK_ORDER_ASSIGNED", "Dispatched to Sector 14 electrical maintenance crew.", "Yesterday 15:00"),
                CaseTimelineEvent("TASK_COMPLETED", "Task 'Inspect line voltage' completed by technician.", "Today 09:30")
            )
        )
    }

    var actionNote by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("Electrical services") }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 850.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isWide) 40.dp else 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Back navigation
            Row(
                modifier = Modifier
                    .clickable { navigator.navigateTo(Route.Workspace) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ext.green,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Back to workspace",
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.green
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Header Eyebrow & Title
            Eyebrow("${complaint.category} · ${complaint.id.take(8)}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = complaint.title,
                style = MaterialTheme.typography.headlineMedium,
                color = ext.ink,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = complaint.address,
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted
            )

            Spacer(modifier = Modifier.height(12.dp))
            Surface(color = ext.sidebarBg, shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = "Sample case preview. Details, timeline, and actions are illustrative and are not saved to the backend.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.ink,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (isWide) {
                // Wide layout: 2 columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Left Column (Summary, Tasks, Timeline)
                    Column(modifier = Modifier.weight(1.3f)) {
                        CaseSummaryPanel(complaint = complaint, ext = ext)
                        Spacer(modifier = Modifier.height(24.dp))
                        CoordinatedWorkPanel(
                            tasks = tasks,
                            onToggleTask = { taskId ->
                                tasks = tasks.map {
                                    if (it.id == taskId) it.copy(isComplete = !it.isComplete) else it
                                }
                            },
                            ext = ext
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        TimelinePanel(events = timeline, ext = ext)
                    }

                    // Right Column (Action panel)
                    Column(modifier = Modifier.weight(0.9f)) {
                        ActionPanel(
                            complaint = complaint,
                            actionNote = actionNote,
                            onActionNoteChange = { actionNote = it },
                            selectedDept = selectedDept,
                            onSelectedDeptChange = { selectedDept = it },
                            feedbackMsg = feedbackMsg,
                            onExecuteAction = { actionName ->
                                if (actionNote.trim().length < 3) {
                                    feedbackMsg = "Please enter an explanation note first."
                                    return@ActionPanel
                                }
                                feedbackMsg = "Action '$actionName' executed successfully."
                                timeline = listOf(
                                    CaseTimelineEvent(actionName, actionNote, "Just now")
                                ) + timeline
                                if (actionName == "APPROVE_CLOSURE") {
                                    complaint = complaint.copy(status = ComplaintStatus.VERIFIED)
                                } else if (actionName == "START_WORK") {
                                    complaint = complaint.copy(status = ComplaintStatus.IN_PROGRESS)
                                }
                                actionNote = ""
                            },
                            ext = ext
                        )
                    }
                }
            } else {
                // Narrow layout: Stacked
                Column(modifier = Modifier.fillMaxWidth()) {
                    CaseSummaryPanel(complaint = complaint, ext = ext)
                    Spacer(modifier = Modifier.height(20.dp))
                    CoordinatedWorkPanel(
                        tasks = tasks,
                        onToggleTask = { taskId ->
                            tasks = tasks.map {
                                if (it.id == taskId) it.copy(isComplete = !it.isComplete) else it
                            }
                        },
                        ext = ext
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    TimelinePanel(events = timeline, ext = ext)
                    Spacer(modifier = Modifier.height(20.dp))
                    ActionPanel(
                        complaint = complaint,
                        actionNote = actionNote,
                        onActionNoteChange = { actionNote = it },
                        selectedDept = selectedDept,
                        onSelectedDeptChange = { selectedDept = it },
                        feedbackMsg = feedbackMsg,
                        onExecuteAction = { actionName ->
                            if (actionNote.trim().length < 3) {
                                feedbackMsg = "Please enter an explanation note first."
                                return@ActionPanel
                            }
                            feedbackMsg = "Action '$actionName' executed successfully."
                            timeline = listOf(
                                CaseTimelineEvent(actionName, actionNote, "Just now")
                            ) + timeline
                            actionNote = ""
                        },
                        ext = ext
                    )
                }
            }
        }
    }
}

@Composable
private fun CaseSummaryPanel(complaint: Complaint, ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusChip(
                label = complaint.status.name.replace("_", " "),
                containerColor = when (complaint.status) {
                    ComplaintStatus.VERIFIED, ComplaintStatus.RESOLVED -> TheekHaiColors.StatusClosed
                    ComplaintStatus.IN_PROGRESS -> TheekHaiColors.StatusPending
                    else -> TheekHaiColors.StatusDefault
                },
                contentColor = when (complaint.status) {
                    ComplaintStatus.VERIFIED, ComplaintStatus.RESOLVED -> TheekHaiColors.StatusClosedText
                    ComplaintStatus.IN_PROGRESS -> TheekHaiColors.StatusPendingText
                    else -> TheekHaiColors.StatusDefaultText
                }
            )

            PriorityBadge(priority = complaint.priority.name)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = complaint.description,
            style = MaterialTheme.typography.bodyLarge,
            color = ext.ink,
            lineHeight = 24.sp
        )

        val isStreetlight = complaint.category.contains("LIGHT", ignoreCase = true) ||
                complaint.title.contains("streetlight", ignoreCase = true) ||
                complaint.id.contains("003")

        Spacer(modifier = Modifier.height(16.dp))
        if (isStreetlight) {
            Image(
                painter = painterResource(Res.drawable.broken_streetlight),
                contentDescription = "Broken Municipal Streetlight Evidence Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else if (complaint.photoUrl != null) {
            AsyncImage(
                model = complaint.photoUrl,
                contentDescription = "Case Evidence Photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE5E7EB))
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        TheekHaiDivider()
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Assigned to:", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                Text(complaint.assignedWorkerId ?: "Municipal Response Team", style = MaterialTheme.typography.titleSmall, color = ext.ink)
            }
            Column {
                Text("Service SLA:", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                Text("24h Standard", style = MaterialTheme.typography.titleSmall, color = TheekHaiColors.Amber)
            }
        }
    }
}

@Composable
private fun CoordinatedWorkPanel(
    tasks: List<CaseTask>,
    onToggleTask: (String) -> Unit,
    ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors
) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Text("Coordinated work", style = MaterialTheme.typography.titleMedium, color = ext.ink)
        Spacer(modifier = Modifier.height(12.dp))

        tasks.forEach { task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleTask(task.id) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (task.isComplete) TheekHaiColors.Green else Color.Transparent)
                        .border(
                            1.dp,
                            if (task.isComplete) TheekHaiColors.Green else ext.muted,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isComplete) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (task.isComplete) FontWeight.Normal else FontWeight.Medium,
                        color = if (task.isComplete) ext.muted else ext.ink
                    )
                    Text(
                        text = task.department,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted
                    )
                }

                Surface(
                    color = ext.sidebarBg,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = if (task.isComplete) "Completed" else "In Progress",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (task.isComplete) TheekHaiColors.StatusClosedText else ext.muted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            TheekHaiDivider()
        }
    }
}

@Composable
private fun TimelinePanel(
    events: List<CaseTimelineEvent>,
    ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors
) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Text("The complete story", style = MaterialTheme.typography.titleMedium, color = ext.ink)
        Spacer(modifier = Modifier.height(16.dp))

        events.forEachIndexed { idx, ev ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (idx == 0) TheekHaiColors.Amber else ext.muted)
                    )
                    if (idx < events.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(40.dp)
                                .background(ext.line)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ev.action.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.muted
                        )
                        Text(
                            text = ev.timestamp,
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.muted,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ev.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.ink
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionPanel(
    complaint: Complaint,
    actionNote: String,
    onActionNoteChange: (String) -> Unit,
    selectedDept: String,
    onSelectedDeptChange: (String) -> Unit,
    feedbackMsg: String?,
    onExecuteAction: (String) -> Unit,
    ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors
) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Eyebrow("NEXT ACTION")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Move it forward.", style = MaterialTheme.typography.headlineSmall, color = ext.ink)
        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = actionNote,
            onValueChange = onActionNoteChange,
            placeholder = { Text("Explain your action or provide review notes...") },
            modifier = Modifier.fillMaxWidth().height(110.dp),
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ext.inputBg,
                unfocusedContainerColor = ext.inputBg,
                focusedBorderColor = ext.green,
                unfocusedBorderColor = ext.line
            )
        )

        feedbackMsg?.let { msg ->
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = Color(0xFFF9EDE1),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg,
                    color = Color(0xFF8A3D24),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Role-based contextual actions
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TheekHaiButton(
                text = "Start work & dispatch crew",
                onClick = { onExecuteAction("START_WORK") },
                modifier = Modifier.fillMaxWidth()
            )

            TheekHaiButton(
                text = "Request closure review",
                onClick = { onExecuteAction("REQUEST_CLOSURE") },
                modifier = Modifier.fillMaxWidth(),
                isSecondary = true
            )

            TheekHaiButton(
                text = "Approve closure (Independent review)",
                onClick = { onExecuteAction("APPROVE_CLOSURE") },
                modifier = Modifier.fillMaxWidth(),
                isSecondary = true
            )

            TheekHaiButton(
                text = "Challenge this resolution",
                onClick = { onExecuteAction("CHALLENGE_CLOSURE") },
                modifier = Modifier.fillMaxWidth(),
                isSecondary = true
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Every decision is recorded in the audit trail. Closure requires independent review.",
            style = MaterialTheme.typography.bodySmall,
            color = ext.muted,
            fontSize = 11.sp
        )
    }
}
