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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import theekhai.app.shared.generated.resources.Res
import theekhai.app.shared.generated.resources.broken_streetlight
import com.example.theekhai.platform.rememberLocationFetcher
import com.example.theekhai.platform.rememberVoiceRecorder
import com.example.theekhai.platform.rememberPhotoCapture
import com.example.theekhai.platform.rememberEmailDraftExporter
import com.example.theekhai.platform.GrievanceEmailDraft
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.localization.t
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors
import com.example.theekhai.presentation.nativeconnect.NativeConnectViewModel
import com.example.theekhai.presentation.map.NeighborhoodMapData
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private data class AuthorityDetails(
    val designation: String,
    val department: String,
    val email: String,
    val slaHours: Int
)

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
    var wardLabel by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }

    var attachedPhotoBase64 by remember { mutableStateOf<String?>(null) }
    var attachedPhotoName by remember { mutableStateOf<String?>(null) }
    var isSampleStreetlight by remember { mutableStateOf(false) }
    var displayedGrievanceDraft by remember { mutableStateOf<GrievanceEmailDraft?>(null) }

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
            statusMessage = "Location attached. Resolving its address…"
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

    val triggerPhotoCapture = rememberPhotoCapture { result ->
        result.onSuccess { photo ->
            attachedPhotoBase64 = photo.base64Data
            attachedPhotoName = photo.fileName
            isSampleStreetlight = false
            if (latitude == null || longitude == null) {
                latitude = 28.4595
                longitude = 77.0266
                wardLabel = "Ward 4"
                address = "Main Market Road near Ward 4 Primary School, Ward 4"
            }
            isError = false
            statusMessage = "Photo evidence attached & geotag locked."
        }.onFailure { err ->
            statusMessage = err.message ?: "Could not capture photo."
        }
    }

    val triggerEmailExport = rememberEmailDraftExporter()
    val sampleStreetlightTitle = t("Broken Streetlight & Exposed Wiring near School Gate", "स्कूल गेट के पास टूटी स्ट्रीटलाइट और खुला तार")
    val draftGeneratedMsg = t(
        "Official grievance petition draft generated on screen below! Tap 'Open in Gmail App' to send.",
        "आधिकारिक शिकायत ड्राफ्ट नीचे स्क्रीन पर तैयार है! भेजने के लिए 'जीमेल ऐप में खोलें' दबाएँ।"
    )

    val handleUseSamplePhoto = {
        attachedPhotoBase64 = "sample://broken_streetlight"
        attachedPhotoName = "Broken_Streetlight_Ward4.jpg"
        isSampleStreetlight = true
        if (title.isBlank()) {
            title = sampleStreetlightTitle
        }
        selectedCategory = "LIGHTING"
        if (latitude == null || longitude == null) {
            latitude = 28.4595
            longitude = 77.0266
            wardLabel = "Ward 4"
            address = "Main Market Road near Ward 4 Primary School, Ward 4"
        }
        isError = false
        statusMessage = "Sample streetlight evidence loaded & geotag locked to Ward 4 Primary School."
    }

    val handleRemovePhoto = {
        attachedPhotoBase64 = null
        attachedPhotoName = null
        isSampleStreetlight = false
    }

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

                val authority = when (selectedCategory) {
                    "LIGHTING" -> AuthorityDetails(
                        "Superintending Engineer (Operations & Streetlighting)",
                        "DHBVN & Municipal Electrical Wing",
                        "se.electrical.dhbvn@haryana.gov.in",
                        48
                    )
                    "ROADS" -> AuthorityDetails(
                        "Executive Engineer (Infrastructure & Roads)",
                        "PWD B&R Division Haryana",
                        "ee.pwd.ggn@haryana.gov.in",
                        72
                    )
                    "WATER" -> AuthorityDetails(
                        "Executive Engineer (Public Health & Sewerage)",
                        "Municipal Corporation Water Supply & Sewerage Division",
                        "xen.water.mcg@haryana.gov.in",
                        48
                    )
                    "WASTE" -> AuthorityDetails(
                        "Joint Commissioner (Swachh Bharat Cell)",
                        "Municipal Corporation Solid Waste & Sanitation Wing",
                        "commissioner@mcg.gov.in",
                        24
                    )
                    else -> AuthorityDetails(
                        "Nodal Public Grievance Officer",
                        "Municipal Corporation Public Grievance Redressal Cell",
                        "commissioner@mcg.gov.in",
                        72
                    )
                }

                val coordStr = if (latitude != null && longitude != null) "${formatCoord(latitude!!)}° N, ${formatCoord(longitude!!)}° E (Ward 4 Murthal)" else "Ward 4 Murthal / Sector 14 Crossing"
                val subject = "[Civic Grievance RTSA-2026] ${title.trim()} - Ward 4"

                val body = """
To:
${authority.designation}
${authority.department}
Municipal Corporation / Government of Haryana
Official Grievance Email: ${authority.email}

Subject: $subject

Respected Sir/Madam,

I am submitting this formal statutory civic grievance under the Haryana Right to Service Act (RTSA), 2026, for urgent municipal intervention in Ward 4.

1. Issue: ${title.trim()}
2. Category: $selectedCategory
3. Location / Landmark: ${address.ifBlank { "Ward 4, Murthal" }}
4. Geotagged GPS Coordinates: $coordStr
5. Prescribed Statutory SLA: ${authority.slaHours} Hours (RTSA Citizen Charter)

6. Detailed Grievance Description:
${description.trim()}

7. Photographic Evidence:
${if (attachedPhotoName != null) "Geotagged photographic visual evidence captured on-site and attached with this email dispatch." else "On-site photographic inspection requested."}

Under Section 7 of the Haryana Right to Service Act 2026, designated municipal officers are bound to resolve this civic defect within ${authority.slaHours} hours or furnish written justification. Kindly register this petition, dispatch the field team, and issue an official acknowledgment receipt.

Yours sincerely,
Resident / Complainant of Ward 4
Dispatched via: TheekHai Municipal Civic Portal
Platform: https://theekhai.in
""".trimIndent()

                val draft = GrievanceEmailDraft(
                    recipientEmail = authority.email,
                    authorityName = "${authority.designation}, ${authority.department}",
                    subject = subject,
                    body = body,
                    trackingId = connectState.report?.id ?: "W4-${System.currentTimeMillis().toString(36).uppercase()}",
                    photoBase64 = attachedPhotoBase64,
                    latitude = latitude,
                    longitude = longitude,
                    address = address.ifBlank { "Ward 4, Murthal" }
                )
                displayedGrievanceDraft = draft

                statusMessage = draftGeneratedMsg
                connectViewModel.submitReport(title, description, selectedCategory, address, latitude, longitude, consentGranted, attachedPhotoBase64)
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
                                attachedPhotoBase64 = attachedPhotoBase64,
                                attachedPhotoName = attachedPhotoName,
                                isSampleStreetlight = isSampleStreetlight,
                                onRequestPhotoCapture = triggerPhotoCapture,
                                onUseSamplePhoto = handleUseSamplePhoto,
                                onRemovePhoto = handleRemovePhoto,
                                displayedGrievanceDraft = displayedGrievanceDraft,
                                onOpenGmail = { triggerEmailExport(it) },
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
                        attachedPhotoBase64 = attachedPhotoBase64,
                        attachedPhotoName = attachedPhotoName,
                        isSampleStreetlight = isSampleStreetlight,
                        onRequestPhotoCapture = triggerPhotoCapture,
                        onUseSamplePhoto = handleUseSamplePhoto,
                        onRemovePhoto = handleRemovePhoto,
                        displayedGrievanceDraft = displayedGrievanceDraft,
                        onOpenGmail = { triggerEmailExport(it) },
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
    attachedPhotoBase64: String?,
    attachedPhotoName: String?,
    isSampleStreetlight: Boolean,
    onRequestPhotoCapture: () -> Unit,
    onUseSamplePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    displayedGrievanceDraft: GrievanceEmailDraft?,
    onOpenGmail: (GrievanceEmailDraft) -> Unit,
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

            // ── Photographic Evidence Section (Section 7 RTSA Compliance) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEEF1E9), RoundedCornerShape(6.dp))
                    .padding(14.dp)
            ) {
                Text(
                    t("📸 PHOTOGRAPHIC EVIDENCE · RTSA COMPLIANCE", "📸 फोटो साक्ष्य · आरटीएसए अनुपालन"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ext.green
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    t(
                        "Geotagged photos provide non-repudiable legal proof under Section 7 of the Haryana Right to Service Act.",
                        "जियोटैग की गई तस्वीरें सेवा का अधिकार अधिनियम की धारा 7 के तहत कानूनी सबूत प्रदान करती हैं।"
                    ),
                    fontSize = 12.sp,
                    color = ext.muted
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRequestPhotoCapture,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = ext.green, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(t("Capture / Attach", "फोटो लें / जोड़ें"), fontSize = 12.sp, color = ext.ink)
                    }

                    OutlinedButton(
                        onClick = onUseSamplePhoto,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(t("💡 Sample Streetlight", "💡 नमूना स्ट्रीटलाइट"), fontSize = 12.sp, color = ext.ink)
                    }
                }

                if (attachedPhotoName != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = ext.paper,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (isSampleStreetlight) {
                                androidx.compose.foundation.Image(
                                    painter = painterResource(Res.drawable.broken_streetlight),
                                    contentDescription = "Broken Streetlight Sample Evidence",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        attachedPhotoName ?: "evidence.jpg",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.ink
                                    )
                                    Text(
                                        "📍 28.4595° N, 77.0266° E · Ward 4",
                                        fontSize = 10.sp,
                                        color = ext.green
                                    )
                                }
                                TextButton(onClick = onRemovePhoto) {
                                    Text(t("Remove", "हटाएं"), fontSize = 11.sp, color = Color(0xFFDC2626))
                                }
                            }
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

            // ── Official Grievance Petition & Authority Dispatch Card (Rendered On Screen) ──
            AnimatedVisibility(visible = displayedGrievanceDraft != null) {
                displayedGrievanceDraft?.let { draft ->
                    val clipboard = LocalClipboardManager.current
                    var copied by remember { mutableStateOf(false) }

                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        color = Color(0xFFF6F8F2),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(2.dp, ext.green)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                t("🏛️ OFFICIAL CIVIC GRIEVANCE PETITION · DISPATCH READY", "🏛️ आधिकारिक नागरिक शिकायत याचिका · प्रेषण तैयार"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ext.green
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                t("Haryana Right to Service Act (RTSA 2026) Official Draft", "हरियाणा सेवा का अधिकार अधिनियम (RTSA 2026) आधिकारिक ड्राफ्ट"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ext.ink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                t("Official petition letter compiled for immediate dispatch to the accountable municipal department.", "जवाबदेह नगरपालिका विभाग को तत्काल भेजने हेतु आधिकारिक शिकायत पत्र तैयार है।"),
                                fontSize = 12.sp,
                                color = ext.muted
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Authority Info Box
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = ext.paper,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD2D9CB))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(t("DESIGNATED AUTHORITY", "नामित प्राधिकारी"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ext.muted)
                                    Text(draft.authorityName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ext.ink)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = Color(0xFFE8EFE3),
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("To: ${draft.recipientEmail}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ext.ink)
                                            Text(
                                                if (copied) "✓ Copied" else "Copy",
                                                fontSize = 11.sp,
                                                color = ext.green,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable {
                                                    clipboard.setText(AnnotatedString(draft.recipientEmail))
                                                    copied = true
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Subject: ${draft.subject}", fontSize = 12.sp, color = ext.ink)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Statutory Citizen Instruction Box
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    t(
                                        "⚠️ Important Citizen Step: Tap 'Open in Gmail App' to launch your email client with the authority email, subject, and petition body pre-filled. Attach your captured geotagged photo before sending so municipal authorities cannot reject under Section 7 of RTSA.",
                                        "⚠️ महत्वपूर्ण नागरिक निर्देश: ईमेल खोलने के लिए 'जीमेल ऐप में खोलें' दबाएँ। भेजने से पहले अपनी खींची गई जियोटैग फोटो अवश्य जोड़ें ताकि अधिकारी आरटीएसए की धारा 7 के तहत इसे खारिज न कर सकें।"
                                    ),
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Preformatted Petition Letter Preview
                            Surface(
                                color = ext.paper,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD2D9CB)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = draft.body,
                                    fontSize = 11.sp,
                                    color = ext.ink,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { onOpenGmail(draft) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA4335), contentColor = Color.White),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(t("Open in Gmail App", "जीमेल ऐप में खोलें"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(draft.body))
                                        copied = true
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = ext.ink)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (copied) t("Copied!", "कॉपी हुआ!") else t("Copy", "कॉपी करें"), fontSize = 11.sp, color = ext.ink)
                                }
                            }
                        }
                    }
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
    val darkMap = ext.paper.luminance() < .5f
    val hasSelectedPoint = selectedLat != null && selectedLon != null &&
        selectedLat in NeighborhoodMapData.MIN_LAT..NeighborhoodMapData.MAX_LAT &&
        selectedLon in NeighborhoodMapData.MIN_LON..NeighborhoodMapData.MAX_LON
    val fx = if (hasSelectedPoint) ((selectedLon - NeighborhoodMapData.MIN_LON) / (NeighborhoodMapData.MAX_LON - NeighborhoodMapData.MIN_LON)).toFloat() else .5f
    val fy = if (hasSelectedPoint) ((NeighborhoodMapData.MAX_LAT - selectedLat) / (NeighborhoodMapData.MAX_LAT - NeighborhoodMapData.MIN_LAT)).toFloat() else .5f
    val landmarks = remember {
        NeighborhoodMapData.landmarks.filter {
            it.lat in NeighborhoodMapData.MIN_LAT..NeighborhoodMapData.MAX_LAT &&
                it.lon in NeighborhoodMapData.MIN_LON..NeighborhoodMapData.MAX_LON && it.name.isNotBlank()
        }.distinctBy { it.name }.take(4)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ext.sidebarBg,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ext.line)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = t("TAP ANYWHERE ON THE MURTHAL MAP TO PIN A LOCATION", "स्थान पिन करने के लिए मुरथल मानचित्र पर कहीं भी टैप करें"),
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
                            val lat = NeighborhoodMapData.MAX_LAT - fy * (NeighborhoodMapData.MAX_LAT - NeighborhoodMapData.MIN_LAT)
                            val lon = NeighborhoodMapData.MIN_LON + fx * (NeighborhoodMapData.MAX_LON - NeighborhoodMapData.MIN_LON)
                            onPinSelected(lat, lon, "Resolving address…")
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(color = ext.sidebarBg)
                    val sx = size.width / 2000f
                    val sy = size.height / 2000f
                    NeighborhoodMapData.roads.forEach { road ->
                        if (road.points.size >= 2) {
                            val path = Path().apply {
                                moveTo(road.points.first().first * sx, road.points.first().second * sy)
                                road.points.drop(1).forEach { (x, y) -> lineTo(x * sx, y * sy) }
                            }
                            val roadColor = if (road.isMajor) {
                                if (darkMap) Color(0xFF737A68) else Color(0xFFB9B3A0)
                            } else {
                                if (darkMap) Color(0xFF3D4A42) else Color(0xFFB9B3A0)
                            }
                            drawPath(path, roadColor, style = Stroke(if (road.isMajor) 8f else 3f))
                            if (road.isMajor) drawPath(path, if (darkMap) Color(0xFF26322B) else Color(0xFFF8F6EF), style = Stroke(2.5f))
                        }
                    }
                    landmarks.forEach { landmark ->
                        drawCircle(Color(0xFF74816D), radius = 3f, center = Offset(landmark.x * sx, landmark.y * sy))
                    }
                    val px = fx * size.width
                    val py = fy * size.height
                    if (hasSelectedPoint) {
                        drawCircle(Color(0xFF123E39).copy(alpha = .2f), 22f, Offset(px, py))
                        drawCircle(Color.White, 11f, Offset(px, py))
                        drawCircle(Color(0xFF123E39), 8f, Offset(px, py))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Nearby landmarks from the same offline map data.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                landmarks.forEach { landmark ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable {
                                onPinSelected(landmark.lat, landmark.lon, "Resolving address…")
                            },
                        color = ext.paper,
                        shape = RoundedCornerShape(999.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ext.line)
                    ) {
                        Text(
                            text = "📍 ${landmark.name}",
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
