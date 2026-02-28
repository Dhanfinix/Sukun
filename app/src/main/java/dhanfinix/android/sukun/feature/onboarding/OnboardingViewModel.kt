package dhanfinix.android.sukun.feature.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dhanfinix.android.sukun.core.datastore.UserPreferences
import kotlinx.coroutines.launch

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val userPrefs = UserPreferences(application)

    fun markOnboardingCompleted() {
        viewModelScope.launch {
            userPrefs.setOnboardingCompleted(true)
        }
    }
}
