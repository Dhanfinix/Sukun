package dhanfinix.android.sukun.navigation

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import dhanfinix.android.sukun.MainViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dhanfinix.android.sukun.feature.onboarding.OnboardingScreen
import dhanfinix.android.sukun.feature.home.HomeScreen
import dhanfinix.android.sukun.feature.splash.SplashScreen

/**
 * Single-screen layout that combines Volume Dashboard and Prayer Settings
 * into one scrollable page. No bottom navigation needed.
 */
@Composable
fun AppNavigation(
    mainVm: MainViewModel,
    isOnboardingCompleted: Boolean,
    isReady: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showOnboardingOverride by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        
        // Background content (Main app) - only visible after splash finishes
        AnimatedVisibility(
            visible = !showSplash,
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main content is always present if onboarding was once completed
                if (isOnboardingCompleted) {
                    HomeScreen(
                        mainVm = mainVm,
                        onShowOnboarding = { showOnboardingOverride = true }
                    )
                }

                if (isReady) {
                    AnimatedVisibility(
                        visible = !isOnboardingCompleted || showOnboardingOverride,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        OnboardingScreen(
                            onBack = if (isOnboardingCompleted) {
                                { showOnboardingOverride = false }
                            } else null
                        )
                    }
                }
            }
        }

        // Custom Animated Splash Screen Overlay
        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { -it }, 
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            SplashScreen(
                isReady = isReady,
                onSplashFinished = { showSplash = false }
            )
        }
    }
}


