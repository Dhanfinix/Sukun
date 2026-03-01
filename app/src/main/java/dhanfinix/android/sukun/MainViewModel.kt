package dhanfinix.android.sukun

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dhanfinix.android.sukun.core.datastore.AppTheme
import dhanfinix.android.sukun.core.datastore.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val hasSeenHomeCoachmark: StateFlow<Boolean> = userPrefs.hasSeenHomeCoachmark
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            userPrefs.setAppTheme(theme)
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
}
