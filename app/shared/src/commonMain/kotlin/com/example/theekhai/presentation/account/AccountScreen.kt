package com.example.theekhai.presentation.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.domain.model.CivicProfile
import com.example.theekhai.presentation.nativeconnect.NativeConnectViewModel
import org.koin.compose.viewmodel.koinViewModel
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors

data class SavedLegalSource(
    val id: String,
    val title: String,
    val jurisdiction: String,
    val snippet: String,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen() {
    val viewModel = koinViewModel<NativeConnectViewModel>()
    val state by viewModel.state.collectAsState()
    val navigator = LocalNavigator.current
    val ext = LocalTheekHaiColors.current

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var organization by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("English") }
    var langExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadAccount() }
    LaunchedEffect(state.profile) {
        state.profile?.let { profile ->
            fullName = profile.fullName; email = profile.email; organization = profile.organization; ward = profile.ward
            selectedLanguage = if (profile.preferredLanguage == "hi") "हिन्दी (Hindi)" else "English"
        }
    }
    val saveFeedback = state.error ?: state.message
    val savedSources = state.savedSources.map { SavedLegalSource(it.id, it.title, it.jurisdiction, it.snippet, it.sourceUrl) }
    val saveProfile = { viewModel.saveProfile(CivicProfile(state.profile?.userId.orEmpty(), email, fullName, organization, state.profile?.role ?: "CITIZEN", state.profile?.department, ward, if (selectedLanguage.startsWith("हिन्दी")) "hi" else "en")) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 850.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isWide) 48.dp else 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Eyebrow("CITIZEN PROFILE")
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Your civic profile & preferences",
                style = MaterialTheme.typography.headlineLarge,
                color = ext.ink,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your profile and saved references sync to your account.",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isWide) {
                // Wide: 2 Columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    // Profile edit panel
                    Column(modifier = Modifier.weight(1.1f)) {
                        ProfileFormPanel(
                            fullName = fullName,
                            onFullNameChange = { fullName = it },
                            email = email,
                            organization = organization,
                            onOrgChange = { organization = it },
                            ward = ward,
                            onWardChange = { ward = it },
                            selectedLanguage = selectedLanguage,
                            onLanguageChange = { selectedLanguage = it },
                            langExpanded = langExpanded,
                            onLangExpandedChange = { langExpanded = it },
                            isSaving = state.isLoading,
                            saveFeedback = saveFeedback,
                            onSave = saveProfile,
                            ext = ext
                        )
                    }

                    // Bookmarked sources panel
                    Column(modifier = Modifier.weight(0.9f)) {
                        SavedSourcesPanel(
                            sources = savedSources,
                            onRemove = viewModel::removeSavedSource,
                            onExploreLibrary = { navigator.navigateTo(Route.Library) },
                            ext = ext
                        )
                    }
                }
            } else {
                // Mobile / narrow layout: Stacked
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileFormPanel(
                        fullName = fullName,
                        onFullNameChange = { fullName = it },
                        email = email,
                        organization = organization,
                        onOrgChange = { organization = it },
                        ward = ward,
                        onWardChange = { ward = it },
                        selectedLanguage = selectedLanguage,
                        onLanguageChange = { selectedLanguage = it },
                        langExpanded = langExpanded,
                        onLangExpandedChange = { langExpanded = it },
                        isSaving = state.isLoading,
                        saveFeedback = saveFeedback,
                        onSave = saveProfile,
                        ext = ext
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    SavedSourcesPanel(
                        sources = savedSources,
                        onRemove = viewModel::removeSavedSource,
                        onExploreLibrary = { navigator.navigateTo(Route.Library) },
                        ext = ext
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            ServerEndpointPanel(
                currentUrl = state.backendUrl,
                onUpdateUrl = viewModel::updateBackendUrl,
                ext = ext
            )

            Spacer(modifier = Modifier.height(24.dp))
            TheekHaiButton(
                text = if (state.isLoading) "Working…" else "Sign out",
                onClick = { viewModel.signOut(); navigator.resetTo(Route.Login) },
                enabled = !state.isLoading,
                isSecondary = true
            )
        }
    }
}

