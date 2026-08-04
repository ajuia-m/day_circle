package com.example.markdown

import com.example.data.model.DefaultCategory
import com.example.data.model.TimeInterval
import java.time.LocalDate

object MarkdownParserExporter {

    fun exportToMarkdown(date: String, intervals: List<TimeInterval>): String {
        val sb = StringBuilder()
        sb.appendLine("# Круг Дня — $date")
        sb.appendLine()
        sb.appendLine("## Записи")

        if (intervals.isEmpty()) {
            sb.appendLine("*На этот день нет записей*")
        } else {
            intervals.forEach { interval ->
                val checkbox = if (interval.isCompleted) "[x]" else "[ ]"
                val timeRange = interval.formattedTimeRange()
                val hexColor = String.format("#%06X", 0xFFFFFF and interval.colorHex.toInt())
                
                sb.appendLine("- $checkbox **$timeRange** | ${interval.title} | #${interval.categoryName} | $hexColor")
                if (interval.notes.isNotBlank()) {
                    sb.appendLine("  - Заметки: ${interval.notes.replace("\n", " ")}")
                }
            }
        }

        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine("*Сгенерировано приложением Круг Дня*")
        return sb.toString()
    }

    fun exportMultipleDaysToMarkdown(intervalsByDate: Map<String, List<TimeInterval>>): String {
        val sb = StringBuilder()
        sb.appendLine("# Круг Дня — Экспорт записей")
        sb.appendLine()

        intervalsByDate.keys.sorted().forEach { date ->
            val intervals = intervalsByDate[date] ?: emptyList()
            sb.appendLine("## Дата: $date")
            intervals.forEach { interval ->
                val checkbox = if (interval.isCompleted) "[x]" else "[ ]"
                val timeRange = interval.formattedTimeRange()
                val hexColor = String.format("#%06X", 0xFFFFFF and interval.colorHex.toInt())
                sb.appendLine("- $checkbox **$timeRange** | ${interval.title} | #${interval.categoryName} | $hexColor")
                if (interval.notes.isNotBlank()) {
                    sb.appendLine("  - Заметки: ${interval.notes.replace("\n", " ")}")
                }
            }
            sb.appendLine()
        }

        sb.appendLine("---")
        sb.appendLine("*Сгенерировано приложением Круг Дня*")
        return sb.toString()
    }

    fun parseMarkdown(content: String, defaultDate: String): List<TimeInterval> {
        val result = mutableListOf<TimeInterval>()
        val lines = content.lines()

        var currentDate = defaultDate
        var currentInterval: TimeIntervalBuilder? = null

        // Regex for date header: e.g. "# Круг Дня — 2026-08-04" or "## Дата: 2026-08-04" or "2026-08-04"
        val dateHeaderRegex = Regex("""(?:#|##|\b)(\d{4}-\d{2}-\d{2})\b""")
        // Regex for interval item line: - [x] **08:00 - 09:00** | Title | #Category | #ColorHex
        val intervalItemRegex = Regex("""^-\s*\[([ xX])\]\s*\*\*(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})\*\*\s*\|\s*([^|]+)(?:\|\s*#?([^|]+))?(?:\|\s*(#?[0-9a-fA-F]{6}))?""")

        for (line in lines) {
            val trimmed = line.trim()

            // Check for date header
            if (trimmed.startsWith("#")) {
                val dateMatch = dateHeaderRegex.find(trimmed)
                if (dateMatch != null) {
                    currentDate = dateMatch.groupValues[1]
                }
            }

            // Check for interval item
            val itemMatch = intervalItemRegex.find(trimmed)
            if (itemMatch != null) {
                // Save previous builder if exists
                currentInterval?.build(currentDate)?.let { result.add(it) }

                val isCompleted = itemMatch.groupValues[1].lowercase() == "x"
                val startTimeStr = itemMatch.groupValues[2]
                val endTimeStr = itemMatch.groupValues[3]
                val title = itemMatch.groupValues[4].trim()
                val category = itemMatch.groupValues[5].trim().removePrefix("#").ifBlank { "Разное" }
                val colorHexStr = itemMatch.groupValues[6].trim()

                val startMinutes = parseTimeToMinutes(startTimeStr)
                var endMinutes = parseTimeToMinutes(endTimeStr)
                if (endMinutes <= startMinutes && endMinutes != 0) {
                    endMinutes = 1440
                } else if (endMinutes == 0) {
                    endMinutes = 1440
                }

                val colorHex = if (colorHexStr.isNotBlank()) {
                    try {
                        val cleaned = colorHexStr.removePrefix("#")
                        0xFF000000 or cleaned.toLong(16)
                    } catch (e: Exception) {
                        getCategoryColor(category)
                    }
                } else {
                    getCategoryColor(category)
                }

                currentInterval = TimeIntervalBuilder(
                    startTimeMinutes = startMinutes,
                    endTimeMinutes = endMinutes,
                    title = title,
                    categoryName = category,
                    colorHex = colorHex,
                    isCompleted = isCompleted
                )
            } else if (trimmed.startsWith("- Заметки:") || trimmed.startsWith("  - Заметки:")) {
                val noteText = trimmed.substringAfter("Заметки:").trim()
                currentInterval?.notes = noteText
            }
        }

        // Add last builder
        currentInterval?.build(currentDate)?.let { result.add(it) }

        return result
    }

    private fun parseTimeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size == 2) {
            val h = parts[0].toIntOrNull() ?: 0
            val m = parts[1].toIntOrNull() ?: 0
            return (h.coerceIn(0, 23) * 60) + m.coerceIn(0, 59)
        }
        return 0
    }

    private fun getCategoryColor(categoryName: String): Long {
        return DefaultCategory.list.find { it.name.equals(categoryName, ignoreCase = true) }?.colorHex
            ?: 0xFF64748B
    }

    private class TimeIntervalBuilder(
        val startTimeMinutes: Int,
        val endTimeMinutes: Int,
        val title: String,
        val categoryName: String,
        val colorHex: Long,
        val isCompleted: Boolean,
        var notes: String = ""
    ) {
        fun build(date: String): TimeInterval {
            return TimeInterval(
                date = date,
                startTimeMinutes = startTimeMinutes,
                endTimeMinutes = endTimeMinutes,
                title = title,
                categoryName = categoryName,
                colorHex = colorHex,
                isCompleted = isCompleted,
                notes = notes
            )
        }
    }
}
