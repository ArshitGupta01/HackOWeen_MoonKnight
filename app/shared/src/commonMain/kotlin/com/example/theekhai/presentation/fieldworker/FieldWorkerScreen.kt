package com.example.theekhai.presentation.fieldworker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val bgDark = Color(0xFF0F172A)
    val cardBg = Color(0xFF1E293B)
    var resolutionNotes by remember { mutableStateOf("Repaired surface asphalt and cleared debris.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgDark)
            .padding(16.dp)
    ) {
        // Top Bar: Worker Status & Offline Sync
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FIELD WORKER PORTAL",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "PWD Ward 14 • Satish Kumar (ID: #4092)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Button(
                onClick = onSyncOffline,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("🔄 Sync Local Queue", fontSize = 12.sp)
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
                    fontSize = 13.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Text(
                            text = "MULTILINGUAL VOICE GRIEVANCE INTAKE",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Record in Hindi, Telugu, Tamil, Marathi or English. Powered by Sarvam & Groq Whisper.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mic Button
                        val micBg = if (state.isRecording) Color(0xFFDC2626) else Color(0xFF4F46E5)
                        Surface(
                            modifier = Modifier
                                .size(72.dp)
                                .clickable { onToggleRecording() },
                            shape = CircleShape,
                            color = micBg
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (state.isRecording) "⏹ STOP" else "🎙 REC",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // DPDP Act 2026 explicit consent toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Checkbox(
                                checked = state.dpdpConsentAccepted,
                                onCheckedChange = onConsentChanged,
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                            )
                            Text(
                                text = "Citizen explicitly consents to voice recording processing under DPDP Act 2026.",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }

                        if (state.recordedAudioBase64 != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onSubmitVoiceGrievance,
                                enabled = state.dpdpConsentAccepted && !state.isSubmitting,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("🚀 Submit Grievance for AI Triage", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(state.assignedComplaints) { complaint ->
                val isSelected = complaint.id == state.selectedComplaint?.id
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectComplaint(complaint) }
                        .border(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155), RoundedCornerShape(10.dp)),
                    color = cardBg,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "#${complaint.id.takeLast(6)}", color = Color(0xFF64748B), fontSize = 11.sp)
                            Text(
                                text = complaint.priority.name,
                                color = if (complaint.priority == ComplaintPriority.CRITICAL) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = complaint.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Location: ${complaint.address}", color = Color(0xFF94A3B8), fontSize = 12.sp)

                        if (isSelected) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFF334155))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(text = "Field Resolution Evidence", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = resolutionNotes,
                                onValueChange = { resolutionNotes = it },
                                label = { Text("Resolution Notes") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF64748B)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onResolveComplaint(complaint.id, resolutionNotes) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("📸 Verify GPS & Mark Resolved", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
