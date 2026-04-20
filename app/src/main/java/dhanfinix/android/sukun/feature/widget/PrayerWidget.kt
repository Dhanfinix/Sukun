package dhanfinix.android.sukun.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import dhanfinix.android.sukun.MainActivity
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.datastore.AppTheme
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.feature.prayer.data.PrayerRepository
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class PrayerWidget : AppWidgetProvider() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        scope.launch {
            val userPrefs = UserPreferences(context)
            val repository = PrayerRepository(context)
            
            val appTheme = userPrefs.appTheme.first()
            val isDark = when (appTheme) {
                AppTheme.DARK -> true
                AppTheme.LIGHT -> false
                AppTheme.SYSTEM -> {
                    val uiMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                    uiMode == Configuration.UI_MODE_NIGHT_YES
                }
            }

            val colors = if (isDark) {
                WidgetColors(
                    surface = "#1C1611",
                    onSurface = "#FAF6F0",
                    primary = "#D4B49A",
                    highlight = "#6B5042",
                    onSurfaceVariant = "#C9BFB5"
                )
            } else {
                WidgetColors(
                    surface = "#FAF6F0",
                    onSurface = "#1C1611",
                    primary = "#6B5042",
                    highlight = "#D4B49A",
                    onSurfaceVariant = "#7D7063"
                )
            }
            
            val lat = userPrefs.latitude.first()
            val lng = userPrefs.longitude.first()
            val method = userPrefs.calculationMethod.first()
            val offsets = userPrefs.prayerOffsets.first()
            val locationName = userPrefs.locationName.first() ?: context.getString(R.string.jakarta)

            val result = repository.getPrayerTimes(LocalDate.now(), lat, lng, method, offsets)
            
            val views = RemoteViews(context.packageName, R.layout.widget_prayer)

            // Dynamic theming
            views.setInt(R.id.widget_root, "setBackgroundColor", Color.parseColor(colors.surface))
            // Header & Footer
            views.setTextColor(R.id.prayer_widget_title, Color.parseColor(colors.primary))
            views.setTextColor(R.id.prayer_widget_location, Color.parseColor(colors.onSurfaceVariant))
            views.setTextColor(R.id.prayer_widget_date, Color.parseColor(colors.onSurfaceVariant))

            // Update footer
            views.setTextViewText(R.id.prayer_widget_location, locationName)
            val dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.getDefault())
            views.setTextViewText(R.id.prayer_widget_date, LocalDate.now().format(dateFormatter))

            result.onSuccess { timings ->
                val nextPrayer = findNextPrayer(timings)
                
                // Populate all slots
                updateSlot(context, views, timings, PrayerName.FAJR, R.id.slot_fajr, R.id.fajr_name, R.id.fajr_time, nextPrayer?.first, colors)
                updateSlot(context, views, timings, PrayerName.DHUHR, R.id.slot_dhuhr, R.id.dhuhr_name, R.id.dhuhr_time, nextPrayer?.first, colors)
                updateSlot(context, views, timings, PrayerName.ASR, R.id.slot_asr, R.id.asr_name, R.id.asr_time, nextPrayer?.first, colors)
                updateSlot(context, views, timings, PrayerName.MAGHRIB, R.id.slot_maghrib, R.id.maghrib_name, R.id.maghrib_time, nextPrayer?.first, colors)
                updateSlot(context, views, timings, PrayerName.ISHA, R.id.slot_isha, R.id.isha_name, R.id.isha_time, nextPrayer?.first, colors)
            }

            // Click to open app
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun updateSlot(
        context: Context,
        views: RemoteViews,
        timings: Map<PrayerName, String>,
        prayer: PrayerName,
        slotId: Int,
        nameId: Int,
        timeId: Int,
        nextPrayerName: PrayerName?,
        colors: WidgetColors
    ) {
        val timeStr = timings[prayer] ?: "--:--"
        
        // Handle Dhuhr/Jumu'ah swap
        val isFriday = LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY
        val displayName = if (prayer == PrayerName.DHUHR && isFriday) {
            context.getString(R.string.prayer_jumuah)
        } else {
            context.getString(prayer.nameRes)
        }

        views.setTextViewText(nameId, displayName)
        views.setTextViewText(timeId, timeStr)

        // Highlight if it's the next prayer
        if (prayer == nextPrayerName) {
            val highlightBg = if (colors.surface == "#1C1611") R.drawable.widget_item_next_bg_dark else R.drawable.widget_item_next_bg_light
            views.setInt(slotId, "setBackgroundResource", highlightBg)
            views.setTextColor(nameId, Color.parseColor(colors.onSurface)) 
            views.setTextColor(timeId, Color.parseColor(colors.onSurface)) 
        } else {
            views.setInt(slotId, "setBackgroundResource", 0) // No background
            views.setTextColor(nameId, Color.parseColor(colors.primary)) 
            views.setTextColor(timeId, Color.parseColor(colors.onSurface)) 
        }
    }

    private fun findNextPrayer(timings: Map<PrayerName, String>): Pair<PrayerName, String>? {
        val now = LocalTime.now()
        val sortedPrayers = listOf(
            PrayerName.FAJR,
            PrayerName.DHUHR,
            PrayerName.ASR,
            PrayerName.MAGHRIB,
            PrayerName.ISHA
        )

        for (prayer in sortedPrayers) {
            val timeStr = timings[prayer] ?: continue
            try {
                val prayerTime = LocalTime.parse(timeStr)
                if (prayerTime.isAfter(now)) {
                    return prayer to timeStr
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        val firstPrayer = sortedPrayers.firstOrNull()
        return firstPrayer?.let { it to (timings[it] ?: "--:--") }
    }

    data class WidgetColors(
        val surface: String,
        val onSurface: String,
        val primary: String,
        val highlight: String,
        val onSurfaceVariant: String
    )

    companion object {
        fun update(context: Context) {
            val intent = Intent(context, PrayerWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, PrayerWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
