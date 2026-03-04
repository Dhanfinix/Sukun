package dhanfinix.android.sukun

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dhanfinix.android.sukun.core.datastore.AppLanguage
import dhanfinix.android.sukun.core.designsystem.SukunTheme
import dhanfinix.android.sukun.core.notification.NotificationHelper
import dhanfinix.android.sukun.navigation.AppNavigation
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val mainVm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Create notification channel early
        NotificationHelper.createChannel(this)

        // DND access is now requested gracefully in OnboardingScreen!
        
        setContent {
            val isOnboardingCompleted by mainVm.isOnboardingCompleted.collectAsState()
            val isReady by mainVm.isReady.collectAsState()
            val appTheme by mainVm.appTheme.collectAsState()
            val useDynamicColor by mainVm.useDynamicColor.collectAsState()
            val appLanguage by mainVm.appLanguage.collectAsState()

            LaunchedEffect(appLanguage) {
                val localeList = when (appLanguage) {
                    AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
                    AppLanguage.ID -> LocaleListCompat.forLanguageTags("in")
                    AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                }
                AppCompatDelegate.setApplicationLocales(localeList)
            }
            
            SukunTheme(appTheme = appTheme, dynamicColor = useDynamicColor) {
                AppNavigation(
                    mainVm = mainVm,
                    isOnboardingCompleted = isOnboardingCompleted,
                    isReady = isReady
                )
            }
        }
    }
}