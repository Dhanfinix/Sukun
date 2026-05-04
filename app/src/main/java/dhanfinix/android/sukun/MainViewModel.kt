package dhanfinix.android.sukun

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dhanfinix.android.sukun.core.datastore.AppLanguage
import dhanfinix.android.sukun.core.datastore.AppTheme
import dhanfinix.android.sukun.core.datastore.TimeFormat
import dhanfinix.android.sukun.core.datastore.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * Global ViewModel for MainActivity.
 * Responsible for:
 * 1. Holding the Splash Screen until DataStore is ready.
 * 2. Providing the onboarding status for initial navigation.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val userPrefs = UserPreferences(application)

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _showSilenceSheet = MutableStateFlow(false)
    val showSilenceSheet: StateFlow<Boolean> = _showSilenceSheet.asStateFlow()

    fun triggerSilenceSheet() {
        _showSilenceSheet.value = true
    }

    fun onSilenceSheetConsumed() {
        _showSilenceSheet.value = false
    }

    val isOnboardingCompleted: StateFlow<Boolean> = userPrefs.isOnboardingCompleted
        .onEach { _isReady.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val appTheme: StateFlow<AppTheme> = userPrefs.appTheme
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    val useDynamicColor: StateFlow<Boolean> = userPrefs.useDynamicColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val appLanguage: StateFlow<AppLanguage> = userPrefs.appLanguage
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppLanguage.SYSTEM
        )



    val timeFormat: StateFlow<TimeFormat> = userPrefs.timeFormat
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TimeFormat.AUTO
        )

    val isReminderEnabled: StateFlow<Boolean> = userPrefs.isReminderEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val reminderMinutes: StateFlow<Int> = userPrefs.reminderMinutes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 10
        )

    val silenceExtendMinutes: StateFlow<Int> = userPrefs.silenceExtendMinutes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 5
        )

    val hasSeenLanding: StateFlow<Boolean> = userPrefs.hasSeenLanding
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val hasSeenHomeCoachmark: StateFlow<Boolean> = userPrefs.hasSeenHomeCoachmark
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            userPrefs.setAppTheme(theme)
            dhanfinix.android.sukun.feature.widget.WidgetUpdateCoordinator.refreshAll(getApplication())
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            userPrefs.setAppLanguage(language)
        }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            userPrefs.setTimeFormat(format)
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setReminderEnabled(enabled)
        }
    }

    fun setReminderMinutes(minutes: Int) {
        viewModelScope.launch {
            userPrefs.setReminderMinutes(minutes)
        }
    }

    fun setSilenceExtendMinutes(minutes: Int) {
        viewModelScope.launch {
            userPrefs.setSilenceExtendMinutes(minutes)
        }
    }

    fun setUseDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setUseDynamicColor(enabled)
            dhanfinix.android.sukun.feature.widget.WidgetUpdateCoordinator.refreshAll(getApplication())
        }
    }

    fun setHasSeenLanding(seen: Boolean) {
        viewModelScope.launch {
            userPrefs.setHasSeenLanding(seen)
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            userPrefs.setOnboardingCompleted(completed)
        }
    }

    fun setCoachmarkShown(shown: Boolean) {
        viewModelScope.launch {
            userPrefs.setHomeCoachmarkShown(shown)
        }
    }

    // ── In-App Review ──

    val shouldShowReview: StateFlow<Boolean> = userPrefs.shouldShowReview
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val shouldShowDonation: StateFlow<Boolean> = userPrefs.shouldShowDonation
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        viewModelScope.launch {
            userPrefs.incrementAppOpenCount()
        }
    }

    fun markAsRated() {
        viewModelScope.launch {
            userPrefs.setHasRated(true)
        }
    }

    fun markDonationAsShown() {
        viewModelScope.launch {
            val currentCount = userPrefs.appOpenCount.first()
            userPrefs.setLastDonationShownCount(currentCount)
        }
    }

    fun disableDonation() {
        viewModelScope.launch {
            userPrefs.setDonationDisabled(true)
        }
    }
}
