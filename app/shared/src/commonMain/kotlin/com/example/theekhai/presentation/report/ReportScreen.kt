package com.example.theekhai.presentation.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.platform.rememberLocationFetcher
import com.example.theekhai.platform.rememberVoiceRecorder
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.localization.t
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors
import com.example.theekhai.presentation.nativeconnect.NativeConnectViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen() {
    val connectViewModel = koinViewModel<NativeConnectViewModel>()
    val connectState by connectViewModel.state.collectAsState()
    val ext = LocalTheekHaiColors.current
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ROADS") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var wardLabel by remember { mutableStateOf("Ward 4") }
    var showMapPicker by remember { mutableStateOf(false) }

    var consentGranted by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var hasRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var voiceProviderLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(connectState.location) {
        connectState.location?.let { resolved -> address = resolved.address; wardLabel = resolved.ward }
    }
    LaunchedEffect(connectState.triage) {
        connectState.triage?.let { suggestion ->
            if (title.isBlank() && suggestion.title.isNotBlank()) title = suggestion.title
            if (suggestion.category in setOf("ROADS", "WATER", "WASTE", "LIGHTING", "OTHER")) selectedCategory = suggestion.category
            statusMessage = suggestion.summary + if (suggestion.similarCaseIds.isNotEmpty()) " Similar reports: ${suggestion.similarCaseIds.size}." else ""
            isError = false
        }
    }

    LaunchedEffect(connectState.message, connectState.error) {
        connectState.message?.let { statusMessage = it; isError = false; isSubmitting = false }
        connectState.error?.let { statusMessage = it; isError = true; isSubmitting = false }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000L)
                recordingSeconds++
            }
        }
    }

    val triggerLocationFetch = rememberLocationFetcher { result ->
        result.onSuccess { geo ->
            latitude = geo.latitude
            longitude = geo.longitude
            wardLabel = geo.ward
            address = geo.address
            connectViewModel.reverseGeocode(geo.latitude, geo.longitude, "en")
            isError = false
            statusMessage = "Geotag attached (${formatCoord(geo.latitude)}° N, ${formatCoord(geo.longitude)}° E · ${geo.ward})."
        }.onFailure { err ->
            showMapPicker = true
            isError = true
            statusMessage = err.message ?: "Could not access GPS. Tap anywhere on the Ward Map below to pin your location."
        }
    }

    val triggerVoiceRecorder = rememberVoiceRecorder(
        isRecording = isRecording,
        onRecordingStateChange = { active -> isRecording = active },
        onResult = { result ->
            result.onSuccess { voice ->
                hasRecording = true
                voiceProviderLabel = "Speech-to-text"
                description = voice.transcript
                if (title.isBlank() && voice.suggestedTitle.isNotBlank()) {
                    title = voice.suggestedTitle
                }
                if (voice.suggestedCategory in setOf("ROADS", "WATER", "WASTE", "LIGHTING", "OTHER")) {
                    selectedCategory = voice.suggestedCategory
                }
                isError = false
                statusMessage = "Voice note transcribed. Review and edit before submitting."
            }.onFailure { err ->
                isError = true
                statusMessage = err.message ?: "Voice capture could not complete. You can type your report directly."
            }
        },
    )

    val categories = listOf(
        "ROADS" to t("Roads & footpaths", "सड़क और फुटपाथ"),
        "WATER" to t("Water & drains", "पानी और नालियाँ"),
        "WASTE" to t("Waste & sanitation", "कचरा और सफाई"),
        "LIGHTING" to t("Street lighting", "स्ट्रीट लाइट"),
        "OTHER" to t("Something else", "अन्य"),
    )

    val handlePinSelect: (Double, Double, String) -> Unit = { lat, lon, resolvedAddr ->
        latitude = lat
        longitude = lon
        address = resolvedAddr
        connectViewModel.reverseGeocode(lat, lon, "en")
        isError = false
        statusMessage = "Pinned location on Ward 4 Map (${formatCoord(lat)}° N, ${formatCoord(lon)}° E)."
    }

    val errTitleMsg = t("Please enter a short issue title (at least 5 characters).", "कृपया कम से कम 5 अक्षरों का शीर्षक लिखें।")
    val errDescMsg = t("Please describe what is happening (at least 10 characters).", "कृपया समस्या का विवरण लिखें (कम से कम 10 अक्षर)।")
    val errAddrMsg = t("Please enter an address or use the GPS / Ward Map geotag button.", "कृपया पता लिखें या जीपीएस / वार्ड मानचित्र का उपयोग करें।")
    val errConsentMsg = t("Please check the consent box before submitting.", "कृपया जमा करने से पहले सहमति बॉक्स चुनें।")
    val submittingMsg = t("Submitting report & syncing with municipal workflow...", "रिपोर्ट भेजी जा रही है...")
    val draftSavedMsg = t("Your draft stays on this screen. Local draft storage is not connected yet.", "आपका ड्राफ्ट इसी स्क्रीन पर रहेगा। स्थानीय ड्राफ्ट स्टोरेज अभी जुड़ा नहीं है।")

    val handleSubmit: () -> Unit = {
        when {
            title.trim().length < 5 -> {
                statusMessage = errTitleMsg
                isError = true
            }
            description.trim().length < 10 -> {
                statusMessage = errDescMsg
                isError = true
            }
            address.trim().length < 3 -> {
                statusMessage = errAddrMsg
                isError = true
            }
            !consentGranted -> {
                statusMessage = errConsentMsg
                isError = true
            }
            else -> {
                isSubmitting = true
                isError = false
                statusMessage = submittingMsg
                connectViewModel.submitReport(title, description, selectedCategory, address, latitude, longitude, consentGranted)
            }
        }
    }

    val handleSaveDraft: () -> Unit = {
        isError = false
        statusMessage = draftSavedMsg
    }

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
                    Eyebrow(t("YOUR VOICE MATTERS · GEOTAGGED & AI-ASSISTED", "आपकी बात महत्वपूर्ण है · जियोटैग और AI सहायता"))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = t("Let’s make it right.", "आइए इसे ठीक करें।"),
                        style = if (isWide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ext.ink,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = t("Speak or type the issue, attach GPS or a map pin, and route it to the accountable municipal team.", "बोलकर या लिखकर समस्या बताएँ, जीपीएस या मानचित्र पिन जोड़ें, और जिम्मेदार टीम तक पहुँचाएँ।"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.muted,
                    )
                }

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(40.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.55f)) {
                            ReportForm(
                                title = title,
                                onTitleChange = { title = it },
                                selectedCategory = selectedCategory,
                                onCategoryChange = { selectedCategory = it },
                                categories = categories,
                                description = description,
                                onDescriptionChange = { description = it },
                                isRecording = isRecording,
                                recordingSeconds = recordingSeconds,
                                hasRecording = hasRecording,
                                voiceProviderLabel = voiceProviderLabel,
                                onToggleRecording = triggerVoiceRecorder,
                                address = address,
                                onAddressChange = { address = it },
                                latitude = latitude,
                                longitude = longitude,
                                wardLabel = wardLabel,
                                showMapPicker = showMapPicker,
                                onToggleMapPicker = { showMapPicker = !showMapPicker },
                                onRequestGpsLocation = triggerLocationFetch,
                                onSelectMapPin = handlePinSelect,
                                consentGranted = consentGranted,
                                onConsentChange = { consentGranted = it },
                                statusMessage = statusMessage,
                                isError = isError,
                                isSubmitting = isSubmitting || connectState.isLoading,
                                onSubmit = handleSubmit,
                                onReview = { if (description.trim().length >= 10) connectViewModel.triage(title, description, address, "en") else { statusMessage = errDescMsg; isError = true } },
                                onSaveDraft = handleSaveDraft,
                            )
                        }

                        Column(modifier = Modifier.weight(1f).padding(top = 10.dp)) {
                            WhatHappensNextCard()
                        }
                    }
                } else {
                    ReportForm(
                        title = title,
                        onTitleChange = { title = it },
                        selectedCategory = selectedCategory,
                        onCategoryChange = { selectedCategory = it },
                        categories = categories,
                        description = description,
                        onDescriptionChange = { description = it },
                        isRecording = isRecording,
                        recordingSeconds = recordingSeconds,
                        hasRecording = hasRecording,
                        voiceProviderLabel = voiceProviderLabel,
                        onToggleRecording = triggerVoiceRecorder,
                        address = address,
                        onAddressChange = { address = it },
                        latitude = latitude,
                        longitude = longitude,
                        wardLabel = wardLabel,
                        showMapPicker = showMapPicker,
                        onToggleMapPicker = { showMapPicker = !showMapPicker },
                        onRequestGpsLocation = triggerLocationFetch,
                        onSelectMapPin = handlePinSelect,
                        consentGranted = consentGranted,
                        onConsentChange = { consentGranted = it },
                        statusMessage = statusMessage,
                        isError = isError,
                        isSubmitting = isSubmitting || connectState.isLoading,
                        onSubmit = handleSubmit,
                        onReview = { if (description.trim().length >= 10) connectViewModel.triage(title, description, address, "en") else { statusMessage = errDescMsg; isError = true } },
                        onSaveDraft = handleSaveDraft,
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    WhatHappensNextCard()
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            if (!isWide && title.isNotBlank() && description.isNotBlank()) {
                ExtendedFloatingActionButton(
                    onClick = { if (!isSubmitting && !connectState.isLoading) handleSubmit() },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Submit") },
                    text = { Text(t("Submit Report", "रिपोर्ट भेजें"), fontWeight = FontWeight.Bold) },
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
    recordingSeconds: Int,
    hasRecording: Boolean,
    voiceProviderLabel: String?,
    onToggleRecording: () -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    latitude: Double?,
    longitude: Double?,
    wardLabel: String,
    showMapPicker: Boolean,
    onToggleMapPicker: () -> Unit,
    onRequestGpsLocation: () -> Unit,
    onSelectMapPin: (Double, Double, String) -> Unit,
    consentGranted: Boolean,
    onConsentChange: (Boolean) -> Unit,
    statusMessage: String?,
    isError: Boolean,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
    onReview: () -> Unit,
    onSaveDraft: () -> Unit,
) {
    val ext = LocalTheekHaiColors.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ext.paper,
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Title
            Text(t("Give the issue a short title", "समस्या का संक्षिप्त शीर्षक"), style = MaterialTheme.typography.labelMedium, color = ext.ink, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text(t("e.g. Streetlight out near the crossing", "जैसे चौराहे के पास स्ट्रीट लाइट खराब"), fontSize = 13.sp, color = ext.muted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                colors = reportFieldColors(ext)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category dropdown
            Text(t("Type of issue", "समस्या का प्रकार"), style = MaterialTheme.typography.labelMedium, color = ext.ink, fontWeight = FontWeight.Bold)
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
            Text(t("What is happening?", "क्या हो रहा है?"), style = MaterialTheme.typography.labelMedium, color = ext.ink, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                placeholder = { Text(t("Describe the issue and how it affects the neighbourhood...", "समस्या और पड़ोस पर उसका असर बताएँ..."), fontSize = 13.sp, color = ext.muted) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 105.dp),
                shape = RoundedCornerShape(6.dp),
                maxLines = 6,
                colors = reportFieldColors(ext)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ── Working Audio Mode (Voice Grievance Recording & AI Speech-to-Text) ──
            Surface(
                color = if (isRecording) Color(0xFFFDF2EE) else ext.sidebarBg,
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isRecording) Color(0xFFEF4444) else ext.line
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        FilledIconButton(
                            onClick = onToggleRecording,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isRecording) Color(0xFFEF4444) else ext.green
                            )
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Check else Icons.Default.Mic,
                                contentDescription = if (isRecording) "Stop recording" else "Record voice note",
                                tint = Color.White
                            )
                        }
                        Column {
                            val mm = (recordingSeconds / 60).toString().padStart(2, '0')
                            val ss = (recordingSeconds % 60).toString().padStart(2, '0')
                            Text(
                                text = when {
                                    isRecording -> t("Recording voice note ($mm:$ss)… Tap to stop & transcribe", "आवाज़ रिकॉर्ड हो रही है ($mm:$ss)… रोकने के लिए टैप करें")
                                    hasRecording -> t("Voice note transcribed into form", "आवाज़ का लिखित रूप फॉर्म में भर दिया गया है")
                                    else -> t("Record voice grievance (English / हिन्दी)", "बोलकर शिकायत दर्ज करें (अंग्रेज़ी / हिन्दी)")
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) Color(0xFFB91C1C) else ext.ink
                            )
                            Text(
                                text = when {
                                    isRecording -> t("Speak clearly — AI will structure your title, category & description.", "स्पष्ट बोलें — AI स्वतः शीर्षक, श्रेणी और विवरण तैयार करेगा।")
                                    hasRecording -> t("Transcribed via ${voiceProviderLabel ?: "Speech-to-Text"} • Tap mic to record again", "${voiceProviderLabel ?: "स्पीच-टू-टेक्स्ट"} द्वारा तैयार • फिर रिकॉर्ड करने के लिए माइक दबाएँ")
                                    else -> t("Tap the microphone to dictate your issue hands-free.", "माइक्रोफोन दबाकर अपनी समस्या बोलें।")
                                },
                                fontSize = 11.sp,
                                color = ext.muted
                            )
                        }
                    }

                    if (hasRecording && !isRecording) {
                        Surface(
                            color = ext.greenSurface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = t("✓ Transcribed", "✓ लिखित रूप तैयार"),
                                fontSize = 10.sp,
                                color = ext.green,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Working Geotagging (Address + GPS Fetcher + Interactive Ward Map Picker) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    t("Address or landmark", "पता या पहचान की जगह"),
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.ink,
                    fontWeight = FontWeight.Bold
                )
                if (latitude != null && longitude != null) {
                    Surface(
                        color = ext.greenSurface,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "📍 ${formatCoord(latitude)}° N, ${formatCoord(longitude)}° E · $wardLabel",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ext.green,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = address,
                onValueChange = onAddressChange,
                placeholder = { Text(t("e.g. 5th Cross Road near Community Park, Ward 4", "जैसे 5वां क्रॉस रोड, कम्युनिटी पार्क के पास, वार्ड 4"), fontSize = 13.sp, color = ext.muted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onRequestGpsLocation) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Use GPS Location", tint = ext.green)
                    }
                },
                colors = reportFieldColors(ext)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // GPS & Interactive Ward Map Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onRequestGpsLocation,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = ext.green, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(t("Use my location", "मेरा स्थान लें"), fontSize = 12.sp, color = ext.ink)
                }

                OutlinedButton(
                    onClick = onToggleMapPicker,
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = ext.green, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (showMapPicker) t("Hide Ward Map", "मानचित्र छिपाएँ") else t("Pick on Ward Map", "मानचित्र पर चुनें"),
                        fontSize = 12.sp,
                        color = ext.ink
                    )
                }
            }

            // Interactive Tap-to-Pin Ward 4 Map Picker
            AnimatedVisibility(visible = showMapPicker) {
                InteractiveWardMapPicker(
                    selectedLat = latitude,
                    selectedLon = longitude,
                    onPinSelected = onSelectMapPin,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

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
                    text = t("I agree to share these details with the teams handling this report.", "मैं यह विवरण रिपोर्ट पर काम करने वाली टीमों के साथ साझा करने के लिए सहमत हूँ।"),
                    fontSize = 12.sp,
                    color = ext.ink,
                )
            }

            // Status message
            AnimatedVisibility(visible = statusMessage != null) {
                Surface(
                    color = if (isError) Color(0xFFF9EDE1) else ext.greenSurface,
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
                OutlinedButton(onClick = onReview, enabled = !isSubmitting, shape = RoundedCornerShape(6.dp)) {
                    Text(t("Review", "जाँचें"), fontSize = 12.sp, color = ext.ink)
                }
                TheekHaiButton(
                    text = if (isSubmitting) t("Submitting...", "भेजा जा रहा है...") else t("Review & submit report  ↗", "जाँचें और रिपोर्ट भेजें  ↗"),
                    onClick = onSubmit,
                    enabled = !isSubmitting,
                    modifier = Modifier.weight(1f)
                )

                OutlinedButton(
                    onClick = onSaveDraft,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(t("Save draft", "ड्राफ्ट सहेजें"), fontSize = 12.sp, color = ext.ink)
                }
            }
        }
    }
}

