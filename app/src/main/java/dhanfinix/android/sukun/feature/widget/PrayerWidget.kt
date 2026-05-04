package dhanfinix.android.sukun.feature.widget

import androidx.glance.appwidget.updateAll

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dhanfinix.android.sukun.MainActivity
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.datastore.UserPreferences
import dhanfinix.android.sukun.core.designsystem.SukunGlanceTheme
import dhanfinix.android.sukun.feature.prayer.data.PrayerRepository
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()
}

class PrayerWidget : GlanceAppWidget() {

    companion object {
        /**
         * Standard update method for non-suspend callers (e.g. ViewModel).
         */
        fun update(context: Context) {
            WidgetUpdateCoordinator.refreshPrayerAsync(context)
        }
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val userPrefs = UserPreferences(context)
        val repository = PrayerRepository(context)

        val lat = userPrefs.latitude.first()
        val lng = userPrefs.longitude.first()
        val method = userPrefs.calculationMethod.first()
        val offsets = userPrefs.prayerOffsets.first()
        val locationName = userPrefs.locationName.first() ?: context.getString(R.string.jakarta)

        val result = repository.getPrayerTimes(LocalDate.now(), lat, lng, method, offsets)
        val timings = result.getOrNull() ?: emptyMap()

        val useDynamicColorInit = userPrefs.useDynamicColor.first()

        provideContent {
            val useDynamicColor by userPrefs.useDynamicColor.collectAsState(initial = useDynamicColorInit)

            SukunGlanceTheme(useDynamicColor = useDynamicColor) {
                PrayerWidgetContent(
                    timings = timings,
                    locationName = locationName
                )
            }
        }
    }

    @Composable
    private fun PrayerWidgetContent(
        timings: Map<PrayerName, String>,
        locationName: String
    ) {
        val context = LocalContext.current
        val nextPrayer = findNextPrayer(timings)
        val colors = GlanceTheme.colors

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.surface)
                .cornerRadius(16.dp)
                .padding(8.dp)
                .clickable(
                    actionStartActivity(
                        Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                    )
                )
        ) {
            // Prayers Row
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    PrayerName.FAJR,
                    PrayerName.DHUHR,
                    PrayerName.ASR,
                    PrayerName.MAGHRIB,
                    PrayerName.ISHA
                ).forEach { prayer ->
                    PrayerSlot(
                        prayer = prayer,
                        time = timings[prayer] ?: "--:--",
                        isNext = prayer == nextPrayer?.first,
                        modifier = GlanceModifier.defaultWeight()
                    )
                }
            }

            // Footer
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
                val dateStr = LocalDate.now().format(dateFormatter)

                Text(
                    text = locationName,
                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = 8.sp),
                    modifier = GlanceModifier.defaultWeight()
                )

                Text(
                    text = "Sukun",
                    style = TextStyle(
                        color = colors.primary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.padding(horizontal = 4.dp)
                )

                Text(
                    text = dateStr,
                    style = TextStyle(
                        color = colors.onSurfaceVariant,
                        fontSize = 8.sp,
                        textAlign = androidx.glance.text.TextAlign.End
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }

    @Composable
    private fun PrayerSlot(
        prayer: PrayerName,
        time: String,
        isNext: Boolean,
        modifier: GlanceModifier = GlanceModifier
    ) {
        val context = LocalContext.current
        val colors = GlanceTheme.colors
        
        val isFriday = LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY
        val displayName = if (prayer == PrayerName.DHUHR && isFriday) {
            context.getString(R.string.prayer_jumuah)
        } else {
            context.getString(prayer.nameRes)
        }

        val nameColor = if (isNext) colors.onPrimaryContainer else colors.primary
        val timeColor = if (isNext) colors.onPrimaryContainer else colors.onSurface
        val slotModifier = modifier
            .fillMaxHeight()
            .padding(2.dp)
            .let {
                if (isNext) {
                    it.background(colors.primaryContainer).cornerRadius(12.dp)
                } else {
                    it.background(Color.Transparent)
                }
            }

        Box(
            modifier = slotModifier,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = displayName,
                    style = TextStyle(
                        color = nameColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = time,
                    style = TextStyle(
                        color = timeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
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
        
        return sortedPrayers.firstOrNull()?.let { it to (timings[it] ?: "--:--") }
    }
}
