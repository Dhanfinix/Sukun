package dhanfinix.android.sukun.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import dhanfinix.android.sukun.MainViewModel
import dhanfinix.android.sukun.feature.home.HomeScreen
import dhanfinix.android.sukun.feature.settings.SettingsScreen
import dhanfinix.android.sukun.feature.settings.AboutScreen
import dhanfinix.android.sukun.feature.landing.LandingScreen
import dhanfinix.android.sukun.feature.onboarding.OnboardingScreen
import dhanfinix.android.sukun.feature.splash.SplashScreen

/**
 * Navigation flows:
 *
 * Fresh install (hasSeenLanding = false):
 *   Splash → Landing [shared icon] → Permission ← Landing → Home
 *
 * Returning user (hasSeenLanding = true):
 *   Splash → Home → (from HomeScreen) Permission (with back)
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    mainVm: MainViewModel,
    isOnboardingCompleted: Boolean,
    isReady: Boolean = true,
    modifier: Modifier = Modifier
) {
    val hasSeenLanding by mainVm.hasSeenLanding.collectAsState()
    var splashFinished by remember { mutableStateOf(false) }
    var showPermission by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val screen = when {
        !splashFinished                          -> Screen.Splash
        !hasSeenLanding && !showPermission       -> Screen.Landing
        showPermission                           -> Screen.Onboarding
        showAbout                                -> Screen.About
        showSettings                             -> Screen.Settings
        else                                     -> Screen.Home
    }

    SharedTransitionLayout(modifier = modifier) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                when {
                    initialState == Screen.Splash ->
                        fadeIn(tween(600)) togetherWith fadeOut(tween(400))
                    (initialState == Screen.Landing && targetState == Screen.Onboarding) ||
                    (initialState == Screen.Onboarding && targetState == Screen.Home && !hasSeenLanding) ||
                    (initialState == Screen.Home && targetState == Screen.Onboarding) ||
                    (initialState == Screen.Home && targetState == Screen.Settings) ||
                    (initialState == Screen.Settings && targetState == Screen.About) ->
                        (slideInHorizontally(tween(350)) { it } + fadeIn(tween(350))) togetherWith
                                (slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)))
                    
                    (initialState == Screen.Onboarding && targetState == Screen.Landing) ||
                    (initialState == Screen.Onboarding && targetState == Screen.Home && hasSeenLanding) ||
                    (initialState == Screen.Settings && targetState == Screen.Home) ||
                    (initialState == Screen.About && targetState == Screen.Settings) ->
                        (slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350))) togetherWith
                                (slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)))
                    else -> fadeIn() togetherWith fadeOut()
                }
            },
            label = "screen_transition"
        ) { currentScreen ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                    Screen.Splash -> {
                        SplashScreen(
                            isReady = isReady,
                            onSplashFinished = { splashFinished = true },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent
                        )
                    }
                    Screen.Landing -> {
                        LandingScreen(
                            mainVm = mainVm,
                            onGetStarted = { showPermission = true },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent
                        )
                    }
                    Screen.Onboarding -> {
                        OnboardingScreen(
                            showBackButton = true,
                            onBack = { showPermission = false },
                            onComplete = {
                                mainVm.setHasSeenLanding(true)
                                mainVm.setOnboardingCompleted(true)
                                showPermission = false
                            }
                        )
                    }
                    Screen.Home -> {
                        HomeScreen(
                            mainVm = mainVm,
                            onShowOnboarding = { showPermission = true },
                            onOpenSettings = { showSettings = true }
                        )
                    }
                    Screen.Settings -> {
                        SettingsScreen(
                            mainVm = mainVm,
                            onOpenAbout = { showAbout = true },
                            onBack = { showSettings = false }
                        )
                    }
                    Screen.About -> {
                        AboutScreen(
                            onBack = { showAbout = false }
                        )
                    }
                }
            }
        }
    }
}

private enum class Screen {
    Splash, Landing, Onboarding, Home, Settings, About
}