@Composable
private fun InteractiveWardMapPicker(
    selectedLat: Double?,
    selectedLon: Double?,
    onPinSelected: (Double, Double, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTheekHaiColors.current
    var pinFraction by remember(selectedLat, selectedLon) {
        val fx = if (selectedLon != null) ((selectedLon - 77.0200) / 0.0180).toFloat().coerceIn(0.08f, 0.92f) else 0.5f
        val fy = if (selectedLat != null) ((28.4720 - selectedLat) / 0.0180).toFloat().coerceIn(0.08f, 0.92f) else 0.5f
        mutableStateOf(Offset(fx, fy))
    }

    val presets = remember {
        listOf(
            Triple("Ward 4 School", 28.4595 to 77.0266, "Main Market Road near Ward 4 Primary School, Ward 4"),
            Triple("Central Park", 28.4612 to 77.0289, "5th Cross Road near Central Park East Gate, Ward 4"),
            Triple("Sector 14 Rotary", 28.4680 to 77.0310, "12th Main Road Junction, Sector 14 Crossing, Ward 4"),
            Triple("Main Market", 28.4645 to 77.0342, "Sector 14 Main Market Lane, Ward 4"),
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ext.sidebarBg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ext.line)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = t("TAP ANYWHERE ON WARD 4 MAP TO PIN GEOTAG", "जियोटैग पिन करने के लिए वार्ड 4 मानचित्र पर कहीं भी टैप करें"),
                style = MaterialTheme.typography.labelSmall,
                color = ext.muted,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val fx = (offset.x / size.width.toFloat()).coerceIn(0.05f, 0.95f)
                            val fy = (offset.y / size.height.toFloat()).coerceIn(0.05f, 0.95f)
                            pinFraction = Offset(fx, fy)
                            val lat = 28.4720 - fy * 0.0180
                            val lon = 77.0200 + fx * 0.0180
                            val resolved = resolveNearestWardAddress(lat, lon)
                            onPinSelected(lat, lon, resolved)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(color = Color(0xFFEEF2E8))

                    // Park & Civic Zones
                    drawRoundRect(
                        color = Color(0xFFD4E6CD),
                        topLeft = Offset(w * 0.06f, h * 0.08f),
                        size = Size(w * 0.32f, h * 0.36f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = Color(0xFFE5EDE0),
                        topLeft = Offset(w * 0.60f, h * 0.08f),
                        size = Size(w * 0.33f, h * 0.32f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = Color(0xFFEDE8DA),
                        topLeft = Offset(w * 0.60f, h * 0.56f),
                        size = Size(w * 0.34f, h * 0.36f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = Color(0xFFE2EBE0),
                        topLeft = Offset(w * 0.07f, h * 0.58f),
                        size = Size(w * 0.30f, h * 0.34f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    // Arterial Roads
                    drawRect(
                        color = Color(0xFFB8C5B2),
                        topLeft = Offset(0f, h * 0.44f),
                        size = Size(w, h * 0.12f)
                    )
                    drawLine(
                        color = Color(0xFFFBBF24),
                        start = Offset(0f, h * 0.50f),
                        end = Offset(w, h * 0.50f),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                    )
                    drawRect(
                        color = Color(0xFFB8C5B2),
                        topLeft = Offset(w * 0.46f, 0f),
                        size = Size(w * 0.08f, h)
                    )
                    drawCircle(
                        color = Color(0xFF9FB996),
                        radius = 16f,
                        center = Offset(w * 0.50f, h * 0.50f)
                    )

                    // Active Pin Marker
                    val px = pinFraction.x * w
                    val py = pinFraction.y * h
                    drawCircle(
                        color = Color(0xFF123E39).copy(alpha = 0.25f),
                        radius = 24f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 12f,
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = Color(0xFF123E39),
                        radius = 9f,
                        center = Offset(px, py)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Landmark Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { (label, coords, fullAddr) ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                onPinSelected(coords.first, coords.second, fullAddr)
                            },
                        color = ext.paper,
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ext.line)
                    ) {
                        Text(
                            text = "📍 $label",
                            fontSize = 10.sp,
                            color = ext.ink,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun resolveNearestWardAddress(lat: Double, lon: Double): String {
    data class Zone(val lat: Double, val lon: Double, val addr: String)
    val zones = listOf(
        Zone(28.4595, 77.0266, "Main Market Road near Ward 4 Primary School, Ward 4"),
        Zone(28.4612, 77.0289, "5th Cross Road near Central Park East Gate, Ward 4"),
        Zone(28.4680, 77.0310, "12th Main Road Junction, Sector 14 Crossing, Ward 4"),
        Zone(28.4645, 77.0342, "Sector 14 Main Market Lane, Ward 4"),
        Zone(28.4630, 77.0240, "West Colony Road near Primary Health Center, Ward 4"),
    )
    val closest = zones.minByOrNull { (it.lat - lat) * (it.lat - lat) + (it.lon - lon) * (it.lon - lon) } ?: zones.first()
    return "${closest.addr} (${formatCoord(lat)}° N, ${formatCoord(lon)}° E)"
}

private fun formatCoord(v: Double): String {
    val scaled = (v * 10000.0).roundToInt() / 10000.0
    return scaled.toString()
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
        Column(modifier = Modifier.padding(18.dp)) {
            Eyebrow(t("WHAT HAPPENS NEXT", "इसके बाद क्या होगा"))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = t("A report with a way forward.", "एक रिपोर्ट, आगे का रास्ता।"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ext.ink,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = t(
                    "Your grievance is assigned to an accountable department with visible SLA deadlines. You can follow tasks, due dates, and independent closure verification from your workspace.",
                    "आपकी शिकायत स्पष्ट समय-सीमा के साथ जिम्मेदार विभाग को सौंपी जाती है। आप कार्यक्षेत्र में हर कदम देख सकते हैं।"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = ext.muted,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = t("Civic pilot • Not an emergency service", "नागरिक पायलट • आपातकालीन सेवा नहीं"),
                fontSize = 11.sp,
                color = ext.amber,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun reportFieldColors(ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ext.green,
        unfocusedBorderColor = ext.line,
        focusedContainerColor = ext.paper,
        unfocusedContainerColor = ext.paper,
        focusedTextColor = ext.ink,
        unfocusedTextColor = ext.ink,
    )
