package com.example.theekhai.presentation.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.theekhai.ui.theme.TheekHaiColors

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

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 850.dp
        val horizontalPadding = if (isWide) 48.dp else 16.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = horizontalPadding, vertical = 20.dp)
            ) {
                // Page Heading
                Column(modifier = Modifier.padding(bottom = 20.dp)) {
                    Eyebrow("CITIZEN GRIEVANCE INTAKE")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Report an issue",
                        style = if (isWide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ext.ink,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "A few details help the right municipal team understand and fix the problem.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.muted,
                    )
                }

                if (isWide) {
                    // ── Wide Desktop Layout: 2 Columns ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(40.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            ReportForm(
                                title = title,
                                onTitleChange = { title = it },
                                selectedCategory = selectedCategory,
                                onCategoryChange = { selectedCategory = it },
                                categories = categories,
                                description = description,
                                onDescriptionChange = { description = it },
                                isRecording = isRecording,
                                hasRecording = hasRecording,
                                onToggleRecording = {
                                    isRecording = !isRecording
                                    if (!isRecording) hasRecording = true
                                },
                                address = address,
                                onAddressChange = { address = it },
                                consentGranted = consentGranted,
                                onConsentChange = { consentGranted = it },
                                statusMessage = statusMessage,
                                isError = isError,
                                isSubmitting = isSubmitting,
                                onSubmit = {
                                    if (!consentGranted) {
                                        statusMessage = "Please consent before submitting."
                                        isError = true
                                        return@ReportForm
                                    }
                                    isSubmitting = true
                                    statusMessage = "Submitting report for AI triage..."
                                    isError = false
                                    navigator.navigateTo(Route.Workspace)
                                },
                                onSaveDraft = {
                                    statusMessage = "Draft saved on this device."
                                    isError = false
                                }
                            )
                        }

                        Column(modifier = Modifier.weight(1f).padding(top = 10.dp)) {
                            WhatHappensNextCard()
                        }
                    }
                } else {
                    // ── Compact Mobile Layout: 1 Column Full Width ──
                    ReportForm(
                        title = title,
                        onTitleChange = { title = it },
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        categories = categories,
                        description = description,
                        onDescriptionChange = { description = it },
                        isRecording = isRecording,
                        hasRecording = hasRecording,
                        onToggleRecording = {
                            isRecording = !isRecording
                            if (!isRecording) hasRecording = true
                        },
                        address = address,
                        onAddressChange = { address = it },
                        consentGranted = consentGranted,
                        onConsentChange = { consentGranted = it },
                        statusMessage = statusMessage,
                        isError = isError,
                        isSubmitting = isSubmitting,
                        onSubmit = {
                            if (!consentGranted) {
                                statusMessage = "Please consent before submitting."
                                isError = true
                                return@ReportForm
                            }
                            isSubmitting = true
                            statusMessage = "Submitting report for AI triage..."
                            isError = false
                            navigator.navigateTo(Route.Workspace)
                        },
                        onSaveDraft = {
                            statusMessage = "Draft saved on this device."
                            isError = false
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    WhatHappensNextCard()
                    Spacer(modifier = Modifier.height(80.dp)) // padding for floating button
                }
            }

            // Mobile Floating Action Button to Submit
            if (!isWide && title.isNotBlank() && description.isNotBlank()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!consentGranted) {
                            statusMessage = "Please check the consent box below."
                            isError = true
                            return@ExtendedFloatingActionButton
                        }
                        isSubmitting = true
                        statusMessage = "Submitting report..."
                        isError = false
                        navigator.navigateTo(Route.Workspace)
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Submit") },
                    text = { Text("Submit Report", fontWeight = FontWeight.Bold) },
                    containerColor = ext.green,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportForm(
    title: String,
    onTitleChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    categories: List<Pair<String, String>>,
    description: String,
    onDescriptionChange: (String) -> Unit,
    isRecording: Boolean,
    hasRecording: Boolean,
    onToggleRecording: () -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    consentGranted: Boolean,
    onConsentChange: (Boolean) -> Unit,
    statusMessage: String?,
    isError: Boolean,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    onSaveDraft: () -> Unit,
) {
    val ext = LocalTheekHaiColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ext.paper,
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title
            Text("Issue title", style = MaterialTheme.typography.labelMedium, color = ext.ink, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text("e.g. Broken streetlight near park entrance", fontSize = 13.sp, color = ext.muted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                colors = reportFieldColors(ext)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category dropdown
            Text("Type of issue", style = MaterialTheme.typography.labelMedium, color = ext.ink, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = categories.find { it.first == selectedCategory }?.second ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(6.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    colors = reportFieldColors(ext)
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label, fontSize = 14.sp) },
                            onClick = {
                                onCategoryChange(value)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text("What is happening?", style = MaterialTheme.typography.labelMedium, color = ext.ink, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = { Text("Describe the condition and how it affects the area...", fontSize = 13.sp, color = ext.muted) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                shape = RoundedCornerShape(6.dp),
                maxLines = 6,
                colors = reportFieldColors(ext)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Voice Recording Bar
            Surface(
                color = ext.sidebarBg,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledIconButton(
                            onClick = onToggleRecording,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isRecording) Color(0xFFEF4444) else ext.green
                            )
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Check else Icons.Default.Mic,
                                contentDescription = "Record",
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = if (isRecording) "Recording voice note..." else if (hasRecording) "Voice note recorded" else "Record voice grievance",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ext.ink
                            )
                            Text(
                                text = "Supports 12+ Indian languages",
                                fontSize = 10.sp,
                                color = ext.muted
                            )
                        }
                    }

                    if (hasRecording) {
                        Surface(
                            color = TheekHaiColors.GreenSurface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "✓ Attached",
                                fontSize = 10.sp,
                                color = ext.green,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Address / Landmark
            Text("Address or landmark", style = MaterialTheme.typography.labelMedium, color = ext.ink, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = address,
                onValueChange = onAddressChange,
                placeholder = { Text("e.g. 5th Cross Road near Community Park", fontSize = 13.sp, color = ext.muted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { onAddressChange("Ward 4, MG Road Crossing") }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Use location", tint = ext.green)
                    }
                },
                colors = reportFieldColors(ext)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Consent Checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { onConsentChange(!consentGranted) }
            ) {
                Checkbox(
                    checked = consentGranted,
                    onCheckedChange = onConsentChange,
                    colors = CheckboxDefaults.colors(checkedColor = ext.green)
                )
                Text(
                    text = "I consent to sharing this report with municipal teams.",
                    fontSize = 12.sp,
                    color = ext.ink,
                )
            }

            // Status message
            AnimatedVisibility(visible = statusMessage != null) {
                Surface(
                    color = if (isError) Color(0xFFF9EDE1) else Color(0xFFE7EEE1),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text(
                        text = statusMessage ?: "",
                        color = if (isError) Color(0xFF8A3D24) else ext.ink,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TheekHaiButton(
                    text = if (isSubmitting) "Submitting..." else "Submit report ↗",
                    onClick = onSubmit,
                    enabled = title.isNotBlank() && description.isNotBlank() && !isSubmitting,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = onSaveDraft,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Save draft", fontSize = 12.sp, color = ext.ink)
                }
            }
        }
    }
}

@Composable
private fun WhatHappensNextCard() {
    val ext = LocalTheekHaiColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ext.sidebarBg,
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Eyebrow("WHAT HAPPENS NEXT")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Accountable resolution.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ext.ink,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your grievance is assigned to the responsible department with statutory deadlines under public service guarantee laws. Track every status change in your workspace.",
                style = MaterialTheme.typography.bodySmall,
                color = ext.muted,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Civic pilot • Not for active life-safety emergencies",
                fontSize = 11.sp,
                color = TheekHaiColors.Amber,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun reportFieldColors(ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ext.green,
        unfocusedBorderColor = Color(0xFFCDD4C9),
        focusedContainerColor = Color(0xFFFFFEF9),
        unfocusedContainerColor = Color(0xFFFFFEF9),
        focusedTextColor = ext.ink,
        unfocusedTextColor = ext.ink,
    )
