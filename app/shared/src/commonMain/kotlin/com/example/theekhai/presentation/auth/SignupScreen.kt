package com.example.theekhai.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors
import com.example.theekhai.presentation.nativeconnect.NativeConnectViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignupScreen() {
    val connectViewModel = koinViewModel<NativeConnectViewModel>()
    val connectState by connectViewModel.state.collectAsState()
    val navigator = LocalNavigator.current
    val ext = LocalTheekHaiColors.current
    val scrollState = rememberScrollState()

    LaunchedEffect(connectState.user) {
        if (connectState.user != null) navigator.resetTo(Route.Workspace)
    }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("Ward 4 (DLF Phase 1 & Old City)") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var consentChecked by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val passwordStrength = remember(password) {
        when {
            password.length >= 10 && password.any { it.isUpperCase() } && password.any { !it.isLetterOrDigit() } -> 3
            password.length >= 8 -> 2
            password.length >= 6 -> 1
            else -> 0
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 850.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(if (isWide) 48.dp else 20.dp)
        ) {
            Eyebrow("CITIZEN REGISTRATION")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Join your neighbourhood",
                style = MaterialTheme.typography.headlineLarge,
                color = ext.ink,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Create a verified resident account to submit civic grievances and track resolution status.",
                style = MaterialTheme.typography.bodyMedium,
                color = ext.muted
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    Column(modifier = Modifier.weight(1.1f)) {
                        if (connectState.signupVerificationSent) {
                            SignupEmailVerificationPending(
                                email = connectState.pendingSignupEmail ?: email,
                                devToken = connectState.signupDevToken,
                                isLoading = connectState.isLoading,
                                errorMessage = connectState.error ?: errorMessage,
                                onVerifyToken = { tok -> connectViewModel.verifyEmail(tok, connectState.pendingSignupEmail ?: email) },
                                onResend = { connectViewModel.resendVerification(connectState.pendingSignupEmail ?: email) },
                                onNavigateLogin = { navigator.navigateTo(Route.Login) },
                                ext = ext
                            )
                        } else {
                            SignupForm(
                                fullName = fullName,
                                onFullNameChange = { fullName = it },
                                email = email,
                                onEmailChange = { email = it },
                                ward = ward,
                                onWardChange = { ward = it },
                                password = password,
                                onPasswordChange = { password = it },
                                confirmPassword = confirmPassword,
                                onConfirmPasswordChange = { confirmPassword = it },
                                passwordVisible = passwordVisible,
                                onTogglePassword = { passwordVisible = !passwordVisible },
                                passwordStrength = passwordStrength,
                                consentChecked = consentChecked,
                                onConsentChange = { consentChecked = it },
                                isLoading = connectState.isLoading,
                                errorMessage = connectState.error ?: errorMessage,
                                onSubmit = {
                                    if (password != confirmPassword) {
                                        errorMessage = "Passwords do not match."
                                        return@SignupForm
                                    }
                                    if (!consentChecked) {
                                        errorMessage = "Please accept the privacy consent to proceed."
                                        return@SignupForm
                                    }
                                    connectViewModel.register(email, password, fullName, ward)
                                },
                                onNavigateLogin = { navigator.navigateTo(Route.Login) },
                                ext = ext
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(0.9f)) {
                        CitizenBenefitsPanel(ext = ext)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    if (connectState.signupVerificationSent) {
                        SignupEmailVerificationPending(
                            email = connectState.pendingSignupEmail ?: email,
                            devToken = connectState.signupDevToken,
                            isLoading = connectState.isLoading,
                            errorMessage = connectState.error ?: errorMessage,
                            onVerifyToken = { tok -> connectViewModel.verifyEmail(tok, connectState.pendingSignupEmail ?: email) },
                            onResend = { connectViewModel.resendVerification(connectState.pendingSignupEmail ?: email) },
                            onNavigateLogin = { navigator.navigateTo(Route.Login) },
                            ext = ext
                        )
                    } else {
                        SignupForm(
                            fullName = fullName,
                            onFullNameChange = { fullName = it },
                            email = email,
                            onEmailChange = { email = it },
                            ward = ward,
                            onWardChange = { ward = it },
                            password = password,
                            onPasswordChange = { password = it },
                            confirmPassword = confirmPassword,
                            onConfirmPasswordChange = { confirmPassword = it },
                            passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible },
                            passwordStrength = passwordStrength,
                            consentChecked = consentChecked,
                            onConsentChange = { consentChecked = it },
                            isLoading = connectState.isLoading,
                            errorMessage = connectState.error ?: errorMessage,
                            onSubmit = {
                                if (password != confirmPassword) {
                                    errorMessage = "Passwords do not match."
                                    return@SignupForm
                                }
                                if (!consentChecked) {
                                    errorMessage = "Please accept the privacy consent to proceed."
                                    return@SignupForm
                                }
                                connectViewModel.register(email, password, fullName, ward)
                            },
                            onNavigateLogin = { navigator.navigateTo(Route.Login) },
                            ext = ext
                        )
                    }

                    CitizenBenefitsPanel(ext = ext)
                }
            }
        }
    }
}

