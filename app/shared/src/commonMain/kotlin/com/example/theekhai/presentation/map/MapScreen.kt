package com.example.theekhai.presentation.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.ui.components.Eyebrow
import com.example.theekhai.ui.components.StatusChip
import com.example.theekhai.ui.components.TheekHaiButton
import com.example.theekhai.ui.components.TheekHaiDivider
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors

data class MapCivicPin(
    val id: String,
    val title: String,
    val category: String,
    val status: String,
    val address: String,
    val reportsCount: Int,
    val relX: Float, // 0.0 to 1.0
    val relY: Float  // 0.0 to 1.0
)

@Composable
fun MapScreen() {
    val navigator = LocalNavigator.current
    val ext = LocalTheekHaiColors.current

    val pins = remember {
        listOf(
            MapCivicPin("c-001", "Damaged streetlight fixture", "LIGHTING", "OPEN", "5th Cross Road, Ward 4", 14, 0.28f, 0.35f),
            MapCivicPin("c-002", "Overflowing drainage line", "WATER", "IN_PROGRESS", "Behind Community Center", 23, 0.65f, 0.42f),
            MapCivicPin("c-003", "Deep pothole causing accidents", "ROADS", "URGENT", "12th Main Road Junction", 41, 0.45f, 0.68f),
            MapCivicPin("c-004", "Uncollected garbage dump", "WASTE", "PENDING", "Sector 4 Market Gate", 8, 0.78f, 0.22f),
            MapCivicPin("c-005", "Cleared stormwater canal", "WATER", "RESOLVED", "South Canal Road", 19, 0.32f, 0.80f),
        )
    }

    var selectedPin by remember { mutableStateOf<MapCivicPin?>(pins[2]) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val categories = listOf("ALL", "ROADS", "LIGHTING", "WATER", "WASTE")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ext.paper)
        ) {
            // Header: Ward Context & Title
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Eyebrow("CIVIC GEOSPATIAL RADAR")
                        Text(
                            text = "Ward 4 • Central Zone",
                            style = MaterialTheme.typography.titleLarge,
                            color = ext.ink,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = TheekHaiColors.GreenSurface,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = ext.green,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "28 Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Surface(
                            modifier = Modifier.clickable { selectedCategoryFilter = cat },
                            color = if (isSelected) ext.green else ext.sidebarBg,
                            shape = RoundedCornerShape(16.dp),
                            border = if (!isSelected) ButtonDefaults.outlinedButtonBorder(enabled = true) else null
                        ) {
                            Text(
                                text = if (cat == "ALL") "All Issues" else cat,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else ext.muted,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            TheekHaiDivider()

            // Interactive Map Visual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFE8ECE5))
            ) {
                // Map Canvas Representation: roads, zones, water channel
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid streets
                    val streetColor = Color(0xFFD6DDD1)
                    val mainRoadColor = Color(0xFFC7D1C1)

                    // Background blocks
                    drawRect(color = Color(0xFFEFF3EC), size = Size(w, h))

                    // Green civic park zone
                    drawRoundRect(
                        color = Color(0xFFD2E0CD),
                        topLeft = Offset(w * 0.15f, h * 0.12f),
                        size = Size(w * 0.35f, h * 0.28f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )

                    // Secondary residential zone
                    drawRoundRect(
                        color = Color(0xFFE3EADF),
                        topLeft = Offset(w * 0.58f, h * 0.55f),
                        size = Size(w * 0.38f, h * 0.35f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )

                    // Main Arterial Road (horizontal)
                    drawLine(
                        color = mainRoadColor,
                        start = Offset(0f, h * 0.48f),
                        end = Offset(w, h * 0.48f),
                        strokeWidth = 24f
                    )
                    // Road center dashed line
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, h * 0.48f),
                        end = Offset(w, h * 0.48f),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                    )

                    // Vertical Main Avenue
                    drawLine(
                        color = mainRoadColor,
                        start = Offset(w * 0.52f, 0f),
                        end = Offset(w * 0.52f, h),
                        strokeWidth = 22f
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.52f, 0f),
                        end = Offset(w * 0.52f, h),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                    )

                    // Diagonal connecting street
                    drawLine(
                        color = streetColor,
                        start = Offset(0f, h * 0.2f),
                        end = Offset(w * 0.8f, h),
                        strokeWidth = 14f
                    )
                }

                // Interactive Pins
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val mapWidth = maxWidth
                    val mapHeight = maxHeight

                    pins.forEach { pin ->
                        if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == pin.category) {
                            val isPinSelected = selectedPin?.id == pin.id
                            val pinColor = when (pin.status) {
                                "URGENT" -> Color(0xFFEF4444)
                                "OPEN", "PENDING" -> Color(0xFFF59E0B)
                                "IN_PROGRESS" -> Color(0xFF0284C7)
                                "RESOLVED" -> Color(0xFF10B981)
                                else -> Color(0xFF64748B)
                            }

                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = mapWidth * pin.relX - 18.dp,
                                        y = mapHeight * pin.relY - 18.dp
                                    )
                                    .clickable { selectedPin = pin }
                            ) {
                                Surface(
                                    modifier = Modifier.size(if (isPinSelected) 38.dp else 30.dp),
                                    shape = CircleShape,
                                    color = pinColor,
                                    shadowElevation = if (isPinSelected) 8.dp else 3.dp,
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = when (pin.category) {
                                                "ROADS" -> "🛣"
                                                "LIGHTING" -> "💡"
                                                "WATER" -> "💧"
                                                "WASTE" -> "🗑"
                                                else -> "📍"
                                            },
                                            fontSize = if (isPinSelected) 16.sp else 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Selected Pin Floating Card
                selectedPin?.let { pin ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .fillMaxWidth(),
                        color = ext.paper,
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 6.dp,
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusChip(label = pin.status)
                                Text(
                                    text = "👥 ${pin.reportsCount} residents confirmed",
                                    fontSize = 11.sp,
                                    color = ext.muted
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = pin.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ext.ink
                            )

                            Spacer(modifier = Modifier.height(3.dp))

                            Text(
                                text = "📍 ${pin.address}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.muted
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TheekHaiButton(
                                    text = "View Case ↗",
                                    onClick = { navigator.navigateTo(Route.CaseDetail(pin.id)) },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { navigator.navigateTo(Route.Report) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Report Here", fontSize = 12.sp, color = ext.ink)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to report at user location
        FloatingActionButton(
            onClick = { navigator.navigateTo(Route.Report) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 140.dp, end = 16.dp),
            containerColor = ext.green,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Report issue at current location")
        }
    }
}
