package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calendar.CalendarSyncManager
import com.example.calendar.DeviceCalendar
import com.example.data.local.AppDatabase
import com.example.data.model.DefaultCategory
import com.example.data.model.SyncOption
import com.example.data.model.TimeInterval
import com.example.data.repository.TimeIntervalRepository
import com.example.markdown.MarkdownParserExporter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.widget.DayCircleWidgetProvider
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DayWheelViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val calendarSyncManager = CalendarSyncManager(application)
    private val repository = TimeIntervalRepository(db.timeIntervalDao(), calendarSyncManager)

    private val prefs = application.getSharedPreferences("day_circle_prefs", Context.MODE_PRIVATE)

    val selectedDate = MutableStateFlow(LocalDate.now())

    // Category Management
    val categories = MutableStateFlow<List<DefaultCategory>>(loadSavedCategories())

    private fun loadSavedCategories(): List<DefaultCategory> {
        val saved = prefs.getString("custom_categories", null)
        if (saved.isNullOrEmpty()) return DefaultCategory.defaultList
        return try {
            saved.split(";;").mapNotNull { item ->
                val parts = item.split("|")
                if (parts.size == 2) {
                    DefaultCategory(parts[0], parts[1].toLong())
                } else null
            }.ifEmpty { DefaultCategory.defaultList }
        } catch (e: Exception) {
            DefaultCategory.defaultList
        }
    }

    private fun saveCategories(list: List<DefaultCategory>) {
        val str = list.joinToString(";;") { "${it.name}|${it.colorHex}" }
        prefs.edit().putString("custom_categories", str).apply()
        categories.value = list
    }

    fun addCategory(name: String, colorHex: Long) {
        if (name.isBlank()) return
        val current = categories.value.toMutableList()
        if (current.none { it.name.equals(name, ignoreCase = true) }) {
            current.add(DefaultCategory(name, colorHex))
            saveCategories(current)
            _toastMessage.value = "Категория «$name» добавлена"
        } else {
            _toastMessage.value = "Категория с таким названием уже существует"
        }
    }

    fun updateCategory(oldName: String, newName: String, newColorHex: Long) {
        if (newName.isBlank()) return
        val current = categories.value.map {
            if (it.name == oldName) DefaultCategory(newName, newColorHex) else it
        }
        saveCategories(current)
        _toastMessage.value = "Категория обновлена"
    }

    fun deleteCategory(categoryName: String) {
        val current = categories.value.filterNot { it.name == categoryName }
        if (current.isEmpty()) {
            _toastMessage.value = "Должна остаться хотя бы одна категория"
            return
        }
        saveCategories(current)
        _toastMessage.value = "Категория удалена"
    }

    fun resetCategories() {
        saveCategories(DefaultCategory.defaultList)
        _toastMessage.value = "Категории сброшены по умолчанию"
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val intervalsForSelectedDate: StateFlow<List<TimeInterval>> = selectedDate
        .flatMapLatest { date ->
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.getIntervalsForDate(dateStr)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedInterval = MutableStateFlow<TimeInterval?>(null)

    // Calendar sync settings
    val selectedCalendarId = MutableStateFlow<Long?>(
        if (prefs.contains("cal_id")) prefs.getLong("cal_id", -1L).takeIf { it != -1L } else null
    )
    val selectedCalendarName = MutableStateFlow(
        prefs.getString("cal_name", "Не выбран") ?: "Не выбран"
    )
    val syncOption = MutableStateFlow(
        SyncOption.valueOf(prefs.getString("sync_option", SyncOption.BOTH.name) ?: SyncOption.BOTH.name)
    )
    val autoSyncEnabled = MutableStateFlow(
        prefs.getBoolean("auto_sync", true)
    )

    private val _availableCalendars = MutableStateFlow<List<DeviceCalendar>>(emptyList())
    val availableCalendars: StateFlow<List<DeviceCalendar>> = _availableCalendars.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        loadAvailableCalendars()
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun loadAvailableCalendars() {
        if (calendarSyncManager.hasCalendarPermission()) {
            val list = calendarSyncManager.getDeviceCalendars()
            _availableCalendars.value = list
            if (selectedCalendarId.value == null && list.isNotEmpty()) {
                val primary = list.find { it.isPrimary } ?: list.first()
                updateCalendarSettings(primary.id, primary.displayName, syncOption.value, autoSyncEnabled.value)
            }
        }
    }

    fun updateCalendarSettings(calendarId: Long?, calendarName: String, option: SyncOption, autoSync: Boolean) {
        selectedCalendarId.value = calendarId
        selectedCalendarName.value = calendarName
        syncOption.value = option
        autoSyncEnabled.value = autoSync

        prefs.edit().apply {
            if (calendarId != null) putLong("cal_id", calendarId) else remove("cal_id")
            putString("cal_name", calendarName)
            putString("sync_option", option.name)
            putBoolean("auto_sync", autoSync)
            apply()
        }
    }

    fun setSelectedDate(date: LocalDate) {
        selectedDate.value = date
        selectedInterval.value = null
    }

    fun nextDay() {
        selectedDate.value = selectedDate.value.plusDays(1)
        selectedInterval.value = null
    }

    fun previousDay() {
        selectedDate.value = selectedDate.value.minusDays(1)
        selectedInterval.value = null
    }

    fun selectToday() {
        selectedDate.value = LocalDate.now()
        selectedInterval.value = null
    }

    fun saveInterval(interval: TimeInterval) {
        viewModelScope.launch {
            repository.saveInterval(
                interval = interval,
                autoSync = autoSyncEnabled.value,
                calendarId = selectedCalendarId.value,
                syncOption = syncOption.value
            )
            _toastMessage.value = "Сохранено"
            DayCircleWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun toggleCompleted(interval: TimeInterval) {
        viewModelScope.launch {
            repository.toggleCompleted(
                interval = interval,
                autoSync = autoSyncEnabled.value,
                calendarId = selectedCalendarId.value,
                syncOption = syncOption.value
            )
            DayCircleWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteInterval(interval: TimeInterval) {
        viewModelScope.launch {
            repository.deleteInterval(interval)
            if (selectedInterval.value?.id == interval.id) {
                selectedInterval.value = null
            }
            _toastMessage.value = "Удалено"
            DayCircleWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun syncCurrentDayWithCalendar() {
        viewModelScope.launch {
            val calId = selectedCalendarId.value
            if (calId == null) {
                _toastMessage.value = "Выберите календарь для синхронизации"
                return@launch
            }
            if (!calendarSyncManager.hasCalendarPermission()) {
                _toastMessage.value = "Предоставьте доступ к календарю"
                return@launch
            }

            val list = intervalsForSelectedDate.value
            var syncedCount = 0
            list.forEach { item ->
                val newEventId = calendarSyncManager.syncIntervalToCalendar(calId, item, syncOption.value)
                if (newEventId != null) {
                    repository.saveInterval(item.copy(calendarEventId = newEventId), false, null, SyncOption.NONE)
                    syncedCount++
                }
            }
            _toastMessage.value = "Синхронизировано записей: $syncedCount"
        }
    }

    fun exportCurrentDayMarkdown(): String {
        val dateStr = selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val list = intervalsForSelectedDate.value
        return MarkdownParserExporter.exportToMarkdown(dateStr, list)
    }

    fun importMarkdownText(markdownText: String) {
        viewModelScope.launch {
            val dateStr = selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val parsed = MarkdownParserExporter.parseMarkdown(markdownText, dateStr)
            if (parsed.isEmpty()) {
                _toastMessage.value = "Не удалось распознать записи в файле"
                return@launch
            }
            repository.importIntervals(
                parsed,
                autoSync = autoSyncEnabled.value,
                calendarId = selectedCalendarId.value,
                syncOption = syncOption.value
            )
            _toastMessage.value = "Импортировано записей: ${parsed.size}"
            DayCircleWidgetProvider.updateAllWidgets(getApplication())
        }
    }
}
