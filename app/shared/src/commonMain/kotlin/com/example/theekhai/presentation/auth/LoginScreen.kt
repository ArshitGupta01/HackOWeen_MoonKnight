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

@Composable
fun LoginScreen() {
    val navigator = LocalNavigator.current
    val ext = LocalTheekHaiColors.current
    val scrollState = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 850.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(if (isWide) 48.dp else 20.dp)
        ) {
            // Heading
            Eyebrow("CITIZEN ACCESS")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Welcome to TheekHai",
                style = MaterialTheme.typography.headlineLarge,
                color = ext.ink,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Sign in to report neighbourhood issues, track repair progress, and verify resolutions.",
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
                        LoginForm(
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible },
                            isLoading = isLoading,
                            errorMessage = errorMessage,
                            onSubmit = {
                                errorMessage = "Sign-in is not connected in this native preview. No account was authenticated."
                            },
                            onNavigateSignup = { navigator.navigateTo(Route.Signup) },
                            onSelectDemo = { _, selectedEmail ->
                                email = selectedEmail
                                password = ""
                            },
                            ext = ext
                        )
                    }

                    Column(modifier = Modifier.weight(0.9f)) {
                        SecurityInfoPanel(ext = ext)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    LoginForm(
                        email = email,
                        onEmailChange = { email = it },
                        password = password,
                        onPasswordChange = { password = it },
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible },
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onSubmit = {
                            errorMessage = "Sign-in is not connected in this native preview. No account was authenticated."
                        },
                        onNavigateSignup = { navigator.navigateTo(Route.Signup) },
                        onSelectDemo = { _, selectedEmail ->
                            email = selectedEmail
                            password = ""
                        },
                        ext = ext
                    )

                    SecurityInfoPanel(ext = ext)
                }
            }
        }
    }
}

@Composable
private fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePassword: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    onSubmit: () -> Unit,
    onNavigateSignup: () -> Unit,
    onSelectDemo: (role: String, email: String) -> Unit,
    ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors
) {
    Column {
        PanelCard(modifier = Modifier.fillMaxWidth()) {
            Text("Email address", style = MaterialTheme.typography.labelMedium, color = ext.ink)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                placeholder = { Text("you@example.com") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Password", style = MaterialTheme.typography.labelMedium, color = ext.ink)
                Text(
                    text = if (passwordVisible) "Hide" else "Show",
                    fontSize = 12.sp,
                    color = ext.green,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onTogglePassword() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                placeholder = { Text("Enter your password") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
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

            Spacer(modifier = Modifier.height(24.dp))

            TheekHaiButton(
                text = if (isLoading) "Signing in..." else "Sign in",
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && password.isNotBlank() && !isLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New to TheekHai? ", style = MaterialTheme.typography.bodySmall, color = ext.muted)
                Text(
                    text = "Create an account →",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.green,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateSignup() }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Demo test accounts
        Surface(
            color = ext.sidebarBg,
            shape = RoundedCornerShape(6.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Demo account examples", style = MaterialTheme.typography.titleSmall, color = ext.ink)
                Text(
                    "These examples only fill the email field. Native sign-in is not connected; no account will be authenticated.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickRoleButton("Citizen", "citizen@theekhai.in", Modifier.weight(1f), onSelectDemo)
                    QuickRoleButton("Officer", "officer@theekhai.in", Modifier.weight(1f), onSelectDemo)
                    QuickRoleButton("Inspector", "inspector@theekhai.in", Modifier.weight(1f), onSelectDemo)
                }
            }
        }
    }
}

@Composable
private fun QuickRoleButton(
    role: String,
    email: String,
    modifier: Modifier = Modifier,
    onSelect: (role: String, email: String) -> Unit
) {
    val ext = LocalTheekHaiColors.current
    Surface(
        modifier = modifier.clickable { onSelect(role, email) },
        color = ext.paper,
        shape = RoundedCornerShape(4.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(role, style = MaterialTheme.typography.labelMedium, color = ext.ink)
        }
    }
}

@Composable
private fun SecurityInfoPanel(ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors) {
    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Eyebrow("TRUST & VERIFICATION")
        Spacer(modifier = Modifier.height(10.dp))
        Text("Accountable civic governance.", style = MaterialTheme.typography.headlineSmall, color = ext.ink, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Every report filed through TheekHai receives an official tracking number, assigned municipal department, and statutory SLA deadline.",
            style = MaterialTheme.typography.bodyMedium,
            color = ext.muted,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))
        TheekHaiDivider()
        Spacer(modifier = Modifier.height(18.dp))

        PillarItem("Strict Data Privacy", "Personal contact details are shared only with the designated handling officer under data protection standards.")
        Spacer(modifier = Modifier.height(14.dp))
        PillarItem("Transparent Audit Trail", "All decisions, inspection notes, and work completion photos are documented and open to citizen review.")
        Spacer(modifier = Modifier.height(14.dp))
        PillarItem("Citizen Right to Challenge", "A case cannot be closed without verified photo evidence. If unresolved, citizens can request independent re-inspection.")
    }
}

@Composable
private fun PillarItem(title: String, desc: String) {
    val ext = LocalTheekHaiColors.current
    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, color = ext.ink)
        Spacer(modifier = Modifier.height(4.dp))
        Text(desc, style = MaterialTheme.typography.bodySmall, color = ext.muted, lineHeight = 18.sp)
    }
}
