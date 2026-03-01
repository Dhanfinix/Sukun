package dhanfinix.android.sukun.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dhanfinix.android.sukun.MainViewModel
import dhanfinix.android.sukun.feature.home.HomeScreen
import dhanfinix.android.sukun.feature.onboarding.OnboardingScreen
import dhanfinix.android.sukun.feature.splash.SplashScreen

/**
 * Main application navigation container.
 */
@Composable
fun AppNavigation(
    mainVm: MainViewModel,
    isOnboardingCompleted: Boolean,
    isReady: Boolean = true,
    modifier: Modifier = Modifier
) {
    // Determine which screen to show based on state
    AnimatedContent(
        targetState = when {
            !isReady -> Screen.Splash
            !isOnboardingCompleted -> Screen.Onboarding
            else -> Screen.Home
        },
        transitionSpec = {
            // Smooth crossfade between major screens
            if (initialState == Screen.Splash && targetState != Screen.Splash) {
                // Slower fade out for splash screen
                fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
            } else {
                fadeIn() togetherWith fadeOut()
            }
        },
        label = "screen_transition",
        modifier = modifier
    ) { screen ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                Screen.Splash -> {
                    SplashScreen(
                        isReady = isReady,
                        onSplashFinished = { /* State changes will trigger navigation automatically due to AnimatedContent */ }
                    )
                }
                Screen.Onboarding -> {
                    OnboardingScreen(
                        onBack = { mainVm.setOnboardingCompleted(true) }
                    )
                }
                Screen.Home -> {
                    HomeScreen(
                        mainVm = mainVm,
                        onShowOnboarding = { mainVm.setOnboardingCompleted(false) } // For re-configuring permissions
                    )
                }
            }
        }
    }
}

private enum class Screen {
    Splash, Onboarding, Home
}