@Composable
private fun SignupForm(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    ward: String,
    onWardChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    passwordStrength: Int,
    consentChecked: Boolean,
    onConsentChange: (Boolean) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onSubmit: () -> Unit,
    onNavigateLogin: () -> Unit,
    ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors
) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Text("Full name", style = MaterialTheme.typography.labelMedium, color = ext.ink)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = { Text("e.g. Priya Sharma") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Email address", style = MaterialTheme.typography.labelMedium, color = ext.ink)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text("priya@example.com") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Neighbourhood / Ward", style = MaterialTheme.typography.labelMedium, color = ext.ink)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = ward,
            onValueChange = onWardChange,
            placeholder = { Text("e.g. Ward 4") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Create password", style = MaterialTheme.typography.labelMedium, color = ext.ink)
            Text(
                text = if (passwordVisible) "Hide" else "Show",
                fontSize = 12.sp,
                color = ext.green,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onTogglePassword() }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("At least 8 characters") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
        )

        if (password.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(3) { index ->
                    val barColor = when {
                        index < passwordStrength -> when (passwordStrength) {
                            1 -> Color(0xFFF59E0B)
                            2 -> Color(0xFF10B981)
                            else -> TheekHaiColors.Green
                        }
                        else -> ext.line
                    }
                    Surface(
                        modifier = Modifier.weight(1f).height(4.dp),
                        color = barColor,
                        shape = RoundedCornerShape(2.dp)
                    ) {}
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Confirm password", style = MaterialTheme.typography.labelMedium, color = ext.ink)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = { Text("Re-enter password") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.clickable { onConsentChange(!consentChecked) }
        ) {
            Checkbox(
                checked = consentChecked,
                onCheckedChange = onConsentChange,
                colors = CheckboxDefaults.colors(checkedColor = TheekHaiColors.Green)
            )
            Text(
                text = "I consent to my contact details being used exclusively for processing civic service reports in accordance with privacy regulations.",
                style = MaterialTheme.typography.bodySmall,
                color = ext.ink,
                lineHeight = 18.sp
            )
        }

        errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(14.dp))
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

        Spacer(modifier = Modifier.height(24.dp))

        TheekHaiButton(
            text = if (isLoading) "Creating account..." else "Create citizen account",
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = fullName.isNotBlank() && email.isNotBlank() && password.length >= 6 && !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Already registered? ", style = MaterialTheme.typography.bodySmall, color = ext.muted)
            Text(
                text = "Sign in instead →",
                style = MaterialTheme.typography.bodySmall,
                color = ext.green,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onNavigateLogin() }
            )
        }
    }
}

@Composable
private fun CitizenBenefitsPanel(ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Eyebrow("CITIZEN PARTICIPATION")
        Spacer(modifier = Modifier.height(10.dp))
        Text("Track every step to resolution.", style = MaterialTheme.typography.headlineSmall, color = ext.ink, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Your citizen profile links all your neighbourhood grievances to responsible departments, service deadlines, and verified fix evidence.",
            style = MaterialTheme.typography.bodyMedium,
            color = ext.muted,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))
        TheekHaiDivider()
        Spacer(modifier = Modifier.height(18.dp))

        Text("Direct SLA Tracking", style = MaterialTheme.typography.titleSmall, color = ext.ink)
        Spacer(modifier = Modifier.height(4.dp))
        Text("See assigned engineers, expected completion hours, and automated escalation milestones.", style = MaterialTheme.typography.bodySmall, color = ext.muted)

        Spacer(modifier = Modifier.height(14.dp))

        Text("Evidence Before Closure", style = MaterialTheme.typography.titleSmall, color = ext.ink)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Field workers must upload geotagged proof of work. No case is marked done on paper alone.", style = MaterialTheme.typography.bodySmall, color = ext.muted)

        Spacer(modifier = Modifier.height(14.dp))

        Text("Notice", style = MaterialTheme.typography.labelSmall, color = TheekHaiColors.Amber)
        Spacer(modifier = Modifier.height(4.dp))
        Text("This portal is intended for non-emergency municipal civic issues (roads, lighting, water, sanitation). For emergency services, contact 112.", style = MaterialTheme.typography.bodySmall, color = ext.muted)
    }
}

@Composable
private fun SignupEmailVerificationPending(
    email: String,
    devToken: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onVerifyToken: (String) -> Unit,
    onResend: () -> Unit,
    onNavigateLogin: () -> Unit,
    ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors
) {
    var tokenInput by remember(devToken) { mutableStateOf(devToken ?: "") }

    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Text("✉️ VERIFY YOUR EMAIL ADDRESS", style = MaterialTheme.typography.labelSmall, color = ext.green, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Check your email inbox", style = MaterialTheme.typography.titleMedium, color = ext.ink, fontWeight = FontWeight.Bold)
        Text(
            text = "An official verification link has been dispatched to $email via Gmail API (arshitgupta34@gmail.com) to confirm that your email address is valid.",
            style = MaterialTheme.typography.bodySmall,
            color = ext.muted
        )

        Spacer(modifier = Modifier.height(18.dp))
        Text("Verification Token", style = MaterialTheme.typography.labelMedium, color = ext.ink)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            placeholder = { Text("Enter verification token") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            singleLine = true
        )

        errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(14.dp))
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

        Spacer(modifier = Modifier.height(20.dp))

        TheekHaiButton(
            text = if (isLoading) "Verifying..." else "Verify Email & Activate Account →",
            onClick = { onVerifyToken(tokenInput) },
            modifier = Modifier.fillMaxWidth(),
            enabled = tokenInput.isNotBlank() && !isLoading
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Resend Link",
                style = MaterialTheme.typography.bodySmall,
                color = ext.green,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onResend() }
            )
            Text(
                text = "Back to Sign In →",
                style = MaterialTheme.typography.bodySmall,
                color = ext.muted,
                modifier = Modifier.clickable { onNavigateLogin() }
            )
        }
    }
}
