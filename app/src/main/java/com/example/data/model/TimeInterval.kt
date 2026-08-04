package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SyncOption(val label: String) {
    PLANNED_ONLY("Только запланированные"),
    COMPLETED_ONLY("Только совершённые"),
    BOTH("Запланированные и совершённые"),
    NONE("Не синхронизировать")
}

@Entity(tableName = "time_intervals")
data class TimeInterval(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val startTimeMinutes: Int, // 0 to 1439 (minutes from 00:00)
    val endTimeMinutes: Int, // 1 to 1440
    val title: String,
    val categoryName: String,
    val colorHex: Long,
    val isCompleted: Boolean, // false = Planned (hatched), true = Completed (solid)
    val notes: String = "",
    val calendarEventId: Long? = null
) {
    val durationMinutes: Int
        get() = if (endTimeMinutes >= startTimeMinutes) {
            endTimeMinutes - startTimeMinutes
        } else {
            (1440 - startTimeMinutes) + endTimeMinutes
        }

    fun formattedTimeRange(): String {
        val startH = startTimeMinutes / 60
        val startM = startTimeMinutes % 60
        val endH = (endTimeMinutes / 60) % 24
        val endM = endTimeMinutes % 60
        return String.format("%02d:%02d - %02d:%02d", startH, startM, endH, endM)
    }
}

data class DefaultCategory(
    val name: String,
    val colorHex: Long
) {
    companion object {
        val availableColors = listOf(
            0xFF6366F1, // Indigo
            0xFF3B82F6, // Blue
            0xFF0EA5E9, // Sky Blue
            0xFF10B981, // Emerald
            0xFF84CC16, // Lime
            0xFFF59E0B, // Amber
            0xFFEF4444, // Red
            0xFFEC4899, // Pink
            0xFF8B5CF6, // Purple
            0xFF14B8A6, // Teal
            0xFF64748B  // Slate
        )

        val defaultList = listOf(
            DefaultCategory("Сон", 0xFF6366F1), // Indigo
            DefaultCategory("Работа", 0xFF3B82F6), // Blue
            DefaultCategory("Здоровье / Спорт", 0xFF10B981), // Emerald
            DefaultCategory("Учёба", 0xFFF59E0B), // Amber
            DefaultCategory("Отдых / Хобби", 0xFFEC4899), // Pink
            DefaultCategory("Быт / Дом", 0xFF8B5CF6), // Purple
            DefaultCategory("Еда", 0xFF14B8A6), // Teal
            DefaultCategory("Разное", 0xFF64748B) // Slate
        )

        val list get() = defaultList
    }
}
