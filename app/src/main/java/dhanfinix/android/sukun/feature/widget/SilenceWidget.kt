package dhanfinix.android.sukun.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.widget.RemoteViews
import dhanfinix.android.sukun.MainActivity
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.datastore.AppTheme
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.worker.SilenceReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SilenceWidget : AppWidgetProvider() {

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
                    primary = "#D4B49A"
                )
            } else {
                WidgetColors(
                    surface = "#FAF6F0",
                    onSurface = "#1C1611",
                    primary = "#6B5042"
                )
            }

            val silenceEndTime = userPrefs.silenceEndTime.first()
            val now = System.currentTimeMillis()
            val isSukunActive = silenceEndTime > now
            val remainingMin = if (isSukunActive) ((silenceEndTime - now) / 60000).toInt().coerceAtLeast(1) else 0

            val views = RemoteViews(context.packageName, R.layout.widget_silence)

            // Dynamic color palette for status text
            val statusColor = if (isDark) "#FFB4AB" else "#BA1A1A" // App's primary red colors

            // Theme root
            views.setInt(R.id.silence_widget_root, "setBackgroundColor", Color.parseColor(colors.surface))
            views.setTextColor(R.id.silence_widget_label, Color.parseColor(colors.primary))
            views.setInt(R.id.silence_widget_icon, "setColorFilter", Color.parseColor(colors.primary))

            if (isSukunActive) {
                // STOP action - Redesigned "Active" state
                val stopIntent = Intent(context, SilenceReceiver::class.java).apply {
                    action = SilenceReceiver.ACTION_STOP_SILENCE
                }
                val pendingIntent = PendingIntent.getBroadcast(context, 101, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.silence_widget_root, pendingIntent)
                
                // Use the new soft-red rectangular backgrounds
                val stopBg = if (isDark) R.drawable.widget_silence_stop_bg_dark else R.drawable.widget_silence_stop_bg_light
                views.setInt(R.id.silence_widget_root, "setBackgroundResource", stopBg)
                
                views.setImageViewResource(R.id.silence_widget_icon, R.drawable.outline_volume_off_24)
                views.setInt(R.id.silence_widget_icon, "setColorFilter", Color.parseColor(statusColor))
                
                views.setTextViewText(R.id.silence_widget_label, context.getString(R.string.silence_active))
                views.setTextColor(R.id.silence_widget_label, Color.parseColor(statusColor))
                
                // Show countdown subtext
                views.setViewVisibility(R.id.silence_widget_subtext, android.view.View.VISIBLE)
                views.setTextViewText(R.id.silence_widget_subtext, "${context.getString(R.string.notif_ends_in)} ${remainingMin}m")
                views.setTextColor(R.id.silence_widget_subtext, Color.parseColor(statusColor))
            } else {
                // START action -> Open Overlay Bottom Sheet
                val startIntent = Intent(context, ManualSilenceOverlayActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(context, 102, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.silence_widget_root, pendingIntent)
                
                // Explicitly restore the standard background to fix transparency issues
                views.setInt(R.id.silence_widget_root, "setBackgroundResource", R.drawable.widget_background)
                // Re-apply background color override to the restored resource
                views.setInt(R.id.silence_widget_root, "setBackgroundColor", Color.parseColor(colors.surface))
                
                views.setImageViewResource(R.id.silence_widget_icon, R.drawable.outline_volume_off_24)
                views.setTextViewText(R.id.silence_widget_label, context.getString(R.string.manual_silent))
                views.setViewVisibility(R.id.silence_widget_subtext, android.view.View.GONE)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    data class WidgetColors(
        val surface: String,
        val onSurface: String,
        val primary: String
    )

    companion object {
        fun update(context: Context) {
            val intent = Intent(context, SilenceWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, SilenceWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
}
