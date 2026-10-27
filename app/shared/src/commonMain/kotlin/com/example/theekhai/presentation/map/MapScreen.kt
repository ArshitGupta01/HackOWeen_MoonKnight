package com.example.theekhai.presentation.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.domain.model.Complaint
import com.example.theekhai.navigation.LocalNavigator
import com.example.theekhai.navigation.Route
import com.example.theekhai.platform.GeotagResult
import com.example.theekhai.platform.rememberLocationFetcher
import com.example.theekhai.ui.components.Eyebrow
import com.example.theekhai.ui.components.StatusChip
import com.example.theekhai.ui.components.TheekHaiDivider
import com.example.theekhai.ui.localization.t
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors
import kotlin.math.roundToInt

private data class MapCivicPin(
    val id: String,
    val title: String,
    val category: String,
    val status: String,
    val address: String,
    val x: Float,
    val y: Float,
)

@Composable
fun MapScreen(
    complaints: List<Complaint> = emptyList(),
    isLoading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
) {
    val navigator = LocalNavigator.current
    val colors = LocalTheekHaiColors.current
    val darkMap = colors.paper.luminance() < .5f
    val categories = listOf("ALL", "LIGHTING", "ROADS", "WATER", "WASTE", "OTHER")
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedPin by remember { mutableStateOf<MapCivicPin?>(null) }
    var currentLocation by remember { mutableStateOf<GeotagResult?>(null) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    val requestLocation = rememberLocationFetcher { result ->
        result.onSuccess {
            val inMapArea = it.latitude in NeighborhoodMapData.MIN_LAT..NeighborhoodMapData.MAX_LAT &&
                it.longitude in NeighborhoodMapData.MIN_LON..NeighborhoodMapData.MAX_LON
            currentLocation = it.takeIf { inMapArea }
            locationMessage = if (inMapArea) "Your location is shown on the map." else "Your location is outside the Murthal map area."
        }.onFailure {
            locationMessage = it.message ?: "Location is unavailable. Check location permission and try again."
        }
    }

    val pins = remember(complaints) {
        complaints.mapNotNull { complaint ->
            val lat = complaint.latitude
            val lon = complaint.longitude
            if (lat == 0.0 || lon == 0.0 ||
                lat !in NeighborhoodMapData.MIN_LAT..NeighborhoodMapData.MAX_LAT ||
                lon !in NeighborhoodMapData.MIN_LON..NeighborhoodMapData.MAX_LON
            ) return@mapNotNull null
            val (x, y) = NeighborhoodMapData.project(lon, lat)
            MapCivicPin(complaint.id, complaint.title, complaint.category.uppercase(), complaint.status.name, complaint.address, x, y)
        }
    }
    LaunchedEffect(pins) {
        if (selectedPin?.id !in pins.map(MapCivicPin::id)) selectedPin = null
    }

    Column(Modifier.fillMaxSize().background(colors.paper)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("MURTHAL · CIVIC MAP")
                    Text("Cases near you", style = MaterialTheme.typography.headlineSmall, color = colors.ink, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = requestLocation) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Show my location", tint = colors.green)
                }
            }
            Text(
                t("Offline neighborhood map with live cases from your account.", "ऑफ़लाइन पड़ोस का नक्शा और आपके खाते के लाइव मामले।"),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            if (isLoading) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.green)
            }
            error?.let {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
            locationMessage?.let { Text(it, color = colors.muted, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    val active = selectedCategory == category
                    Surface(
                        modifier = Modifier.clickable {
                            selectedCategory = category
                            if (category != "ALL" && selectedPin?.category != category) selectedPin = null
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) colors.green else colors.sidebarBg,
                        border = if (active) null else ButtonDefaults.outlinedButtonBorder(enabled = true),
                    ) {
                        Text(
                            if (category == "ALL") "All cases" else category.lowercase().replaceFirstChar { it.uppercase() },
                            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            color = if (active) Color.White else colors.ink,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
        TheekHaiDivider()

        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(0.dp))) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }
            val heightPx = with(density) { maxHeight.toPx() }
            val baseScale = minOf(widthPx, heightPx) / 2000f
            val originX = (widthPx - 2000f * baseScale) / 2f
            val originY = (heightPx - 2000f * baseScale) / 2f
            val mapScale = baseScale * zoom

            Canvas(
                Modifier.fillMaxSize().background(colors.sidebarBg).pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        zoom = (zoom * zoomChange).coerceIn(0.75f, 4f)
                        pan += panChange
                    }
                }
            ) {
                val mapLeft = originX + pan.x
                val mapTop = originY + pan.y
                translate(mapLeft, mapTop) {
                    scale(mapScale, pivot = Offset.Zero) {
                        clipRect(0f, 0f, 2000f, 2000f) {
                            NeighborhoodMapData.roads.forEach { road ->
                                if (road.points.size >= 2) {
                                    val path = Path().apply {
                                        moveTo(road.points.first().first, road.points.first().second)
                                        road.points.drop(1).forEach { (x, y) -> lineTo(x, y) }
                                    }
                                    drawPath(
                                        path = path,
                                        color = if (road.isMajor) {
                                            if (darkMap) Color(0xFF737A68) else Color(0xFFB2A98E)
                                        } else {
                                            if (darkMap) Color(0xFF3D4A42) else Color(0xFFC9CEC4)
                                        },
                                        style = Stroke(width = if (road.isMajor) 26f else 11f),
                                    )
                                    if (road.isMajor) drawPath(path, if (darkMap) Color(0xFF26322B) else Color(0xFFF8F6EF), style = Stroke(width = 8f))
                                }
                            }
                            NeighborhoodMapData.landmarks.forEach { landmark ->
                                drawCircle(Color(0xFF75816E), radius = 7f, center = Offset(landmark.x, landmark.y))
                            }
                            pins.filter { selectedCategory == "ALL" || it.category == selectedCategory }.forEach { pin ->
                                val color = when (pin.status) {
                                    "RESOLVED", "VERIFIED", "CLOSED" -> Color(0xFF2F8058)
                                    "IN_PROGRESS", "ASSIGNED" -> Color(0xFF3479A8)
                                    else -> Color(0xFFD88C22)
                                }
                                drawCircle(color.copy(alpha = .22f), radius = if (selectedPin?.id == pin.id) 36f else 28f, center = Offset(pin.x, pin.y))
                                drawCircle(Color.White, radius = 18f, center = Offset(pin.x, pin.y))
                                drawCircle(color, radius = 13f, center = Offset(pin.x, pin.y))
                            }
                            currentLocation?.let { location ->
                                val (x, y) = NeighborhoodMapData.project(location.longitude, location.latitude)
                                drawCircle(Color(0xFF3678D4).copy(alpha = .2f), radius = 38f, center = Offset(x, y))
                                drawCircle(Color(0xFF3678D4), radius = 15f, center = Offset(x, y))
                                drawCircle(Color.White, radius = 6f, center = Offset(x, y))
                            }
                        }
                    }
                }
            }

            pins.filter { selectedCategory == "ALL" || it.category == selectedCategory }.forEach { pin ->
                val x = originX + pan.x + pin.x * mapScale
                val y = originY + pan.y + pin.y * mapScale
                Box(
                    Modifier.offset { IntOffset(x.roundToInt() - 18, y.roundToInt() - 18) }
                        .size(36.dp)
                        .clickable { selectedPin = pin },
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(Modifier.size(26.dp), shape = CircleShape, color = Color.Transparent) {}
                }
            }

            Column(Modifier.align(Alignment.TopEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                FilledIconButton(onClick = { zoom = (zoom * 1.25f).coerceAtMost(4f) }) { Icon(Icons.Default.Add, "Zoom in") }
                FilledIconButton(onClick = { zoom = (zoom / 1.25f).coerceAtLeast(.75f) }) { Icon(Icons.Default.Remove, "Zoom out") }
                FilledIconButton(onClick = { zoom = 1f; pan = Offset.Zero; selectedPin = null }) {
                    Icon(Icons.Default.LocationOn, "Center map")
                }
            }
            Surface(Modifier.align(Alignment.TopStart).padding(12.dp), color = colors.paper.copy(alpha = .94f), shape = RoundedCornerShape(10.dp)) {
                Text("OpenStreetMap · Murthal", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = colors.ink, style = MaterialTheme.typography.labelSmall)
            }
            if (!isLoading && error == null && pins.isEmpty()) {
                Surface(Modifier.align(Alignment.Center).padding(20.dp), color = colors.paper.copy(alpha = .96f), shape = RoundedCornerShape(12.dp)) {
                    Text("No cases with coordinates in this map area yet.", Modifier.padding(14.dp), color = colors.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            selectedPin?.let { pin ->
                Surface(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                    color = colors.paper,
                    shape = RoundedCornerShape(14.dp),
                    shadowElevation = 8.dp,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(pin.status.replace('_', ' '))
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { selectedPin = null }) { Text("Close") }
                        }
                        Text(pin.title, color = colors.ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${pin.category} · ${pin.address}", color = colors.muted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { navigator.navigateTo(Route.CaseDetail(pin.id)) }, Modifier.fillMaxWidth()) {
                            Text("View case")
                        }
                    }
                }
            }
        }
    }
}
