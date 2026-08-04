package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DefaultCategory
import com.example.data.model.SyncOption
import com.example.ui.DayWheelViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: DayWheelViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val availableCalendars by viewModel.availableCalendars.collectAsStateWithLifecycle()
    val selectedCalendarId by viewModel.selectedCalendarId.collectAsStateWithLifecycle()
    val selectedCalendarName by viewModel.selectedCalendarName.collectAsStateWithLifecycle()
    val syncOption by viewModel.syncOption.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<DefaultCategory?>(null) }
    var importMarkdownText by remember { mutableStateOf("") }
    var showImportSuccessToast by remember { mutableStateOf(false) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readOk = permissions[android.Manifest.permission.READ_CALENDAR] ?: false
        val writeOk = permissions[android.Manifest.permission.WRITE_CALENDAR] ?: false
        if (readOk && writeOk) {
            viewModel.loadAvailableCalendars()
            viewModel.syncCurrentDayWithCalendar()
        } else {
            Toast.makeText(context, "Для синхронизации требуется доступ к календарю", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // SECTION 1: CATEGORY MANAGEMENT
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Категории интервалов",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Настраивайте названия и цвета категорий для удобного разделения отрезков дня на круге:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Categories List
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color(category.colorHex), CircleShape)
                                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Action buttons
                                    IconButton(
                                        onClick = { editingCategory = category },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteCategory(category.name) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showAddCategoryDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Добавить категорию")
                        }

                        TextButton(onClick = { viewModel.resetCategories() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("По умолчанию", fontSize = 12.sp)
                        }
                    }
                }
            }

            // SECTION 2: CALENDAR SYNC
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Синхронизация с Календарем",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Выберите системный календарь для выгрузки событий:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!viewModel.calendarSyncManager.hasCalendarPermission()) {
                        OutlinedButton(
                            onClick = {
                                calendarPermissionLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.READ_CALENDAR,
                                        android.Manifest.permission.WRITE_CALENDAR
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Запросить доступ к календарю")
                        }
                    } else {
                        // Calendar Picker
                        Text("Календарь для синхронизации:", style = MaterialTheme.typography.labelLarge)

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            availableCalendars.forEach { cal ->
                                val isSelected = cal.id == selectedCalendarId
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateCalendarSettings(cal.id, cal.displayName, syncOption, autoSyncEnabled)
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.updateCalendarSettings(cal.id, cal.displayName, syncOption, autoSyncEnabled)
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(cal.displayName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(cal.accountName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Sync Mode
                        Text("Режим выгрузки событий:", style = MaterialTheme.typography.labelLarge)
                        SyncOption.values().forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateCalendarSettings(selectedCalendarId, selectedCalendarName, option, autoSyncEnabled)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = syncOption == option,
                                    onClick = {
                                        viewModel.updateCalendarSettings(selectedCalendarId, selectedCalendarName, option, autoSyncEnabled)
                                    }
                                )
                                Text(option.label, fontSize = 14.sp)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Auto Sync Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Автоматическая синхронизация при сохранении", fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = autoSyncEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.updateCalendarSettings(selectedCalendarId, selectedCalendarName, syncOption, checked)
                                }
                            )
                        }

                        // Sync Now Button
                        Button(
                            onClick = { viewModel.syncCurrentDayWithCalendar() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Синхронизировать события сейчас")
                        }
                    }
                }
            }

            // SECTION 3: MARKDOWN IMPORT & EXPORT
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Импорт и Экспорт Markdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    val exportedText = remember(selectedDate) { viewModel.exportCurrentDayMarkdown() }

                    // Export Card
                    Text("Экспорт текущего дня ($selectedDate):", style = MaterialTheme.typography.labelLarge)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (exportedText.isNotBlank()) exportedText else "# Расписание пусто",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(exportedText))
                                    Toast.makeText(context, "Скопировано в буфер обмена!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Скопировать", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Import Card
                    Text("Импорт расписания из Markdown:", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = importMarkdownText,
                        onValueChange = { importMarkdownText = it },
                        placeholder = { Text("Вставьте текст в формате # YYYY-MM-DD\n- [ ] 09:00 - 10:00 | Работа | Задача...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8
                    )

                    Button(
                        onClick = {
                            if (importMarkdownText.isNotBlank()) {
                                viewModel.importMarkdownText(importMarkdownText)
                                importMarkdownText = ""
                            }
                        },
                        enabled = importMarkdownText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Импортировать записи")
                    }
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        CategoryEditorDialog(
            initialCategory = null,
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.addCategory(name, colorHex)
                showAddCategoryDialog = false
            }
        )
    }

    // Edit Category Dialog
    editingCategory?.let { cat ->
        CategoryEditorDialog(
            initialCategory = cat,
            onDismiss = { editingCategory = null },
            onConfirm = { name, colorHex ->
                viewModel.updateCategory(cat.name, name, colorHex)
                editingCategory = null
            }
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun CategoryEditorDialog(
    initialCategory: DefaultCategory?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: Long) -> Unit
) {
    var name by remember { mutableStateOf(initialCategory?.name ?: "") }
    var selectedColor by remember { mutableStateOf(initialCategory?.colorHex ?: DefaultCategory.availableColors.first()) }

    // RGB Components derived from selectedColor
    val currentColor = remember(selectedColor) { Color(selectedColor) }
    var red by remember(selectedColor) { mutableStateOf((currentColor.red * 255f)) }
    var green by remember(selectedColor) { mutableStateOf((currentColor.green * 255f)) }
    var blue by remember(selectedColor) { mutableStateOf((currentColor.blue * 255f)) }

    // Hex text field state
    var hexText by remember(selectedColor) {
        mutableStateOf(String.format("%06X", selectedColor and 0xFFFFFF))
    }

    fun updateColorFromRgb(r: Float, g: Float, b: Float) {
        red = r
        green = g
        blue = b
        val argb = (0xFF shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
        selectedColor = argb.toLong() and 0xFFFFFFFFL
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialCategory == null) "Новая категория" else "Редактировать категорию",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название категории") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Preview Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(selectedColor),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Предпросмотр цвета",
                            color = if ((red * 0.299 + green * 0.587 + blue * 0.114) > 180) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.3f)
                        ) {
                            Text(
                                text = "#${hexText.uppercase()}",
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Presets
                Text("Готовые цвета:", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaultCategory.availableColors.forEach { colorHex ->
                        val color = Color(colorHex)
                        val isSelected = (colorHex and 0xFFFFFF) == (selectedColor and 0xFFFFFF)

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(color, CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .clip(CircleShape)
                                .clickable {
                                    val r = (colorHex shr 16 and 0xFF).toFloat()
                                    val g = (colorHex shr 8 and 0xFF).toFloat()
                                    val b = (colorHex and 0xFF).toFloat()
                                    updateColorFromRgb(r, g, b)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Custom Color Tuning (RGB Sliders & HEX Input)
                Text("Произвольный цвет:", style = MaterialTheme.typography.labelLarge)

                // Hex input
                OutlinedTextField(
                    value = hexText,
                    onValueChange = { input ->
                        val cleaned = input.take(6).filter { it.isLetterOrDigit() }
                        hexText = cleaned
                        if (cleaned.length == 6) {
                            try {
                                val parsed = cleaned.toLong(16)
                                val r = (parsed shr 16 and 0xFF).toFloat()
                                val g = (parsed shr 8 and 0xFF).toFloat()
                                val b = (parsed and 0xFF).toFloat()
                                updateColorFromRgb(r, g, b)
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text("HEX код цвета (#RRGGBB)") },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Red Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Красный (R)", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                        Text("${red.toInt()}", fontSize = 12.sp)
                    }
                    Slider(
                        value = red,
                        onValueChange = { updateColorFromRgb(it, green, blue) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red.copy(alpha = 0.7f))
                    )
                }

                // Green Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Зеленый (G)", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        Text("${green.toInt()}", fontSize = 12.sp)
                    }
                    Slider(
                        value = green,
                        onValueChange = { updateColorFromRgb(red, it, blue) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF10B981), activeTrackColor = Color(0xFF10B981).copy(alpha = 0.7f))
                    )
                }

                // Blue Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Синий (B)", fontSize = 12.sp, color = Color.Blue, fontWeight = FontWeight.Bold)
                        Text("${blue.toInt()}", fontSize = 12.sp)
                    }
                    Slider(
                        value = blue,
                        onValueChange = { updateColorFromRgb(red, green, it) },
                        valueRange = 0f..255f,
                        colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue.copy(alpha = 0.7f))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), selectedColor)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
