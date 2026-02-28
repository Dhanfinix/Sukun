package dhanfinix.android.sukun.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName // Temp: will be fixed later

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sukun_prefs")

/**
 * DataStore-backed repository for persisting user settings.
 * Single source of truth for prayer toggles, silence duration, location, and saved volumes.
 */
class UserPreferences(private val context: Context) {

    // ── Prayer toggle keys ──
    private val KEY_FAJR_ENABLED = booleanPreferencesKey("fajr_enabled")
    private val KEY_DHUHR_ENABLED = booleanPreferencesKey("dhuhr_enabled")
    private val KEY_ASR_ENABLED = booleanPreferencesKey("asr_enabled")
    private val KEY_MAGHRIB_ENABLED = booleanPreferencesKey("maghrib_enabled")
    private val KEY_ISHA_ENABLED = booleanPreferencesKey("isha_enabled")

    // ── Duration ──
    private val KEY_SILENCE_DURATION = intPreferencesKey("silence_duration_min")

    // ── Location ──
    private val KEY_LATITUDE = doublePreferencesKey("latitude")
    private val KEY_LONGITUDE = doublePreferencesKey("longitude")
    private val KEY_LOCATION_NAME = androidx.datastore.preferences.core.stringPreferencesKey("location_name")

    // ── Calculation method ──
    private val KEY_METHOD = intPreferencesKey("calculation_method")

    // ── Saved volume levels (for restore after silence) ──
    private val KEY_SAVED_MEDIA = intPreferencesKey("saved_media_vol")
    private val KEY_SAVED_RING = intPreferencesKey("saved_ring_vol")
    private val KEY_SAVED_NOTIFICATION = intPreferencesKey("saved_notification_vol")
    private val KEY_SAVED_ALARM = intPreferencesKey("saved_alarm_vol")

    // ── Silence Metadata ──
    private val KEY_SILENCE_END_TIME = longPreferencesKey("silence_end_time")
    private val KEY_SILENCE_LABEL = stringPreferencesKey("silence_label")
    private val KEY_SILENCE_MODE = stringPreferencesKey("silence_mode")

    // ── VOIP toggle ──
    private val KEY_VOIP_LINKED = booleanPreferencesKey("voip_linked")
    private val KEY_NOTIF_LINKED = booleanPreferencesKey("is_notif_linked")

    // ── Onboarding ──
    private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

    // ── App Settings ──
    private val KEY_APP_THEME = stringPreferencesKey("app_theme")
    private val KEY_HOME_COACH_MARK_SHOWN = booleanPreferencesKey("home_coachmark_shown")

    // ── Flows ──

    val isPrayerEnabled: Flow<Map<PrayerName, Boolean>> = context.dataStore.data.map { prefs ->
        mapOf(
            PrayerName.FAJR to (prefs[KEY_FAJR_ENABLED] ?: true),
            PrayerName.DHUHR to (prefs[KEY_DHUHR_ENABLED] ?: true),
            PrayerName.ASR to (prefs[KEY_ASR_ENABLED] ?: true),
            PrayerName.MAGHRIB to (prefs[KEY_MAGHRIB_ENABLED] ?: true),
            PrayerName.ISHA to (prefs[KEY_ISHA_ENABLED] ?: true)
        )
    }

