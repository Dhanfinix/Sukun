package dhanfinix.android.sukun

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import dhanfinix.android.sukun.core.designsystem.SukunTheme
import dhanfinix.android.sukun.core.notification.NotificationHelper
import dhanfinix.android.sukun.navigation.AppNavigation

class MainActivity : ComponentActivity() {

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