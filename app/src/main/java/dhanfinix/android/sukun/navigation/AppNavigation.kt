package dhanfinix.android.sukun.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
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
    // ── Navigation holds on splash until the splash ITSELF calls onSplashFinished ──
    // isReady merely unblocks the animation sequence inside SplashScreen;
    // we don't switch screens until the full animation has run.
    var splashFinished by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = when {
            !splashFinished -> Screen.Splash
            !isOnboardingCompleted -> Screen.Onboarding
            else -> Screen.Home
        },
        transitionSpec = {
            if (initialState == Screen.Splash && targetState != Screen.Splash) {
                fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(400))
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
                        onSplashFinished = { splashFinished = true }
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
                        onShowOnboarding = { mainVm.setOnboardingCompleted(false) }
                    )
                }
            }
        }
    }
}

private enum class Screen {
    Splash, Onboarding, Home
}