@Composable
private fun ServerEndpointPanel(
    currentUrl: String,
    onUpdateUrl: (String) -> Unit,
    ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors
) {
    var customUrl by remember(currentUrl) { mutableStateOf(currentUrl) }

    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Eyebrow("BACKEND SERVER INTEGRATION")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Municipal API Endpoint", style = MaterialTheme.typography.titleMedium, color = ext.ink, fontWeight = FontWeight.Bold)
        Text("Switch connection between Docker, local development, and live cloud backend:", style = MaterialTheme.typography.bodySmall, color = ext.muted)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = customUrl,
            onValueChange = { customUrl = it },
            label = { Text("API Base URL") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    customUrl = "https://bless-stinger-plow.ngrok-free.dev"
                    onUpdateUrl(customUrl)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("🌐 Cloud Ngrok", fontSize = 11.sp)
            }
            OutlinedButton(
                onClick = {
                    customUrl = "http://10.0.2.2:8080"
                    onUpdateUrl(customUrl)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("🐳 Docker/Emulator", fontSize = 11.sp)
            }
            Button(
                onClick = { onUpdateUrl(customUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = ext.green)
            ) {
                Text("Connect", fontSize = 12.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileFormPanel(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    organization: String,
    onOrgChange: (String) -> Unit,
    ward: String,
    onWardChange: (String) -> Unit,
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    langExpanded: Boolean,
    onLangExpandedChange: (Boolean) -> Unit,
    isSaving: Boolean,
    saveFeedback: String?,
    onSave: () -> Unit,
    ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors
) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarCircle(initials = fullName, size = 52.dp, bgColor = TheekHaiColors.Green)
            Column {
                StatusChip(
                    label = "Connected account",
                    containerColor = TheekHaiColors.GreenSurface,
                    contentColor = TheekHaiColors.Green
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(fullName, style = MaterialTheme.typography.titleLarge, color = ext.ink)
                Text(if (email.isBlank()) "Not signed in" else email, style = MaterialTheme.typography.bodySmall, color = ext.muted)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        TheekHaiDivider()
        Spacer(modifier = Modifier.height(20.dp))

        // Form fields
        Text("Full name", style = MaterialTheme.typography.labelMedium, color = ext.ink)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Organization / RWA", style = MaterialTheme.typography.labelMedium, color = ext.ink)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = organization,
            onValueChange = onOrgChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Default neighbourhood / Ward", style = MaterialTheme.typography.labelMedium, color = ext.ink)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = ward,
            onValueChange = onWardChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Preferred language", style = MaterialTheme.typography.labelMedium, color = ext.ink)
        Spacer(modifier = Modifier.height(6.dp))
        ExposedDropdownMenuBox(
            expanded = langExpanded,
            onExpandedChange = onLangExpandedChange
        ) {
            OutlinedTextField(
                value = selectedLanguage,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                shape = RoundedCornerShape(4.dp)
            )
            ExposedDropdownMenu(
                expanded = langExpanded,
                onDismissRequest = { onLangExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("English") },
                    onClick = {
                        onLanguageChange("English")
                        onLangExpandedChange(false)
                    }
                )
                DropdownMenuItem(
                    text = { Text("हिन्दी (Hindi)") },
                    onClick = {
                        onLanguageChange("हिन्दी (Hindi)")
                        onLangExpandedChange(false)
                    }
                )
            }
        }

        saveFeedback?.let { msg ->
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                color = Color(0xFFE8EDE2),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = TheekHaiColors.Green, modifier = Modifier.size(16.dp))
                    Text(msg, color = TheekHaiColors.Green, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TheekHaiButton(
            text = if (isSaving) "Saving..." else "Save Profile",
            onClick = onSave,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SavedSourcesPanel(
    sources: List<SavedLegalSource>,
    onRemove: (String) -> Unit,
    onExploreLibrary: () -> Unit,
    ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors
) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Eyebrow("BOOKMARKED CIVIC GUIDELINES")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Saved service standards", style = MaterialTheme.typography.headlineSmall, color = ext.ink)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Quick access to municipal service timelines, standards, and appeal guidelines.",
            style = MaterialTheme.typography.bodySmall,
            color = ext.muted
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (sources.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = Icons.Default.Bookmark, contentDescription = null, tint = ext.muted, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("No bookmarked legal sources yet.", style = MaterialTheme.typography.bodyMedium, color = ext.muted)
                Spacer(modifier = Modifier.height(12.dp))
                TheekHaiButton(text = "Explore Reference Library  ↗", onClick = onExploreLibrary, isSecondary = true)
            }
        } else {
            sources.forEach { src ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = TheekHaiColors.StatusDefault,
                            shape = RoundedCornerShape(3.dp)
                        ) {
                            Text(
                                text = src.jurisdiction,
                                style = MaterialTheme.typography.labelSmall,
                                color = TheekHaiColors.StatusDefaultText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = { onRemove(src.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = ext.muted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = src.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = ext.green,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = src.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted,
                        lineHeight = 18.sp
                    )
                }
                TheekHaiDivider()
            }

            Spacer(modifier = Modifier.height(14.dp))
            TheekHaiButton(
                text = "Explore more in Reference Library  →",
                onClick = onExploreLibrary,
                modifier = Modifier.fillMaxWidth(),
                isSecondary = true
            )
        }
    }
}
