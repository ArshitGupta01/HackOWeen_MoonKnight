package com.example.theekhai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.presentation.admin.CommandCenterRoute
import com.example.theekhai.presentation.admin.CommandCenterViewModel
import com.example.theekhai.presentation.fieldworker.FieldWorkerRoute
import com.example.theekhai.presentation.fieldworker.FieldWorkerViewModel
import org.koin.compose.viewmodel.koinViewModel

enum class AppRole {
    COMMAND_CENTER,
    FIELD_WORKER
}

@Composable
fun App() {
    var currentRole by remember { mutableStateOf(AppRole.COMMAND_CENTER) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0F172A),
            surface = Color(0xFF1E293B),
            primary = Color(0xFF06B6D4)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
                .safeContentPadding()
        ) {
            // Global App Role Switcher Bar
            Surface(
                color = Color(0xFF111827),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "THEEKHAI",
                            color = Color(0xFF38BDF8),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Smart Cities Redressal",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { currentRole = AppRole.COMMAND_CENTER },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentRole == AppRole.COMMAND_CENTER) Color(0xFF4F46E5) else Color(0xFF1F2937),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("🏢 Command Center (Desktop)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { currentRole = AppRole.FIELD_WORKER },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentRole == AppRole.FIELD_WORKER) Color(0xFF059669) else Color(0xFF1F2937),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("👷 Field Worker (Android)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (currentRole) {
                    AppRole.COMMAND_CENTER -> {
                        val commandCenterViewModel = koinViewModel<CommandCenterViewModel>()
                        CommandCenterRoute(commandCenterViewModel)
                    }
                    AppRole.FIELD_WORKER -> {
                        val fieldWorkerViewModel = koinViewModel<FieldWorkerViewModel>()
                        FieldWorkerRoute(fieldWorkerViewModel)
                    }
                }
            }
        }
    }
}
