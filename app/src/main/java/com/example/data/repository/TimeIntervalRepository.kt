package com.example.data.repository

import com.example.calendar.CalendarSyncManager
import com.example.data.local.TimeIntervalDao
import com.example.data.model.SyncOption
import com.example.data.model.TimeInterval
import kotlinx.coroutines.flow.Flow

class TimeIntervalRepository(
    private val dao: TimeIntervalDao,
    private val calendarSyncManager: CalendarSyncManager
) {
    fun getIntervalsForDate(date: String): Flow<List<TimeInterval>> =
        dao.getIntervalsForDate(date)

    suspend fun getIntervalsForDateSync(date: String): List<TimeInterval> =
        dao.getIntervalsForDateSync(date)

    fun getAllIntervals(): Flow<List<TimeInterval>> =
        dao.getAllIntervals()

    suspend fun getAllIntervalsList(): List<TimeInterval> =
        dao.getAllIntervalsList()

    suspend fun saveInterval(
        interval: TimeInterval,
        autoSync: Boolean,
        calendarId: Long?,
        syncOption: SyncOption
    ): Long {
        var eventId: Long? = interval.calendarEventId

        if (autoSync && calendarId != null && calendarSyncManager.hasCalendarPermission()) {
            eventId = calendarSyncManager.syncIntervalToCalendar(calendarId, interval, syncOption)
        }

        val updatedInterval = interval.copy(calendarEventId = eventId)
        val id = dao.insertInterval(updatedInterval)
        return id
    }

    suspend fun toggleCompleted(
        interval: TimeInterval,
        autoSync: Boolean,
        calendarId: Long?,
        syncOption: SyncOption
    ) {
        val updated = interval.copy(isCompleted = !interval.isCompleted)
        saveInterval(updated, autoSync, calendarId, syncOption)
    }

    suspend fun deleteInterval(interval: TimeInterval) {
        if (interval.calendarEventId != null && calendarSyncManager.hasCalendarPermission()) {
            calendarSyncManager.deleteEventFromCalendar(interval.calendarEventId)
        }
        dao.deleteInterval(interval)
    }

    suspend fun importIntervals(
        intervals: List<TimeInterval>,
        autoSync: Boolean,
        calendarId: Long?,
        syncOption: SyncOption
    ) {
        intervals.forEach { interval ->
            saveInterval(interval, autoSync, calendarId, syncOption)
        }
    }
}
