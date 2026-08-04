package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.data.model.TimeInterval
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DayWheelCanvas(
    intervals: List<TimeInterval>,
    selectedInterval: TimeInterval?,
    isToday: Boolean,
    onIntervalSelected: (TimeInterval?) -> Unit,
    onTimeSelected: (startMin: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val textPaintColor = onSurfaceColor.toArgb()

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800),
        label = "wheelAnimation"
    )

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    if (isToday) {
        LaunchedEffect(Unit) {
            while (true) {
                currentTime = LocalTime.now()
                delay(1000L)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(intervals) {
                    detectTapGestures { offset ->
                        val size = this.size
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width.coerceAtMost(size.height) / 2f
                        val outerRadius = radius * 0.90f

                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val distFromCenter = kotlin.math.sqrt(dx * dx + dy * dy)

                        if (distFromCenter <= outerRadius) {
                            var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            // Normalize angle so 00:00 (Midnight) is at BOTTOM (90 deg) and 12:00 is at TOP (-90/270 deg)
                            val normalizedAngle = (angleDeg - 90f + 360f) % 360f
                            val clickedMinute = ((normalizedAngle / 360f) * 1440f).toInt().coerceIn(0, 1439)

                            // Find matching interval
                            val clicked = intervals.find { interval ->
                                val start = interval.startTimeMinutes
                                val end = interval.endTimeMinutes
                                if (end > start) {
                                    clickedMinute in start until end
                                } else {
                                    clickedMinute >= start || clickedMinute < end
                                }
                            }

                            if (clicked != null) {
                                onIntervalSelected(clicked)
                            } else {
                                onIntervalSelected(null)
                                onTimeSelected(clickedMinute)
                            }
                        } else {
                            onIntervalSelected(null)
                        }
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val outerRadius = (canvasWidth.coerceAtMost(canvasHeight) / 2f) * 0.88f
            val hubRadius = outerRadius * 0.35f

            // 1. Base circle background
            drawCircle(color = trackColor, radius = outerRadius, center = center)

            // 2. Draw Pie Sectors (Center -> Start Point -> Arc to End Point -> Center)
            intervals.forEach { interval ->
                val startMinute = interval.startTimeMinutes
                val duration = interval.durationMinutes
                // 00:00 at 90 deg (Bottom), 12:00 at 270 deg (Top)
                val startAngle = 90f + (startMinute / 1440f) * 360f
                val sweepAngle = ((duration / 1440f) * 360f) * animatedProgress

                if (sweepAngle > 0.1f) {
                    val color = Color(interval.colorHex)
                    val isSelected = interval.id == selectedInterval?.id

                    val sectorRadius = if (isSelected) outerRadius * 1.04f else outerRadius

                    // Create Pie Sector Path anchored at center (3 points: center, start arc, end arc)
                    val sectorPath = createPieSectorPath(
                        center = center,
                        outerRadius = sectorRadius,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle
                    )

                    if (interval.isCompleted) {
                        // Vibrant Solid Fill for Completed
                        drawPath(sectorPath, color = color)

                        // Subtle inner light highlight line along edge
                        drawPath(
                            path = sectorPath,
                            color = Color.White.copy(alpha = 0.3f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    } else {
                        // Hatching Pattern for Planned
                        // 1. Solid background color tint for high visibility
                        drawPath(sectorPath, color = color.copy(alpha = 0.5f))

                        // 2. High-contrast diagonal hatch lines
                        clipPath(sectorPath) {
                            val lineSpacing = 14f
                            val strokeWidth = 3.5f
                            val boundsSize = outerRadius * 2.5f

                            var x = -boundsSize
                            while (x < boundsSize) {
                                drawLine(
                                    color = color.copy(alpha = 0.95f),
                                    start = Offset(center.x + x, center.y - boundsSize),
                                    end = Offset(center.x + x + boundsSize, center.y + boundsSize),
                                    strokeWidth = strokeWidth
                                )
                                x += lineSpacing
                            }
                        }
                    }

                    // Explicit sector stroke outline
                    drawPath(
                        path = sectorPath,
                        color = if (isSelected) primaryColor else color,
                        style = Stroke(width = if (isSelected) 4.dp.toPx() else 2.dp.toPx())
                    )
                }
            }

            // 3. Center Hub Cap for crisp visual balance
            drawCircle(color = trackColor.copy(alpha = 0.85f), radius = hubRadius, center = center)
            drawCircle(color = outlineColor, radius = hubRadius, center = center, style = Stroke(width = 1.5.dp.toPx()))

            // 4. Outer Rim Circle Line
            drawCircle(color = outlineColor, radius = outerRadius, center = center, style = Stroke(width = 2.dp.toPx()))

            // 4. Hour Ticks and Labels (00 at Bottom, 12 at Top)
            val paint = Paint().apply {
                color = textPaintColor
                textSize = 12.dp.toPx()
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }

            for (hour in 0 until 24) {
                // 00:00 -> 90 deg (Bottom), 12:00 -> 270 deg (Top)
                val hourAngle = 90f + (hour / 24f) * 360f
                val rad = Math.toRadians(hourAngle.toDouble())
                val isMajor = hour % 3 == 0

                val tickStartR = outerRadius
                val tickEndR = outerRadius + (if (isMajor) 10.dp.toPx() else 5.dp.toPx())

                val startX = center.x + tickStartR * cos(rad).toFloat()
                val startY = center.y + tickStartR * sin(rad).toFloat()
                val endX = center.x + tickEndR * cos(rad).toFloat()
                val endY = center.y + tickEndR * sin(rad).toFloat()

                drawLine(
                    color = if (isMajor) onSurfaceColor else outlineColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )

                if (isMajor) {
                    val labelR = outerRadius + 22.dp.toPx()
                    val labelX = center.x + labelR * cos(rad).toFloat()
                    val labelY = center.y + labelR * sin(rad).toFloat() + (paint.textSize / 3f)

                    val hourLabel = String.format("%02d", hour)
                    drawContext.canvas.nativeCanvas.drawText(hourLabel, labelX, labelY, paint)
                }
            }

            // 5. Current Time Red Indicator Hand if Today
            if (isToday) {
                val currentMinutes = currentTime.hour * 60 + currentTime.minute + (currentTime.second / 60f)
                val nowAngle = 90f + (currentMinutes / 1440f) * 360f
                val nowRad = Math.toRadians(nowAngle.toDouble())

                val handStart = Offset(
                    center.x + (hubRadius - 4.dp.toPx()) * cos(nowRad).toFloat(),
                    center.y + (hubRadius - 4.dp.toPx()) * sin(nowRad).toFloat()
                )
                val handEnd = Offset(
                    center.x + (outerRadius + 8.dp.toPx()) * cos(nowRad).toFloat(),
                    center.y + (outerRadius + 8.dp.toPx()) * sin(nowRad).toFloat()
                )

                drawLine(
                    color = Color(0xFFE53935),
                    start = handStart,
                    end = handEnd,
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Small dot at needle tip
                drawCircle(
                    color = Color(0xFFE53935),
                    radius = 5.dp.toPx(),
                    center = handEnd
                )
            }
        }
    }
}

private fun createPieSectorPath(
    center: Offset,
    outerRadius: Float,
    startAngle: Float,
    sweepAngle: Float
): Path {
    val outerRect = Rect(center.x - outerRadius, center.y - outerRadius, center.x + outerRadius, center.y + outerRadius)
    return Path().apply {
        moveTo(center.x, center.y)
        arcTo(
            rect = outerRect,
            startAngleDegrees = startAngle,
            sweepAngleDegrees = sweepAngle,
            forceMoveTo = false
        )
        close()
    }
}
