package com.trajectory.vault.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.trajectory.vault.ui.theme.LocalQuality
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** A small rounded label with a tonal fill, used for counts such as Present, Absent and Total. */
@Composable
fun StatPill(label: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/**
 * A half-circle gauge that springs to [percent], with a dot at the safe [threshold] and the number in large
 * display type in the middle. The arc turns to the error colour while the attendance is under the threshold.
 */
@Composable
fun SemiGauge(
    percent: Double?,
    threshold: Int,
    arcColor: Color,
    trackColor: Color,
    belowColor: Color,
    markerColor: Color,
    numberColor: Color,
    captionColor: Color,
    modifier: Modifier = Modifier,
    caption: String = "attendance",
) {
    val q = LocalQuality.current
    val target = ((percent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = q.spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "semiGauge",
    )
    val below = percent != null && percent < threshold
    val color by animateColorAsState(
        targetValue = if (below) belowColor else arcColor,
        animationSpec = q.spring(),
        label = "semiGaugeColor",
    )
    Box(modifier = modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.BottomCenter) {
        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            val stroke = 18.dp.toPx()
            val diameter = minOf(size.width - stroke, (size.height - stroke / 2) * 2f)
            val left = (size.width - diameter) / 2f
            val topLeft = Offset(left, stroke / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (progress > 0f) {
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            val angle = Math.toRadians(180.0 + 180.0 * threshold / 100.0)
            val radius = diameter / 2f
            drawCircle(
                color = markerColor,
                radius = 4.dp.toPx(),
                center = Offset(
                    x = left + radius + (radius * cos(angle)).toFloat(),
                    y = stroke / 2f + radius + (radius * sin(angle)).toFloat(),
                ),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (percent == null) "--" else "${percent.roundToInt()}%",
                style = MaterialTheme.typography.displayMedium,
                color = numberColor,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelMedium,
                color = captionColor,
            )
        }
    }
}

/** Splits [items] into rows of two equal tiles, as in a card grid. A last odd tile keeps half the width. */
@Composable
fun <T> TileGrid(items: List<T>, modifier: Modifier = Modifier, tile: @Composable (Int, T) -> Unit) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEachIndexed { column, item ->
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) { tile(rowIndex * 2 + column, item) }
                }
                if (row.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
    }
}
