package com.example.theekhai.presentation.fieldworker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.domain.model.ComplaintPriority
import com.example.theekhai.ui.components.Eyebrow
import com.example.theekhai.ui.components.PriorityBadge
import com.example.theekhai.ui.components.StatusChip
import com.example.theekhai.ui.components.TheekHaiButton
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors

@Composable
fun FieldWorkerRoute(
    viewModel: FieldWorkerViewModel
) {
    val state by viewModel.uiState.collectAsState()
    FieldWorkerScreen(
        state = state,
        onToggleRecording = viewModel::toggleRecording,
        onConsentChanged = viewModel::toggleDpdpConsent,
        onSubmitVoiceGrievance = { viewModel.submitVoiceGrievance() },
        onSelectComplaint = viewModel::selectComplaint,
        onResolveComplaint = { id, notes -> viewModel.resolveWorkOrderWithEvidence(id, notes) },
        onSyncOffline = viewModel::syncOfflineQueue
    )
}

@Composable
fun FieldWorkerScreen(
    state: FieldWorkerUiState,
    onToggleRecording: () -> Unit,
    onConsentChanged: (Boolean) -> Unit,
    onSubmitVoiceGrievance: () -> Unit,
    onSelectComplaint: (Complaint) -> Unit,
    onResolveComplaint: (String, String) -> Unit,
    onSyncOffline: () -> Unit
) {
    val ext = LocalTheekHaiColors.current
    val bgDark = Color(0xFF0F172A)
    val cardBg = Color(0xFF1E293B)
    var resolutionNotes by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header: Worker Status & Sync Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Eyebrow("MUNICIPAL FIELD OPS", modifier = Modifier.padding(bottom = 2.dp))
                    Text(
                        text = "Field Worker Portal",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Field worker preview · no authenticated worker",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                FilledTonalButton(
                    onClick = onSyncOffline,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF0369A1),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Status Banner
            if (state.syncStatusMessage != null || state.evidenceVerificationStatus != null) {
                Surface(
                    color = Color(0xFF334155),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        text = state.evidenceVerificationStatus ?: state.syncStatusMessage ?: "",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Card 1: Multilingual Voice Intake with DPDP consent
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VOICE GRIEVANCE INTAKE",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "12+ Languages",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Mic Button
                            val micBg = if (state.isRecording) Color(0xFFDC2626) else Color(0xFF4F46E5)
                            Surface(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clickable { onToggleRecording() },
                                shape = CircleShape,
                                color = micBg,
                                shadowElevation = 6.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (state.isRecording) Icons.Default.Check else Icons.Default.Mic,
                                        contentDescription = "Record",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (state.isRecording) "Recording... Tap to stop" else "Tap mic to record citizen voice note",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // DPDP Consent Checkbox
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onConsentChanged(!state.dpdpConsentAccepted) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Checkbox(
                                    checked = state.dpdpConsentAccepted,
                                    onCheckedChange = onConsentChanged,
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                                )
                                Text(
                                    text = "Consent obtained for voice processing (DPDP Act)",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }

                            if (state.recordedAudioBase64 != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onSubmitVoiceGrievance,
                                    enabled = state.dpdpConsentAccepted && !state.isSubmitting,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (state.isSubmitting) "Submitting..." else "Submit for AI Triage ↗",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: Assigned Work Orders
                item {
                    Text(
                        text = "ASSIGNED WORK ORDERS (${state.assignedComplaints.size})",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                items(state.assignedComplaints) { complaint ->
                    val isSelected = state.selectedComplaint?.id == complaint.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectComplaint(complaint) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF1E3A5F) else cardBg
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isSelected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${complaint.id}",
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp
                                )
                                PriorityBadge(complaint.priority.name)
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = complaint.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "📍 ${complaint.address}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Resolution Notes:",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = resolutionNotes,
                                    onValueChange = { resolutionNotes = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A)
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = { onResolveComplaint(complaint.id, resolutionNotes) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("✓ Mark Resolved with Evidence", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
