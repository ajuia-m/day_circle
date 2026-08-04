package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TimeInterval
import androidx.compose.material.icons.filled.Settings
import com.example.ui.DayWheelViewModel
import com.example.ui.components.DayWheelCanvas
import com.example.ui.components.IntervalCard
import com.example.ui.dialogs.AddEditIntervalDialog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DayWheelViewModel,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val intervals by viewModel.intervalsForSelectedDate.collectAsStateWithLifecycle()
    val selectedInterval by viewModel.selectedInterval.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var intervalToEdit by remember { mutableStateOf<TimeInterval?>(null) }
    var newIntervalStartMinute by remember { mutableStateOf(540) } // Default 09:00

    var showDatePickerDialog by remember { mutableStateOf(false) }

    val dateStr = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val isToday = selectedDate == LocalDate.now()

    // Calculate statistics
    val plannedMinutes = intervals.filter { !it.isCompleted }.sumOf { it.durationMinutes }
    val completedMinutes = intervals.filter { it.isCompleted }.sumOf { it.durationMinutes }
    val totalMinutesTracked = plannedMinutes + completedMinutes
    val trackedPercentage = ((totalMinutesTracked / 1440f) * 100).toInt().coerceIn(0, 100)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Круг Дня",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    // Open Settings Screen
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    intervalToEdit = null
                    newIntervalStartMinute = 540
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить интервал")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Date Selector Header
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { viewModel.previousDay() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий день")
                        }

                        // Date Title Button
                        val russianLocale = Locale("ru", "RU")
                        val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", russianLocale)
                        val formattedDate = selectedDate.format(formatter)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showDatePickerDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = formattedDate,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (isToday) "• Сегодня" else "Нажмите для выбора даты",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isToday) {
                                IconButton(onClick = { viewModel.selectToday() }) {
                                    Icon(
                                        Icons.Default.Today,
                                        contentDescription = "Сегодня",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.nextDay() }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Следующий день")
                            }
                        }
                    }
                }
            }

            // 2. Day Wheel Canvas
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        DayWheelCanvas(
                            intervals = intervals,
                            selectedInterval = selectedInterval,
                            isToday = isToday,
                            onIntervalSelected = { interval ->
                                viewModel.selectedInterval.value = interval
                            },
                            onTimeSelected = { startMin ->
                                intervalToEdit = null
                                newIntervalStartMinute = startMin
                                showAddEditDialog = true
                            }
                        )

                        // Center Selected Info / Instruction text
                        if (selectedInterval != null) {
                            val sel = selectedInterval!!
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        intervalToEdit = sel
                                        showAddEditDialog = true
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${sel.formattedTimeRange()} — ${sel.title}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (sel.isCompleted) "Совершено (заливка)" else "Запланировано (штриховка)",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    TextButton(onClick = {
                                        intervalToEdit = sel
                                        showAddEditDialog = true
                                    }) {
                                        Text("Изменить")
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Коснитесь сектора круга для деталей или пустого места для создания",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 3. Summary Statistics Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Planned Stats
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Запланировано", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${plannedMinutes / 60}ч ${plannedMinutes % 60}м",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Completed Stats
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Совершено", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${completedMinutes / 60}ч ${completedMinutes % 60}м",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Total % Tracked
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Заполнено", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$trackedPercentage%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // 4. Interval List Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "События дня (${intervals.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 5. Interval Cards List
            if (intervals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "На выбранную дату дел пока нет",
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Нажмите на круг или кнопку «+» внизу, чтобы добавить промежуток времени.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(intervals, key = { it.id }) { interval ->
                    IntervalCard(
                        interval = interval,
                        isSelected = selectedInterval?.id == interval.id,
                        onToggleCompleted = { viewModel.toggleCompleted(interval) },
                        onClick = {
                            intervalToEdit = interval
                            showAddEditDialog = true
                        }
                    )
                }
            }

            // Bottom spacer for FAB
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Dialogs
    if (showAddEditDialog) {
        AddEditIntervalDialog(
            initialInterval = intervalToEdit,
            initialStartMinute = newIntervalStartMinute,
            dateStr = dateStr,
            categories = categories,
            onDismiss = { showAddEditDialog = false },
            onSave = { interval ->
                viewModel.saveInterval(interval)
                showAddEditDialog = false
            },
            onDelete = { interval ->
                viewModel.deleteInterval(interval)
                showAddEditDialog = false
            }
        )
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        viewModel.setSelectedDate(localDate)
                    }
                    showDatePickerDialog = false
                }) {
                    Text("Выбрать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Отмена")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
