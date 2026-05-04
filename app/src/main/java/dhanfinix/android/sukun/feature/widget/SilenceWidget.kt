package dhanfinix.android.sukun.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.core.designsystem.SukunGlanceTheme
import dhanfinix.android.sukun.worker.SilenceReceiver
import kotlinx.coroutines.flow.first

class SilenceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SilenceWidget()
}

class SilenceWidget : GlanceAppWidget() {

    companion object {
        /**
         * Standard update method for non-suspend callers.
         */
        fun update(context: Context) {
            WidgetUpdateCoordinator.refreshSilenceAsync(context)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val userPrefs = UserPreferences(context)
        val initialUseDynamicColor = userPrefs.useDynamicColor.first()
        val initialSilenceEndTime = userPrefs.silenceEndTime.first()
        
        provideContent {
            val useDynamicColor by userPrefs.useDynamicColor.collectAsState(initial = initialUseDynamicColor)
            val silenceEndTime by userPrefs.silenceEndTime.collectAsState(initial = initialSilenceEndTime)
            
            SukunGlanceTheme(useDynamicColor = useDynamicColor) {
                SilenceWidgetContent(silenceEndTime = silenceEndTime)
            }
        }
    }

    @Composable
    private fun SilenceWidgetContent(silenceEndTime: Long) {
        val context = LocalContext.current
        val now = System.currentTimeMillis()
        val isSukunActive = silenceEndTime > now
        val remainingMin = if (isSukunActive) ((silenceEndTime - now) / 60000).toInt().coerceAtLeast(1) else 0

        // Use dynamic color tints for the "Active" state to match Material You
        val colors = GlanceTheme.colors
        
        val backgroundColor = if (isSukunActive) {
            // Use error container for the "Active" (alert/stop) state
            colors.errorContainer
        } else {
            colors.surface
        }

        val contentColor = if (isSukunActive) {
            colors.onErrorContainer
        } else {
            colors.primary
        }

        val subtextColor = if (isSukunActive) {
            colors.onErrorContainer
        } else {
            colors.onSurfaceVariant
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(backgroundColor)
                .cornerRadius(16.dp)
                .padding(8.dp)
                .clickable(
                    if (isSukunActive) {
                        actionRunCallback<StopSilenceCallback>()
                    } else {
                        actionStartActivity(Intent(context, ManualSilenceOverlayActivity::class.java))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.outline_volume_off_24),
                    contentDescription = null,
                    modifier = GlanceModifier.size(32.dp),
                    colorFilter = androidx.glance.ColorFilter.tint(contentColor)
                )

                Spacer(modifier = GlanceModifier.height(4.dp))

                Text(
                    text = context.getString(if (isSukunActive) R.string.silence_active else R.string.manual_silent),
                    style = TextStyle(
                        color = contentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )

                if (isSukunActive) {
                    Text(
                        text = "${context.getString(R.string.notif_ends_in)} ${remainingMin}m",
                        style = TextStyle(
                            color = subtextColor,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

class StopSilenceCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val stopIntent = Intent(context, SilenceReceiver::class.java).apply {
            action = SilenceReceiver.ACTION_STOP_SILENCE
        }
        context.sendBroadcast(stopIntent)
        // Widget will update automatically when receiver updates DataStore
    }
}
