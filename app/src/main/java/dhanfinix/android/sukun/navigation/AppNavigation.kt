package dhanfinix.android.sukun.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dhanfinix.android.sukun.MainViewModel
import dhanfinix.android.sukun.core.designsystem.components.DonationBottomSheet
import dhanfinix.android.sukun.feature.home.HomeScreen
import dhanfinix.android.sukun.feature.landing.LandingScreen
import dhanfinix.android.sukun.feature.onboarding.OnboardingScreen
import dhanfinix.android.sukun.feature.settings.AboutScreen
import dhanfinix.android.sukun.feature.settings.SettingsScreen
import dhanfinix.android.sukun.feature.splash.SplashScreen
import dhanfinix.android.sukun.feature.mosque.SilentZonesScreen
import dhanfinix.android.sukun.feature.webview.WebViewScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavigation(
    mainVm: MainViewModel,
    isOnboardingCompleted: Boolean,
    isReady: Boolean = true,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val hasSeenLanding by mainVm.hasSeenLanding.collectAsState()
    val shouldShowDonation by mainVm.shouldShowDonation.collectAsState()
    var showDonationSheet by remember { mutableStateOf(false) }

    LaunchedEffect(shouldShowDonation) {
        if (shouldShowDonation) {
            showDonationSheet = true
        }
    }

    if (showDonationSheet) {
        DonationBottomSheet(
            onDismissRequest = {
                showDonationSheet = false
                mainVm.markDonationAsShown()
            },
            onDonateKofi = {
                showDonationSheet = false
                mainVm.markDonationAsShown()
                navController.navigate(Route.WebView("https://ko-fi.com/dhandev", "Donate via Ko-fi"))
            },
            onDonateSaweria = {
                showDonationSheet = false
                mainVm.markDonationAsShown()
                navController.navigate(Route.WebView("https://saweria.co/dhandev", "Donate via Saweria"))
            }
        )
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Route.Splash,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition = { fadeOut(tween(300)) },
            popEnterTransition = { fadeIn(tween(350)) },
            popExitTransition = { fadeOut(tween(300)) }
        ) {
            composable<Route.Splash>(
                exitTransition = { fadeOut(tween(400)) }
            ) {
                SplashScreen(
                    isReady = isReady,
                    onSplashFinished = {
                        val destination = if (hasSeenLanding) Route.Home else Route.Landing
                        navController.navigate(destination) {
                            popUpTo(Route.Splash) { inclusive = true }
                        }
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<Route.Landing> {
                LandingScreen(
                    mainVm = mainVm,
                    onGetStarted = { navController.navigate(Route.Onboarding) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<Route.Onboarding>(
                enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) {
                OnboardingScreen(
                    showBackButton = true,
                    onBack = { navController.popBackStack() },
                    onComplete = {
                        mainVm.setHasSeenLanding(true)
                        mainVm.setOnboardingCompleted(true)
                        navController.navigate(Route.Home) {
                            popUpTo(0) { inclusive = true } // Clear backstack
                        }
                    }
                )
            }

            composable<Route.Home>(
                enterTransition = { slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350)) },
                exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) {
                HomeScreen(
                    mainVm = mainVm,
                    onShowOnboarding = { navController.navigate(Route.Onboarding) },
                    onOpenSettings = { navController.navigate(Route.Settings) },
                    onOpenLocationSilent = { navController.navigate(Route.LocationSilent) },
                    onShowDonation = { showDonationSheet = true }
                )
            }

            composable<Route.Settings>(
                enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) {
                SettingsScreen(
                    mainVm = mainVm,
                    onOpenAbout = { navController.navigate(Route.About) },
                    onOpenLocationSilent = { navController.navigate(Route.LocationSilent) },
                    onOpenWebView = { url, title -> navController.navigate(Route.WebView(url, title)) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.LocationSilent>(
                enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) {
                SilentZonesScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.About>(
                enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Route.WebView>(
                enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) { backStackEntry ->
                val webViewRoute = backStackEntry.toRoute<Route.WebView>()
                WebViewScreen(
                    url = webViewRoute.url,
                    title = webViewRoute.title,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
