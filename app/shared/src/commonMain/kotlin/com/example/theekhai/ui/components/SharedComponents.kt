package com.example.theekhai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.theekhai.ui.theme.LocalTheekHaiColors
import com.example.theekhai.ui.theme.TheekHaiColors

/* ── Eyebrow label (small uppercase caption like "01" / "YOUR VOICE MATTERS") ── */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalTheekHaiColors.current.muted,
        modifier = modifier
    )
}

/* ── Status Chip (PENDING, TRIAGED, VERIFIED etc.) ── */
@Composable
fun StatusChip(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = TheekHaiColors.StatusDefault,
    contentColor: Color = TheekHaiColors.StatusDefaultText
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = RoundedCornerShape(3.dp)
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            lineHeight = 15.sp,
        )
    }
}

/* ── Priority Badge ── */
@Composable
fun PriorityBadge(priority: String, modifier: Modifier = Modifier) {
    val color = when (priority.uppercase()) {
        "CRITICAL" -> TheekHaiColors.PriorityCritical
        "HIGH" -> TheekHaiColors.PriorityHigh
        "MEDIUM" -> TheekHaiColors.PriorityMedium
        else -> TheekHaiColors.PriorityLow
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = priority.uppercase(),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/* ── Metric Card (stats row) ── */
@Composable
fun MetricCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val ext = LocalTheekHaiColors.current
    Surface(
        modifier = modifier,
        color = ext.paper,
        shape = RoundedCornerShape(5.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = ext.muted,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal,
                color = accent,
            )
        }
    }
}

/* ── Promise step item (01 · Tell us what happened) ── */
@Composable
fun PromiseStep(number: String, title: String, body: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = number,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF88957B),
            modifier = Modifier.width(34.dp)
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = LocalTheekHaiColors.current.ink)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = LocalTheekHaiColors.current.muted)
        }
    }
}

/* ── Sidebar nav item ── */
@Composable
fun SidebarNavItem(
    number: String,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalTheekHaiColors.current
    val bg by animateColorAsState(
        if (isActive) ext.navActive else Color.Transparent,
        animationSpec = tween(180)
    )
    val textColor by animateColorAsState(
        if (isActive) Color(0xFFFCFBF6) else ext.navText,
        animationSpec = tween(180)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text(
            text = number,
            fontSize = 10.sp,
            color = textColor.copy(alpha = 0.6f),
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.W500,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/* ── Avatar circle with initials ── */
@Composable
fun AvatarCircle(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    bgColor: Color = TheekHaiColors.Green,
    textColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.uppercase().take(2),
            fontWeight = FontWeight.W700,
            fontSize = (size.value * 0.32f).sp,
            color = textColor
        )
    }
}

/* ── Panel card (white bordered card like form panels) ── */
@Composable
fun PanelCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = LocalTheekHaiColors.current
    Surface(
        modifier = modifier,
        color = ext.paper,
        shape = RoundedCornerShape(5.dp),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
    ) {
        Column(modifier = Modifier.padding(28.dp), content = content)
    }
}

/* ── Primary action button (green) ── */
@Composable
fun TheekHaiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isSecondary: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(4.dp),
        colors = if (isSecondary) {
            ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = TheekHaiColors.Green,
            )
        } else {
            ButtonDefaults.buttonColors(
                containerColor = TheekHaiColors.Green,
                contentColor = Color(0xFFFFFDF5),
            )
        },
        border = if (isSecondary) {
            ButtonDefaults.outlinedButtonBorder(enabled = enabled)
        } else null,
        contentPadding = PaddingValues(horizontal = 23.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.W500,
            fontSize = 13.sp,
        )
    }
}

/* ── Dot indicator (online/live status) ── */
@Composable
fun DotIndicator(
    color: Color = Color(0xFF648773),
    modifier: Modifier = Modifier,
    size: Dp = 6.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

/* ── Divider with line color ── */
@Composable
fun TheekHaiDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = LocalTheekHaiColors.current.line,
        thickness = 1.dp
    )
}
