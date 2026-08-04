package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TimeInterval

@Composable
fun IntervalCard(
    interval: TimeInterval,
    isSelected: Boolean,
    onToggleCompleted: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = Color(interval.colorHex)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visual Color Strip / Pattern Indicator
            Box(
                modifier = Modifier
                    .width(10.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(categoryColor.copy(alpha = 0.3f))
            ) {
                if (interval.isCompleted) {
                    // Solid Fill for Completed
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(categoryColor)
                    )
                } else {
                    // Hatching lines for Planned
                    Canvas(modifier = Modifier.fillMaxHeight().fillMaxWidth()) {
                        val strokeW = 2.dp.toPx()
                        val spacing = 6.dp.toPx()
                        var y = -size.width
                        while (y < size.height + size.width) {
                            drawLine(
                                color = categoryColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y + size.width),
                                strokeWidth = strokeW
                            )
                            y += spacing
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Checkbox for status toggle
            Checkbox(
                checked = interval.isCompleted,
                onCheckedChange = { onToggleCompleted() }
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Main Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = interval.formattedTimeRange(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    val durationMinutes = interval.durationMinutes
                    val durationText = "${durationMinutes / 60}ч ${durationMinutes % 60}м"
                    Text(
                        text = "• $durationText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = interval.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textDecoration = if (interval.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (interval.notes.isNotBlank()) {
                    Text(
                        text = interval.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Category Badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = categoryColor.copy(alpha = 0.2f),
                contentColor = categoryColor
            ) {
                Text(
                    text = interval.categoryName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
