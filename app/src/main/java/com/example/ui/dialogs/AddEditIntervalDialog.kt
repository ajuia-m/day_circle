package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultCategory
import com.example.data.model.TimeInterval

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditIntervalDialog(
    initialInterval: TimeInterval?,
    initialStartMinute: Int = 540, // 09:00
    dateStr: String,
    categories: List<DefaultCategory> = DefaultCategory.defaultList,
    onDismiss: () -> Unit,
    onSave: (TimeInterval) -> Unit,
    onDelete: ((TimeInterval) -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialInterval?.title ?: "") }
    var startMin by remember { mutableIntStateOf(initialInterval?.startTimeMinutes ?: initialStartMinute) }
    var endMin by remember { mutableIntStateOf(initialInterval?.endTimeMinutes ?: (initialStartMinute + 60).coerceAtMost(1440)) }
    var categoryName by remember { mutableStateOf(initialInterval?.categoryName ?: "Работа") }
    var colorHex by remember { mutableStateOf(initialInterval?.colorHex ?: 0xFF3B82F6) }
    var isCompleted by remember { mutableStateOf(initialInterval?.isCompleted ?: false) }
    var notes by remember { mutableStateOf(initialInterval?.notes ?: "") }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialInterval == null) "Новый интервал ($dateStr)" else "Редактирование интервала",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название дела / активности") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Status Segmented Button
                Text(
                    text = "Тип активности на круге:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isCompleted,
                        onClick = { isCompleted = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Запланировано (штриховка)", fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = isCompleted,
                        onClick = { isCompleted = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Совершено (заливка)", fontSize = 12.sp)
                    }
                }

                // Time Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Start Time Button
                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Начало: ${formatMinutesToHHMM(startMin)}",
                            fontSize = 12.sp
                        )
                    }

                    // End Time Button
                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Конец: ${formatMinutesToHHMM(endMin)}",
                            fontSize = 12.sp
                        )
                    }
                }

                // Duration hint
                val duration = if (endMin >= startMin) endMin - startMin else (1440 - startMin) + endMin
                Text(
                    text = "Длительность: ${duration / 60}ч ${duration % 60}мин",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Category selector
                Text(
                    text = "Категория:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        val isSelected = categoryName == category.name
                        val catColor = Color(category.colorHex)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) catColor else catColor.copy(alpha = 0.2f),
                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    categoryName = category.name
                                    colorHex = category.colorHex
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(catColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметки (опционально)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) title = "Без названия"
                    val interval = (initialInterval ?: TimeInterval(
                        date = dateStr,
                        startTimeMinutes = startMin,
                        endTimeMinutes = endMin,
                        title = title,
                        categoryName = categoryName,
                        colorHex = colorHex,
                        isCompleted = isCompleted,
                        notes = notes
                    )).copy(
                        title = title,
                        startTimeMinutes = startMin,
                        endTimeMinutes = endMin,
                        categoryName = categoryName,
                        colorHex = colorHex,
                        isCompleted = isCompleted,
                        notes = notes
                    )
                    onSave(interval)
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            Row {
                if (initialInterval != null && onDelete != null) {
                    IconButton(onClick = { onDelete(initialInterval) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Отмена")
                }
            }
        }
    )

    // Time Picker Dialogs
    if (showStartTimePicker) {
        TimeSelectionDialog(
            initialHour = startMin / 60,
            initialMinute = startMin % 60,
            title = "Время начала",
            onDismiss = { showStartTimePicker = false },
            onConfirm = { h, m ->
                startMin = h * 60 + m
                if (endMin <= startMin) {
                    endMin = (startMin + 60).coerceAtMost(1440)
                }
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimeSelectionDialog(
            initialHour = (endMin / 60) % 24,
            initialMinute = endMin % 60,
            title = "Время окончания",
            onDismiss = { showEndTimePicker = false },
            onConfirm = { h, m ->
                val endCalculated = if (h == 0 && m == 0) 1440 else h * 60 + m
                endMin = endCalculated
                showEndTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelectionDialog(
    initialHour: Int,
    initialMinute: Int,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

fun formatMinutesToHHMM(minutes: Int): String {
    val h = (minutes / 60) % 24
    val m = minutes % 60
    return String.format("%02d:%02d", h, m)
}