    val silenceDurationMin: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SILENCE_DURATION] ?: 15
    }

    val latitude: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_LATITUDE] ?: -6.2088 // Default: Jakarta
    }

    val longitude: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_LONGITUDE] ?: 106.8456 // Default: Jakarta
    }

    val locationName: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_LOCATION_NAME]
    }

    val calculationMethod: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_METHOD] ?: 20 // Default: Kemenag
    }

    val isNotifLinked: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIF_LINKED] ?: true // Default to true
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val appTheme: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val themeName = prefs[KEY_APP_THEME] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    val silenceMode: Flow<SilenceMode> = context.dataStore.data.map { prefs ->
        val modeName = prefs[KEY_SILENCE_MODE] ?: SilenceMode.DND.name
        try {
            SilenceMode.valueOf(modeName)
        } catch (e: Exception) {
            SilenceMode.DND
        }
    }

    val hasSeenHomeCoachmark: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HOME_COACH_MARK_SHOWN] ?: false
    }

    // ── Setters ──

    suspend fun setPrayerEnabled(prayer: PrayerName, enabled: Boolean) {
        val key = when (prayer) {
            PrayerName.FAJR -> KEY_FAJR_ENABLED
            PrayerName.DHUHR -> KEY_DHUHR_ENABLED
            PrayerName.ASR -> KEY_ASR_ENABLED
            PrayerName.MAGHRIB -> KEY_MAGHRIB_ENABLED
            PrayerName.ISHA -> KEY_ISHA_ENABLED
        }
        context.dataStore.edit { it[key] = enabled }
    }

    suspend fun setSilenceDuration(minutes: Int) {
        context.dataStore.edit { it[KEY_SILENCE_DURATION] = minutes }
    }

    suspend fun setLocation(lat: Double, lng: Double, name: String? = null) {
        context.dataStore.edit {
            it[KEY_LATITUDE] = lat
            it[KEY_LONGITUDE] = lng
            if (name != null) {
                it[KEY_LOCATION_NAME] = name
            } else {
                it.remove(KEY_LOCATION_NAME)
            }
        }
    }

    suspend fun setCalculationMethod(method: Int) {
        context.dataStore.edit { it[KEY_METHOD] = method }
    }

    suspend fun setNotifLinked(linked: Boolean) {
        context.dataStore.edit { it[KEY_NOTIF_LINKED] = linked }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_COMPLETED] = completed }
    }

    // ── Volume save/restore ──

    suspend fun saveCurrentVolumes(media: Int, ring: Int, notification: Int) {
        context.dataStore.edit {
            it[KEY_SAVED_MEDIA] = media
            it[KEY_SAVED_RING] = ring
            it[KEY_SAVED_NOTIFICATION] = notification
        }
    }

    val savedVolumes: Flow<Triple<Int, Int, Int>> = context.dataStore.data.map { prefs ->
        Triple(
            prefs[KEY_SAVED_MEDIA] ?: -1,
            prefs[KEY_SAVED_RING] ?: -1,
            prefs[KEY_SAVED_NOTIFICATION] ?: -1
        )
    }

    val silenceEndTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_SILENCE_END_TIME] ?: 0L
    }

    val silenceLabel: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SILENCE_LABEL]
    }

    suspend fun setSilenceMetadata(endTime: Long, label: String?) {
        context.dataStore.edit {
            it[KEY_SILENCE_END_TIME] = endTime
            if (label != null) {
                it[KEY_SILENCE_LABEL] = label
            } else {
                it.remove(KEY_SILENCE_LABEL)
            }
        }
    }

    suspend fun saveAllVolumes(media: Int, ring: Int, notif: Int, alarm: Int) {
        context.dataStore.edit {
            it[KEY_SAVED_MEDIA] = media
            it[KEY_SAVED_RING] = ring
            it[KEY_SAVED_NOTIFICATION] = notif
            it[KEY_SAVED_ALARM] = alarm
        }
    }

    val savedAlarmVol: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVED_ALARM] ?: -1
    }

    suspend fun clearSilenceState() {
        context.dataStore.edit {
            it[KEY_SILENCE_END_TIME] = 0L
            it.remove(KEY_SILENCE_LABEL)
            it.remove(KEY_SAVED_MEDIA)
            it.remove(KEY_SAVED_RING)
            it.remove(KEY_SAVED_NOTIFICATION)
            it.remove(KEY_SAVED_ALARM)
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_THEME] = theme.name
        }
    }

    suspend fun setHomeCoachmarkShown(shown: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HOME_COACH_MARK_SHOWN] = shown
        }
    }

    suspend fun setSilenceMode(mode: SilenceMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SILENCE_MODE] = mode.name
        }
    }
}
