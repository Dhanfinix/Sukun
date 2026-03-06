package dhanfinix.android.sukun

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import dhanfinix.android.sukun.core.datastore.AppLanguage
import dhanfinix.android.sukun.core.designsystem.SukunTheme
import dhanfinix.android.sukun.core.notification.NotificationHelper
import dhanfinix.android.sukun.navigation.AppNavigation

class MainActivity : AppCompatActivity() {

    private val mainVm: MainViewModel by viewModels()
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateSnackbar = true
        }
    }

    @Volatile
    private var showUpdateSnackbar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Create notification channel early
        NotificationHelper.createChannel(this)

        // Register listener for flexible update progress
        appUpdateManager.registerListener(installStateListener)

        // Check for flexible in-app update
        checkForUpdate()
        
        setContent {
            val isOnboardingCompleted by mainVm.isOnboardingCompleted.collectAsState()
            val isReady by mainVm.isReady.collectAsState()
            val appTheme by mainVm.appTheme.collectAsState()
            val useDynamicColor by mainVm.useDynamicColor.collectAsState()
            val appLanguage by mainVm.appLanguage.collectAsState()
            val shouldShowReview by mainVm.shouldShowReview.collectAsState()

            LaunchedEffect(appLanguage) {
                val localeList = when (appLanguage) {
                    AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
                    AppLanguage.ID -> LocaleListCompat.forLanguageTags("in")
                    AppLanguage.AR -> LocaleListCompat.forLanguageTags("ar")
                    AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                }
                AppCompatDelegate.setApplicationLocales(localeList)
            }

            // Trigger In-App Review after 5th open
            LaunchedEffect(shouldShowReview) {
                if (shouldShowReview) {
                    requestInAppReview()
                }
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

    override fun onResume() {
        super.onResume()
        // If the update was downloaded while the app was in background, complete it
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                appUpdateManager.completeUpdate()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        appUpdateManager.unregisterListener(installStateListener)
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlow(
                    info,
                    this,
                    AppUpdateOptions.defaultOptions(AppUpdateType.FLEXIBLE)
                )
            }
        }.addOnFailureListener { e ->
            Log.d("MainActivity", "Update check failed: ${e.message}")
        }
    }

    private fun requestInAppReview() {
        val reviewManager = ReviewManagerFactory.create(this)
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewManager.launchReviewFlow(this, task.result)
                    .addOnCompleteListener {
                        // Mark as rated whether user actually rated or dismissed.
                        // Google doesn't tell us the result for anti-spam reasons.
                        mainVm.markAsRated()
                    }
            } else {
                Log.d("MainActivity", "Review flow request failed: ${task.exception?.message}")
            }
        }
    }
}