package com.example.theekhai.presentation.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.ui.components.Eyebrow
import com.example.theekhai.ui.components.StatusChip
import com.example.theekhai.ui.components.TheekHaiButton
import com.example.theekhai.ui.components.TheekHaiDivider
import com.example.theekhai.ui.localization.t
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors
import org.jetbrains.compose.resources.painterResource
import theekhai.app.shared.generated.resources.Res
import theekhai.app.shared.generated.resources.broken_streetlight
import kotlin.math.roundToInt

data class MapCivicPin(
    val id: String,
    val title: String,
    val category: String,
    val status: String,
    val address: String,
    val reportsCount: Int,
    val timeAgo: String,
    val mapX: Float, // coordinate on 2000x2000 canvas
    val mapY: Float
)

@Composable
fun MapScreen() {
    val navigator = LocalNavigator.current
    val ext = LocalTheekHaiColors.current

    // Expanded authentic pins across the enlarged ward map
    val pins = remember {
        listOf(
            MapCivicPin("c-003", "Broken Streetlight on 5th Cross Road", "LIGHTING", "IN_PROGRESS", "5th Cross Road, Ward 4", 14, "Yesterday", 560f, 620f),
            MapCivicPin("c-001", "Deep pothole causing accidents", "ROADS", "URGENT", "12th Main Road Junction", 41, "2h ago", 1020f, 1020f),
            MapCivicPin("c-002", "Sewage drain overflow near Market", "WATER", "OPEN", "Sector 14 Main Market Lane", 23, "5h ago", 1450f, 780f),
            MapCivicPin("c-004", "Uncollected garbage near Metro Gate", "WASTE", "PENDING", "Sector 14 Metro Station", 18, "1d ago", 1380f, 1380f),
            MapCivicPin("c-005", "Resurfaced road & asphalt seal", "ROADS", "RESOLVED", "Community Center Avenue", 32, "3d ago", 420f, 1420f),
            MapCivicPin("c-006", "Flickering high-mast light at Park", "LIGHTING", "OPEN", "Central Park East Gate", 9, "4h ago", 820f, 440f),
            MapCivicPin("c-007", "Drinking water pipeline leakage", "WATER", "IN_PROGRESS", "Govt School Link Road", 16, "1d ago", 650f, 1150f)
        )
    }

    var selectedPin by remember { mutableStateOf<MapCivicPin?>(pins[0]) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }

    val categories = listOf("ALL", "LIGHTING", "ROADS", "WATER", "WASTE")

    // Enlarged map canvas dimensions (2000 x 2000 px = 100%+ enlargement)
    val mapCanvasWidth = 2000f
    val mapCanvasHeight = 2000f

    // Interactive pan and zoom state
    var zoomScale by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset(-250f, -250f)) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ext.paper)
        ) {
            // ── Top Header: Ward Context & Category Filters ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
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
                                text = t("Sample map", "नमूना मानचित्र"),
                                style = MaterialTheme.typography.labelSmall,
                                color = ext.green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = t(
                        "Illustrative preview only. Pins and counts are sample data, not live civic reports.",
                        "यह केवल उदाहरण है। पिन और संख्या नमूना डेटा हैं, लाइव नागरिक रिपोर्ट नहीं।"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.muted
                )

                Spacer(modifier = Modifier.height(8.dp))

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

            // ── 100% Enlarged Pannable & Zoomable Map Canvas ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(0.dp))
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            zoomScale = (zoomScale * zoom).coerceIn(0.6f, 2.8f)
                            panOffset += pan
                        }
                    }
            ) {
                // Background & Rich Map Canvas
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val scale = zoomScale
                    val ox = panOffset.x
                    val oy = panOffset.y

                    // Helper functions to map coordinates
                    fun tx(x: Float) = x * scale + ox
                    fun ty(y: Float) = y * scale + oy
                    fun ts(v: Float) = v * scale

                    // 1. Base Neighborhood Ground
                    drawRect(color = Color(0xFFF1F4EE))

                    // 2. Central Community Park & Lake (Lush green zone)
                    drawRoundRect(
                        color = Color(0xFFD4E6CD),
                        topLeft = Offset(tx(300f), ty(200f)),
                        size = Size(ts(650f), ts(550f)),
                        cornerRadius = CornerRadius(ts(24f), ts(24f))
                    )
                    // Inner Park Lake
                    drawOval(
                        color = Color(0xFFBFDDF3),
                        topLeft = Offset(tx(420f), ty(320f)),
                        size = Size(ts(220f), ts(150f))
                    )
                    // Park Jogging Track (dashed circular path)
                    drawRoundRect(
                        color = Color(0xFFC1D8BA),
                        topLeft = Offset(tx(340f), ty(240f)),
                        size = Size(ts(570f), ts(470f)),
                        cornerRadius = CornerRadius(ts(20f), ts(20f)),
                        style = Stroke(width = ts(6f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
                    )

                    // 3. Educational & Health Zones
                    // Primary Health Center (PHC)
                    drawRoundRect(
                        color = Color(0xFFE5EDE0),
                        topLeft = Offset(tx(1200f), ty(220f)),
                        size = Size(ts(450f), ts(380f)),
                        cornerRadius = CornerRadius(ts(16f), ts(16f))
                    )
                    // Govt Model School
                    drawRoundRect(
                        color = Color(0xFFE2EBE0),
                        topLeft = Offset(tx(280f), ty(1050f)),
                        size = Size(ts(480f), ts(420f)),
                        cornerRadius = CornerRadius(ts(16f), ts(16f))
                    )

                    // 4. Sector 14 Main Market Complex & Commercial Hub
                    drawRoundRect(
                        color = Color(0xFFEDE8DA),
                        topLeft = Offset(tx(1250f), ty(720f)),
                        size = Size(ts(520f), ts(460f)),
                        cornerRadius = CornerRadius(ts(16f), ts(16f))
                    )

                    // 5. Sector 14 Metro Station & Transport Hub
                    drawRoundRect(
                        color = Color(0xFFDDE6E8),
                        topLeft = Offset(tx(1150f), ty(1300f)),
                        size = Size(ts(580f), ts(480f)),
                        cornerRadius = CornerRadius(ts(16f), ts(16f))
                    )

                    // 6. Stormwater Canal (Nallah) - diagonal curving water channel
                    val canalPath = Path().apply {
                        moveTo(tx(100f), ty(850f))
                        cubicTo(
                            tx(500f), ty(880f),
                            tx(850f), ty(940f),
                            tx(1900f), ty(1150f)
                        )
                    }
                    drawPath(
                        path = canalPath,
                        color = Color(0xFFB0D5EB),
                        style = Stroke(width = ts(28f))
                    )
                    drawPath(
                        path = canalPath,
                        color = Color(0xFF86B9D6),
                        style = Stroke(width = ts(32f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f)))
                    )

                    // 7. Arterial & Secondary Road Network
                    val roadColor = Color(0xFFCBD5C5)
                    val mainRoadColor = Color(0xFFB8C5B2)

                    // Road 1: 12th Main Arterial Road (Horizontal)
                    drawRect(
                        color = mainRoadColor,
                        topLeft = Offset(tx(0f), ty(980f)),
                        size = Size(ts(mapCanvasWidth), ts(80f))
                    )
                    // Road 1 center yellow divider
                    drawLine(
                        color = Color(0xFFFBBF24),
                        start = Offset(tx(0f), ty(1020f)),
                        end = Offset(tx(mapCanvasWidth), ty(1020f)),
                        strokeWidth = ts(4f)
                    )
                    // Road 1 lane dashes
                    drawLine(
                        color = Color.White,
                        start = Offset(tx(0f), ty(1000f)),
                        end = Offset(tx(mapCanvasWidth), ty(1000f)),
                        strokeWidth = ts(2f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 16f))
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(tx(0f), ty(1040f)),
                        end = Offset(tx(mapCanvasWidth), ty(1040f)),
                        strokeWidth = ts(2f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 16f))
                    )

                    // Road 2: Central Boulevard (Vertical)
                    drawRect(
                        color = mainRoadColor,
                        topLeft = Offset(tx(980f), ty(0f)),
                        size = Size(ts(80f), ts(mapCanvasHeight))
                    )
                    drawLine(
                        color = Color(0xFFFBBF24),
                        start = Offset(tx(1020f), ty(0f)),
                        end = Offset(tx(1020f), ty(mapCanvasHeight)),
                        strokeWidth = ts(4f)
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(tx(1000f), ty(0f)),
                        end = Offset(tx(1000f), ty(mapCanvasHeight)),
                        strokeWidth = ts(2f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 16f))
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(tx(1040f), ty(0f)),
                        end = Offset(tx(1040f), ty(mapCanvasHeight)),
                        strokeWidth = ts(2f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 16f))
                    )

                    // Road 3: 5th Cross Road (Upper horizontal)
                    drawRect(
                        color = roadColor,
                        topLeft = Offset(tx(0f), ty(580f)),
                        size = Size(ts(mapCanvasWidth), ts(48f))
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(tx(0f), ty(604f)),
                        end = Offset(tx(mapCanvasWidth), ty(604f)),
                        strokeWidth = ts(2f),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))
                    )

                    // Road 4: Sector 14 Avenue (Lower horizontal)
                    drawRect(
                        color = roadColor,
                        topLeft = Offset(tx(0f), ty(1500f)),
                        size = Size(ts(mapCanvasWidth), ts(48f))
                    )

                    // Road 5: West Colony Road (Left vertical)
                    drawRect(
                        color = roadColor,
                        topLeft = Offset(tx(220f), ty(0f)),
                        size = Size(ts(48f), ts(mapCanvasHeight))
                    )

                    // Road 6: Metro Link Avenue (Right vertical)
                    drawRect(
                        color = roadColor,
                        topLeft = Offset(tx(1720f), ty(0f)),
                        size = Size(ts(48f), ts(mapCanvasHeight))
                    )

                    // Central Junction Roundabout
                    drawCircle(
                        color = mainRoadColor,
                        radius = ts(90f),
                        center = Offset(tx(1020f), ty(1020f))
                    )
                    drawCircle(
                        color = Color(0xFFC7D9C0),
                        radius = ts(48f),
                        center = Offset(tx(1020f), ty(1020f))
                    )
                    drawCircle(
                        color = Color(0xFF9FB996),
                        radius = ts(18f),
                        center = Offset(tx(1020f), ty(1020f))
                    )

                    // Pedestrian Crosswalks (Zebra Stripes at 12th Main & Central Ave)
                    for (i in 0..6) {
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(tx(900f + i * 14f), ty(990f)),
                            size = Size(ts(8f), ts(60f))
                        )
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(tx(1070f + i * 14f), ty(990f)),
                            size = Size(ts(8f), ts(60f))
                        )
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(tx(990f), ty(900f + i * 14f)),
                            size = Size(ts(60f), ts(8f))
                        )
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(tx(990f), ty(1070f + i * 14f)),
                            size = Size(ts(60f), ts(8f))
                        )
                    }
                }

                // Landmark Labels on Map Canvas
                pins.forEach { _ -> /* rendered via transformed composables below */ }

                // Interactive Dynamic Pins
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

                        val screenX = (pin.mapX * zoomScale + panOffset.x).roundToInt()
                        val screenY = (pin.mapY * zoomScale + panOffset.y).roundToInt()

                        Box(
                            modifier = Modifier
                                .offset { IntOffset(screenX - 22, screenY - 22) }
                                .clickable { selectedPin = pin }
                        ) {
                            // Pulsating outer aura when selected
                            if (isPinSelected) {
                                Surface(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .offset((-5).dp, (-5).dp),
                                    shape = CircleShape,
                                    color = pinColor.copy(alpha = 0.25f)
                                ) {}
                            }

                            // Main Pin Button
                            Surface(
                                modifier = Modifier.size(if (isPinSelected) 44.dp else 34.dp),
                                shape = CircleShape,
                                color = pinColor,
                                shadowElevation = if (isPinSelected) 10.dp else 4.dp,
                                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = when (pin.category) {
                                            "LIGHTING" -> "💡"
                                            "ROADS" -> "🛣"
                                            "WATER" -> "💧"
                                            "WASTE" -> "🗑"
                                            else -> "📍"
                                        },
                                        fontSize = if (isPinSelected) 18.sp else 13.sp
                                    )
                                }
                            }

                            // Small Citizen Count Badge
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-4).dp),
                                shape = CircleShape,
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = pin.reportsCount.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // ── Map Floating Navigation Controls (Zoom In, Zoom Out, Center) ──
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledIconButton(
                        onClick = { zoomScale = (zoomScale * 1.25f).coerceAtMost(2.8f) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = ext.paper,
                            contentColor = ext.ink
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
                    }

                    FilledIconButton(
                        onClick = { zoomScale = (zoomScale / 1.25f).coerceAtLeast(0.6f) },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = ext.paper,
                            contentColor = ext.ink
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Text("−", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ext.ink)
                    }

                    FilledIconButton(
                        onClick = {
                            zoomScale = 1.0f
                            panOffset = Offset(-250f, -250f)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = ext.paper,
                            contentColor = ext.green
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Center Ward 4", modifier = Modifier.size(18.dp))
                    }
                }

                // ── Map Scale Badge ──
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp),
                    color = ext.paper.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🗺 2x Enlarged Ward", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ext.ink)
                        Text("• Drag to pan", fontSize = 10.sp, color = ext.muted)
                    }
                }

                // ── Floating Action Button (Cleanly positioned on right) ──
                FloatingActionButton(
                    onClick = { navigator.navigateTo(Route.Report) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = if (selectedPin != null) 180.dp else 24.dp, end = 16.dp),
                    containerColor = ext.green,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Report issue at location")
                }

                // ── Selected Issue Detail Bottom Card ──
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = selectedPin != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        selectedPin?.let { pin ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            color = ext.paper,
                            shape = RoundedCornerShape(14.dp),
                            shadowElevation = 10.dp,
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatusChip(label = pin.status)
                                        Text(
                                            text = t("${pin.reportsCount} sample reports", "${pin.reportsCount} नमूना रिपोर्ट"),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ext.muted
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedPin = null },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = ext.muted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // If streetlight, show authentic Indian broken streetlight photo thumbnail!
                                    if (pin.category == "LIGHTING") {
                                        Image(
                                            painter = painterResource(Res.drawable.broken_streetlight),
                                            contentDescription = "Streetlight thumbnail",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = pin.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ext.ink,
                                            maxLines = 2
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "📍 ${pin.address} • ${pin.timeAgo}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ext.muted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { navigator.navigateTo(Route.CaseDetail(pin.id)) },
                                        enabled = true,
                                        modifier = Modifier.weight(1.3f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) { Text(t("Inspect Case →", "केस देखें →"), fontSize = 12.sp, color = ext.green) }
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
            }
        }
    }
}
