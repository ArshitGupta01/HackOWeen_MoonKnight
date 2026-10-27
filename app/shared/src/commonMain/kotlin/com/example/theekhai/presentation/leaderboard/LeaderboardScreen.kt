package com.example.theekhai.presentation.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.contract.LeaderboardEntryDto
import com.example.theekhai.presentation.nativeconnect.NativeConnectViewModel
import com.example.theekhai.ui.components.*
import com.example.theekhai.ui.localization.t
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LeaderboardScreen() {
    val viewModel = koinViewModel<NativeConnectViewModel>()
    val state by viewModel.state.collectAsState()
    val ext = LocalTheekHaiColors.current
    var selectedFilter by remember { mutableStateOf("ALL") }

    LaunchedEffect(selectedFilter) {
        viewModel.loadLeaderboard(selectedFilter)
    }

    val board = state.leaderboard
    val entries = board?.entries ?: emptyList()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth >= 850.dp
        val pad = if (isWide) 48.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(ext.paper)
                .padding(horizontal = pad, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Eyebrow(t("CIVIC ACCOUNTABILITY LEADERBOARD", "नागरिक जवाबदेही लीडरबोर्ड"))
                Spacer(Modifier.height(6.dp))
                Text(
                    text = t("Recognizing genuine reports & decisive action.", "सच्ची शिकायतों और त्वरित कार्रवाई का सम्मान।"),
                    style = if (isWide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ext.ink
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = t(
                        "Regular users rank up when their complaints are verified genuine and approved by authorities (+35 to +85 pts). Authorities earn points for reviewing, resolving, and approving civic issues (+15 to +40 pts).",
                        "जब नागरिकों की शिकायतें सच्ची पाई जाती हैं और स्वीकृत होती हैं तो उनके अंक बढ़ते हैं। अधिकारियों को समस्याओं के समाधान और सत्यापन पर अंक मिलते हैं।"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.muted
                )
            }

            // Filter Pills
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "ALL" to t("All Ranked", "सभी प्रतिभागी"),
                        "CITIZEN" to t("Regular Citizens", "नागरिक"),
                        "AUTHORITY" to t("Authorities", "अधिकारी")
                    ).forEach { (code, label) ->
                        val active = selectedFilter == code
                        TheekHaiButton(
                            text = label,
                            onClick = { selectedFilter = code },
                            isSecondary = !active
                        )
                    }
                }
            }

            if (state.isLoading && board == null) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ext.green)
                }
            }

            state.message?.let { msg ->
                item {
                    Surface(color = ext.greenSurface, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(msg, color = ext.green, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
            }

            // Summary Metrics Row
            if (board != null) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard(t("PARTICIPANTS", "प्रतिभागी"), board.totalParticipants.toString(), ext.ink, Modifier.weight(1f))
                        MetricCard(t("TOTAL POINTS", "कुल अंक"), board.totalPointsAwarded.toString(), ext.green, Modifier.weight(1f))
                        MetricCard(
                            t("APPROVED CASES", "स्वीकृत मामले"),
                            entries.sumOf { it.complaintsApprovedCount }.toString(),
                            TheekHaiColors.Amber,
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            // Ranked List Header
            item {
                Text(
                    text = t("Official Standings", "आधिकारिक रैंकिंग तालिका"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ext.ink
                )
            }

            items(entries, key = { it.userId }) { entry ->
                LeaderboardRowCard(
                    entry = entry,
                    isCurrentUser = state.user?.email == entry.email || state.profile?.email == entry.email,
                    onInstantSwitch = { viewModel.instantSignIn(entry.email) }
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRowCard(
    entry: LeaderboardEntryDto,
    isCurrentUser: Boolean,
    onInstantSwitch: () -> Unit
) {
    val ext = LocalTheekHaiColors.current
    val medalColor = when (entry.rank) {
        1 -> Color(0xFFD97706)
        2 -> Color(0xFF475569)
        3 -> Color(0xFFB45309)
        else -> ext.green
    }

    PanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = medalColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#${entry.rank}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = medalColor
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = entry.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ext.ink
                        )
                        StatusChip(
                            label = if (entry.userType == "AUTHORITY") entry.role else "CITIZEN",
                            containerColor = if (entry.userType == "AUTHORITY") TheekHaiColors.StatusClosed else TheekHaiColors.StatusDefault,
                            contentColor = if (entry.userType == "AUTHORITY") TheekHaiColors.StatusClosedText else TheekHaiColors.StatusDefaultText
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${entry.badge} • ${entry.department ?: entry.ward}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.muted
                    )
                    Spacer(Modifier.height(2.dp))
                    val statsDetail = if (entry.userType == "AUTHORITY") {
                        "${entry.actionsTakenCount} authority actions completed"
                    } else {
                        "${entry.complaintsApprovedCount} approved / ${entry.complaintsSubmittedCount} submitted complaints"
                    }
                    Text(
                        text = statsDetail,
                        fontSize = 11.sp,
                        color = ext.green,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.score} pts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ext.green
                )
                Spacer(Modifier.height(4.dp))
                if (isCurrentUser) {
                    Text("Signed In ✓", fontSize = 11.sp, color = ext.green, fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        text = "Instant Sign-In →",
                        fontSize = 11.sp,
                        color = ext.amber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onInstantSwitch() }
                    )
                }
            }
        }
    }
}
