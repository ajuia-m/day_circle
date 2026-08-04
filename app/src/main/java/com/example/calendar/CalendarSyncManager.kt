package com.example.calendar

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.data.model.SyncOption
import com.example.data.model.TimeInterval
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

data class DeviceCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val isPrimary: Boolean
)

class CalendarSyncManager(private val context: Context) {

    fun hasCalendarPermission(): Boolean {
        val readPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writePerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readPerm && writePerm
    }

    fun getDeviceCalendars(): List<DeviceCalendar> {
        if (!hasCalendarPermission()) return emptyList()

        val calendars = mutableListOf<DeviceCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.IS_PRIMARY
        )

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )

            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accIdx = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val primaryIdx = it.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val name = it.getString(nameIdx) ?: "Календарь $id"
                    val account = it.getString(accIdx) ?: ""
                    val isPrimary = if (primaryIdx != -1) it.getInt(primaryIdx) == 1 else false
                    calendars.add(DeviceCalendar(id, name, account, isPrimary))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return calendars
    }

    fun syncIntervalToCalendar(
        calendarId: Long,
        interval: TimeInterval,
        syncOption: SyncOption
    ): Long? {
        if (!hasCalendarPermission()) return null

        // Check sync options criteria
        val shouldSync = when (syncOption) {
            SyncOption.PLANNED_ONLY -> !interval.isCompleted
            SyncOption.COMPLETED_ONLY -> interval.isCompleted
            SyncOption.BOTH -> true
            SyncOption.NONE -> false
        }

        if (!shouldSync) {
            // If shouldn't sync, and already synced, delete existing calendar event
            interval.calendarEventId?.let { deleteEventFromCalendar(it) }
            return null
        }

        val localDate = try {
            LocalDate.parse(interval.date)
        } catch (e: Exception) {
            LocalDate.now()
        }

        val startHour = interval.startTimeMinutes / 60
        val startMin = interval.startTimeMinutes % 60
        val endHour = interval.endTimeMinutes / 60
        val endMin = interval.endTimeMinutes % 60

        val startDateTime = localDate.atTime(startHour % 24, startMin)
        val endDateTime = if (interval.endTimeMinutes >= 1440) {
            localDate.plusDays(1).atTime(0, 0)
        } else {
            localDate.atTime(endHour % 24, endMin)
        }

        val zoneId = ZoneId.systemDefault()
        val startMillis = startDateTime.atZone(zoneId).toInstant().toEpochMilli()
        val endMillis = endDateTime.atZone(zoneId).toInstant().toEpochMilli()

        val statusPrefix = if (interval.isCompleted) "[Совершено] " else "[Запланировано] "
        val eventTitle = "$statusPrefix${interval.title} (${interval.categoryName})"

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.TITLE, eventTitle)
            put(CalendarContract.Events.DESCRIPTION, interval.notes.ifBlank { "Синхронизировано из приложения 'Круг Дня'" })
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        return try {
            if (interval.calendarEventId != null) {
                // Update existing event
                val updateUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, interval.calendarEventId)
                val updatedRows = context.contentResolver.update(updateUri, values, null, null)
                if (updatedRows > 0) {
                    interval.calendarEventId
                } else {
                    // Re-insert if update failed
                    insertNewEvent(values)
                }
            } else {
                insertNewEvent(values)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun insertNewEvent(values: ContentValues): Long? {
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        return uri?.lastPathSegment?.toLongOrNull()
    }

    fun deleteEventFromCalendar(eventId: Long): Boolean {
        if (!hasCalendarPermission()) return false
        return try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val rows = context.contentResolver.delete(deleteUri, null, null)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
