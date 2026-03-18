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
import java.util.Locale
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
            val shouldShowDonation by mainVm.shouldShowDonation.collectAsState()

            var showDonationSheet by remember { mutableStateOf(false) }

            LaunchedEffect(shouldShowDonation) {
                if (shouldShowDonation) {
                    showDonationSheet = true
                }
            }

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
            
            SukunTheme(
                appTheme = appTheme,
                dynamicColor = useDynamicColor
            ) {
                AppNavigation(
                    mainVm = mainVm,
                    isOnboardingCompleted = isOnboardingCompleted,
                    isReady = isReady
                )

                if (showDonationSheet) {
                    DonationBottomSheet(
                        onDismiss = {
                            showDonationSheet = false
                            mainVm.markDonationAsShown()
                        },
                        onDonateKofi = {
                            val url = "https://ko-fi.com/dhandev"
                            val intent = CustomTabsIntent.Builder().build()
                            intent.launchUrl(this@MainActivity, Uri.parse(url))
                            showDonationSheet = false
                            mainVm.markDonationAsShown()
                        },
                        onDonateSaweria = {
                            val url = "https://saweria.co/dhandev"
                            val intent = CustomTabsIntent.Builder().build()
                            intent.launchUrl(this@MainActivity, Uri.parse(url))
                            showDonationSheet = false
                            mainVm.markDonationAsShown()
                        }
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DonationBottomSheet(
        onDismiss: () -> Unit,
        onDonateKofi: () -> Unit,
        onDonateSaweria: () -> Unit
    ) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.donation_sheet_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.donation_sheet_persuasive_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onDonateKofi,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Ko-fi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "(${stringResource(R.string.label_global)})",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Button(
                        onClick = onDonateSaweria,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large,
                        contentPadding = PaddingValues(vertical = 16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Saweria",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "(${stringResource(R.string.label_indonesia)})",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.btn_maybe_later),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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