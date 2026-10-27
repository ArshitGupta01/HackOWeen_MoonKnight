package com.example.theekhai.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.navigation.AppNavigator
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors

@Composable
fun HomeScreen() {
    val navigator = LocalNavigator.current
    val ext = LocalTheekHaiColors.current
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 850.dp

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // ── Hero Section ──
                if (isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp)
                            .padding(top = 48.dp, bottom = 36.dp),
                        horizontalArrangement = Arrangement.spacedBy(42.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1.1f)) {
                            HeroCopy(navigator = navigator, ext = ext, isWide = true)
                        }
                        Box(modifier = Modifier.weight(0.9f)) {
                            CityIllustration(ext = ext)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        HeroCopy(navigator = navigator, ext = ext, isWide = false)
                        CityIllustration(ext = ext)
                    }
                }

                TheekHaiDivider()

            // ── Promise Strip (Numbered Steps) ──
            val promisePadding = if (isWide) 48.dp else 20.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = promisePadding, vertical = 42.dp)
            ) {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp)
                    ) {
                        PromiseStep(
                            number = "01",
                            title = "Tell us what happened",
                            body = "Voice note in your language, photo proof, or quick text. We log consent and DPDP verification instantly.",
                            modifier = Modifier.weight(1f)
                        )
                        PromiseStep(
                            number = "02",
                            title = "AI triage & auto dispatch",
                            body = "Gemini 3.8 Flash classifies department, urgency, and statutory SLA timeline under municipal rules.",
                            modifier = Modifier.weight(1f)
                        )
                        PromiseStep(
                            number = "03",
                            title = "Independent verified closure",
                            body = "TinyFish autonomous web agents and field workers corroborate fix photos before case is marked resolved.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        PromiseStep(
                            number = "01",
                            title = "Tell us what happened",
                            body = "Voice note in your language, photo proof, or quick text. We log consent and DPDP verification instantly."
                        )
                        PromiseStep(
                            number = "02",
                            title = "AI triage & auto dispatch",
                            body = "Gemini 3.8 Flash classifies department, urgency, and statutory SLA timeline under municipal rules."
                        )
                        PromiseStep(
                            number = "03",
                            title = "Independent verified closure",
                            body = "TinyFish autonomous web agents and field workers corroborate fix photos before case is marked resolved."
                        )
                    }
                }
            }

            TheekHaiDivider()

            // ── Editorial / Reference Options Strip ──
            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp, vertical = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Eyebrow("ONE SHARED RESPONSIBILITY")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Less wondering.\nMore knowing.",
                            style = MaterialTheme.typography.headlineLarge,
                            color = ext.ink,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Your report stays connected to the people, tasks and evidence behind its resolution. Every action is recorded in the tamper-evident audit trail.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.muted,
                            lineHeight = 22.sp
                        )
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = ext.sidebarBg,
                        shape = RoundedCornerShape(6.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Column(modifier = Modifier.padding(28.dp)) {
                            Eyebrow("UNDERSTAND YOUR OPTIONS")
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Clear guidance, with\nthe source attached.",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ext.ink,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Explore approved civic guidance and statutory appeal mechanisms (AAS, DPDP). Know what is available and when it was checked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.muted,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.clickable { navigator.navigateTo(Route.Library) },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Open the reference library  ↗",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.W600,
                                    color = ext.green
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column {
                        Eyebrow("ONE SHARED RESPONSIBILITY")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Less wondering. More knowing.",
                            style = MaterialTheme.typography.headlineSmall,
                            color = ext.ink,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Your report stays connected to the people, tasks and evidence behind its resolution.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.muted
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ext.sidebarBg,
                        shape = RoundedCornerShape(6.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Eyebrow("UNDERSTAND YOUR OPTIONS")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Clear guidance, with the source attached.",
                                style = MaterialTheme.typography.titleMedium,
                                color = ext.ink
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Explore approved civic guidance and statutory appeal mechanisms.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.muted
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Open the reference library  ↗",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.W600,
                                color = ext.green,
                                modifier = Modifier.clickable { navigator.navigateTo(Route.Library) }
                            )
                        }
                    }
                }
            }

                Spacer(modifier = Modifier.height(40.dp))
            }

            // Mobile Floating Action Button to Report
            if (!isWide) {
                FloatingActionButton(
                    onClick = { navigator.navigateTo(Route.Report) },
                    containerColor = ext.green,
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Report an issue")
                }
            }
        }
    }
}

@Composable
private fun HeroCopy(
    navigator: AppNavigator,
    ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors,
    isWide: Boolean
) {
    Column {
        Eyebrow("SMALL REPORTS. MEANINGFUL CHANGE.")
        Spacer(modifier = Modifier.height(if (isWide) 16.dp else 10.dp))

        Text(
            text = buildAnnotatedString {
                append("A better city\nstarts with ")
                withStyle(SpanStyle(color = Color(0xFF4D7563), fontStyle = FontStyle.Italic)) {
                    append("you.")
                }
            },
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = if (isWide) 50.sp else 32.sp,
                lineHeight = if (isWide) 56.sp else 38.sp,
                letterSpacing = (-1.0).sp,
            ),
            color = ext.ink,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(if (isWide) 16.dp else 10.dp))

        Text(
            text = "The broken streetlight. The overflowing drain. The road that needs care. Report it, follow the work, and help make it right.",
            style = if (isWide) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = ext.muted,
            lineHeight = if (isWide) 24.sp else 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TheekHaiButton(
                text = "Report a neighbourhood issue  ↗",
                onClick = { navigator.navigateTo(Route.Report) }
            )
            Text(
                text = "Follow your report  →",
                fontSize = 13.sp,
                fontWeight = FontWeight.W600,
                color = ext.green,
                modifier = Modifier
                    .clickable { navigator.navigateTo(Route.Workspace) }
                    .padding(vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "A clear owner. A visible deadline. A resolution you can question.",
            style = MaterialTheme.typography.bodySmall,
            color = ext.muted
        )
    }
}

@Composable
private fun CityIllustration(ext: com.example.theekhai.ui.theme.TheekHaiExtendedColors) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp, max = 420.dp)
            .clip(RoundedCornerShape(topStart = 60.dp, topEnd = 6.dp, bottomStart = 6.dp, bottomEnd = 6.dp)),
        color = Color(0xFFE9E9DC),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(28.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    CityBlock(color = Color(0xFFF6F0DF))
                    CityBlock(color = Color(0xFFC8D2B9))
                    CityBlock(color = Color(0xFFF6F0DF))
                }

                Surface(
                    modifier = Modifier.size(60.dp),
                    color = TheekHaiColors.Green,
                    shape = RoundedCornerShape(50)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✓", color = Color(0xFFFAF8ED), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    CityBlock(color = Color(0xFFD1BBA0))
                    CityBlock(color = Color(0xFFB8C1B4))
                    CityBlock(color = Color(0xFFD1BBA0))
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color(0xFFFCFBF3).copy(alpha = 0.94f),
                shape = RoundedCornerShape(4.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DotIndicator()
                    Text(
                        text = "Pilot goal: verify resolutions with field evidence before closure",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W600,
                        color = ext.ink
                    )
                }
            }
        }
    }
}

@Composable
private fun CityBlock(color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(width = 54.dp, height = 50.dp),
        color = color,
        shape = RoundedCornerShape(3.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
    ) {}
}
