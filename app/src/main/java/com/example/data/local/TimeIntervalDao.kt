package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TimeInterval
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeIntervalDao {
    @Query("SELECT * FROM time_intervals WHERE date = :date ORDER BY startTimeMinutes ASC")
    fun getIntervalsForDate(date: String): Flow<List<TimeInterval>>

    @Query("SELECT * FROM time_intervals WHERE date = :date ORDER BY startTimeMinutes ASC")
    suspend fun getIntervalsForDateSync(date: String): List<TimeInterval>

    @Query("SELECT * FROM time_intervals ORDER BY date ASC, startTimeMinutes ASC")
    fun getAllIntervals(): Flow<List<TimeInterval>>

    @Query("SELECT * FROM time_intervals ORDER BY date ASC, startTimeMinutes ASC")
    suspend fun getAllIntervalsList(): List<TimeInterval>

    @Query("SELECT DISTINCT date FROM time_intervals ORDER BY date DESC")
    fun getAllDates(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterval(interval: TimeInterval): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntervals(intervals: List<TimeInterval>): List<Long>

    @Update
    suspend fun updateInterval(interval: TimeInterval)

    @Delete
    suspend fun deleteInterval(interval: TimeInterval)

    @Query("DELETE FROM time_intervals WHERE id = :id")
    suspend fun deleteIntervalById(id: Long)

    @Query("DELETE FROM time_intervals WHERE date = :date")
    suspend fun deleteIntervalsForDate(date: String)
}
