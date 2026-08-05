package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.widget.RemoteViews
import com.example.R
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.model.TimeInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

class DayCircleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        when (action) {
            ACTION_PREV_DAY -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val currentOffset = prefs.getInt(KEY_OFFSET_PREFIX + appWidgetId, 0)
                    prefs.edit().putInt(KEY_OFFSET_PREFIX + appWidgetId, currentOffset - 1).apply()
                    updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
            }
            ACTION_NEXT_DAY -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val currentOffset = prefs.getInt(KEY_OFFSET_PREFIX + appWidgetId, 0)
                    prefs.edit().putInt(KEY_OFFSET_PREFIX + appWidgetId, currentOffset + 1).apply()
                    updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
            }
            ACTION_RESET_TODAY -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    prefs.edit().putInt(KEY_OFFSET_PREFIX + appWidgetId, 0).apply()
                    updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
                }
            }
            ACTION_UPDATE_ALL_WIDGETS -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, DayCircleWidgetProvider::class.java)
                )
                for (id in appWidgetIds) {
                    updateWidget(context, appWidgetManager, id)
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove(KEY_OFFSET_PREFIX + appWidgetId)
        }
        editor.apply()
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val offset = prefs.getInt(KEY_OFFSET_PREFIX + appWidgetId, 0)

        val targetDate = LocalDate.now().plusDays(offset.toLong())
        val isToday = (offset == 0)

        val ruLocale = Locale("ru")
        val dateTitle = when (offset) {
            0 -> "Сегодня, " + targetDate.format(DateTimeFormatter.ofPattern("d MMM", ruLocale))
            1 -> "Завтра, " + targetDate.format(DateTimeFormatter.ofPattern("d MMM", ruLocale))
            -1 -> "Вчера, " + targetDate.format(DateTimeFormatter.ofPattern("d MMM", ruLocale))
            else -> targetDate.format(DateTimeFormatter.ofPattern("EEE, d MMM", ruLocale)).replaceFirstChar { it.uppercase() }
        }

        val dateStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Fetch intervals for target date
        val intervals: List<TimeInterval> = runBlocking(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                db.timeIntervalDao().getIntervalsForDateSync(dateStr)
            } catch (e: Exception) {
                emptyList()
            }
        }

        val wheelBitmap = renderWheelBitmap(intervals, isToday)

        val views = RemoteViews(context.packageName, R.layout.widget_day_circle)
        views.setTextViewText(R.id.tv_widget_date, dateTitle)
        views.setImageViewBitmap(R.id.iv_widget_wheel, wheelBitmap)

        val completedCount = intervals.count { it.isCompleted }
        val infoText = if (intervals.isEmpty()) {
            "Записей нет • Открыть"
        } else {
            "Записей: ${intervals.size} (Выполнено: $completedCount)"
        }
        views.setTextViewText(R.id.tv_widget_info, infoText)

        // PendingIntent for PREV_DAY
        val prevIntent = Intent(context, DayCircleWidgetProvider::class.java).apply {
            action = ACTION_PREV_DAY
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val prevPending = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 1,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_prev, prevPending)

        // PendingIntent for NEXT_DAY
        val nextIntent = Intent(context, DayCircleWidgetProvider::class.java).apply {
            action = ACTION_NEXT_DAY
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val nextPending = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_next, nextPending)

        // PendingIntent for RESET_TODAY
        val todayIntent = Intent(context, DayCircleWidgetProvider::class.java).apply {
            action = ACTION_RESET_TODAY
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val todayPending = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 3,
            todayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_widget_today, todayPending)

        // PendingIntent to open MainActivity
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPending = PendingIntent.getActivity(
            context,
            appWidgetId * 10 + 4,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.iv_widget_wheel, appPending)
        views.setOnClickPendingIntent(R.id.tv_widget_date, appPending)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun renderWheelBitmap(intervals: List<TimeInterval>, isToday: Boolean): Bitmap {
        val size = 500
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val cx = size / 2f
        val cy = size / 2f
        val outerRadius = size * 0.44f
        val innerRadius = size * 0.22f
        val hubRadius = size * 0.20f

        val outerRect = RectF(cx - outerRadius, cy - outerRadius, cx + outerRadius, cy + outerRadius)
        val innerRect = RectF(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius)

        // Background ring paint
        val bgRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#262836")
            style = Paint.Style.FILL
        }

        // Base empty ring
        val baseRingPath = Path().apply {
            addOval(outerRect, Path.Direction.CW)
            addOval(innerRect, Path.Direction.CCW)
        }
        canvas.drawPath(baseRingPath, bgRingPaint)

        // Draw interval arcs
        for (interval in intervals) {
            val startAngle = 90f + (interval.startTimeMinutes / 1440f) * 360f
            val duration = interval.durationMinutes
            val sweepAngle = (duration / 1440f) * 360f

            val sectorPath = Path().apply {
                arcTo(outerRect, startAngle, sweepAngle, false)
                arcTo(innerRect, startAngle + sweepAngle, -sweepAngle, false)
                close()
            }

            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = interval.colorHex.toInt()
                style = Paint.Style.FILL
                alpha = if (interval.isCompleted) 255 else 180
            }
            canvas.drawPath(sectorPath, fillPaint)

            // Outline / hatching for planned vs completed
            if (!interval.isCompleted) {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    pathEffect = DashPathEffect(floatArrayOf(8f, 8f), 0f)
                }
                canvas.drawPath(sectorPath, strokePaint)
            } else {
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#33FFFFFF")
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                }
                canvas.drawPath(sectorPath, borderPaint)
            }
        }

        // Draw hour tick lines (24 hours)
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B5563")
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        val mainTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9CA3AF")
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }

        for (h in 0 until 24) {
            val angle = 90f + (h / 24f) * 360f
            val rad = Math.toRadians(angle.toDouble())
            val isMain = (h % 6 == 0)

            val p1x = cx + (innerRadius - 4f) * cos(rad).toFloat()
            val p1y = cy + (innerRadius - 4f) * sin(rad).toFloat()
            val p2x = cx + (outerRadius + 4f) * cos(rad).toFloat()
            val p2y = cy + (outerRadius + 4f) * sin(rad).toFloat()

            canvas.drawLine(p1x, p1y, p2x, p2y, if (isMain) mainTickPaint else tickPaint)
        }

        // Draw Hour labels (00, 06, 12, 18)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D1D5DB")
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val labelRadius = outerRadius + 22f
        val hourLabels = listOf(0 to "00", 6 to "06", 12 to "12", 18 to "18")
        for ((hour, text) in hourLabels) {
            val angle = 90f + (hour / 24f) * 360f
            val rad = Math.toRadians(angle.toDouble())
            val tx = cx + labelRadius * cos(rad).toFloat()
            val ty = cy + labelRadius * sin(rad).toFloat() + 8f
            canvas.drawText(text, tx, ty, textPaint)
        }

        // Center hub
        val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1F2937")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, hubRadius, hubPaint)

        val hubBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#374151")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawCircle(cx, cy, hubRadius, hubBorder)

        // Center text in hub
        val centerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText("${intervals.size}", cx, cy - 4f, centerTextPaint)

        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#9CA3AF")
            textSize = 18f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("записей", cx, cy + 22f, subTextPaint)

        // Current time red hand if today
        if (isToday) {
            val now = LocalTime.now()
            val currentMinutes = now.hour * 60 + now.minute + (now.second / 60f)
            val nowAngle = 90f + (currentMinutes / 1440f) * 360f
            val nowRad = Math.toRadians(nowAngle.toDouble())

            val handStartRadius = hubRadius - 6f
            val handEndRadius = outerRadius + 14f

            val hx1 = cx + handStartRadius * cos(nowRad).toFloat()
            val hy1 = cy + handStartRadius * sin(nowRad).toFloat()
            val hx2 = cx + handEndRadius * cos(nowRad).toFloat()
            val hy2 = cy + handEndRadius * sin(nowRad).toFloat()

            val redHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EF4444")
                strokeWidth = 7f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
            }
            canvas.drawLine(hx1, hy1, hx2, hy2, redHandPaint)

            // Red cap dot at end
            val redDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EF4444")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(hx2, hy2, 8f, redDotPaint)
        }

        return bitmap
    }

    companion object {
        const val PREFS_NAME = "DayCircleWidgetPrefs"
        const val KEY_OFFSET_PREFIX = "widget_offset_"

        const val ACTION_PREV_DAY = "com.example.widget.ACTION_PREV_DAY"
        const val ACTION_NEXT_DAY = "com.example.widget.ACTION_NEXT_DAY"
        const val ACTION_RESET_TODAY = "com.example.widget.ACTION_RESET_TODAY"
        const val ACTION_UPDATE_ALL_WIDGETS = "com.example.widget.ACTION_UPDATE_ALL_WIDGETS"

        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, DayCircleWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_ALL_WIDGETS
            }
            context.sendBroadcast(intent)
        }
    }
}
