package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.DeviceCalendar
import com.example.data.model.SyncOption

@Composable
fun CalendarSyncDialog(
    availableCalendars: List<DeviceCalendar>,
    selectedCalendarId: Long?,
    selectedCalendarName: String,
    currentSyncOption: SyncOption,
    isAutoSyncEnabled: Boolean,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onSaveSettings: (calendarId: Long?, calendarName: String, option: SyncOption, autoSync: Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onDismiss: () -> Unit
) {
    var calId by remember { mutableStateOf(selectedCalendarId) }
    var calName by remember { mutableStateOf(selectedCalendarName) }
    var option by remember { mutableStateOf(currentSyncOption) }
    var autoSync by remember { mutableStateOf(isAutoSyncEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Синхронизация с календарем", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!hasPermission) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Требуется разрешение на доступ к календарю устройства.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onRequestPermission) {
                                Text("Предоставить доступ")
                            }
                        }
                    }
                } else {
                    // Auto-sync Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Автовыгрузка в календарь", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Автоматически обновлять события при изменениях", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = autoSync, onCheckedChange = { autoSync = it })
                    }

                    HorizontalDivider()

                    // Sync Options
                    Text("Что сохранять в календарь:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    SyncOption.entries.forEach { syncOpt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { option = syncOpt },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = option == syncOpt,
                                onClick = { option = syncOpt }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(syncOpt.label, fontSize = 13.sp)
                        }
                    }

                    HorizontalDivider()

                    // Calendar Selection
                    Text("Выберите календарь:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (availableCalendars.isEmpty()) {
                        Text("Календари не найдены", style = MaterialTheme.typography.bodySmall)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                        ) {
                            items(availableCalendars) { calendar ->
                                val isSelected = calId == calendar.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            calId = calendar.id
                                            calName = calendar.displayName
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(calendar.displayName, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        if (calendar.accountName.isNotBlank()) {
                                            Text(calendar.accountName, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Выбран", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onSyncNow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Синхронизировать выбранный день")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSettings(calId, calName, option, autoSync)
                    onDismiss()
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
