package dhanfinix.android.sukun.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import androidx.navigation.toRoute
import dhanfinix.android.sukun.MainViewModel
import dhanfinix.android.sukun.core.designsystem.components.DonationBottomSheet
import dhanfinix.android.sukun.core.designsystem.util.launchSukunCustomTab
import dhanfinix.android.sukun.feature.home.HomeScreen
import dhanfinix.android.sukun.feature.landing.LandingScreen
import dhanfinix.android.sukun.feature.onboarding.OnboardingScreen
import dhanfinix.android.sukun.feature.settings.AboutScreen
import dhanfinix.android.sukun.feature.settings.SettingsScreen
import dhanfinix.android.sukun.feature.splash.SplashScreen
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
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary.toArgb()

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
                launchSukunCustomTab(context, "https://ko-fi.com/dhandev", primaryColor, onPrimaryColor)
            },
            onDonateSaweria = {
                showDonationSheet = false
                mainVm.markDonationAsShown()
                launchSukunCustomTab(context, "https://saweria.co/dhandev", primaryColor, onPrimaryColor)
            }
        )
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Splash,
            enterTransition = { fadeIn(tween(350)) },
            exitTransition = { fadeOut(tween(300)) },
            popEnterTransition = { fadeIn(tween(350)) },
            popExitTransition = { fadeOut(tween(300)) }
        ) {
            composable<Splash>(
                exitTransition = { fadeOut(tween(400)) }
            ) {
                SplashScreen(
                    isReady = isReady,
                    onSplashFinished = {
                        val destination: Route = if (hasSeenLanding) Home else Landing
                        navController.navigate(destination) {
                            popUpTo(Splash) { inclusive = true }
                        }
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<Landing> {
                LandingScreen(
                    mainVm = mainVm,
                    onGetStarted = { navController.navigate(Onboarding) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                )
            }

            composable<Onboarding>(
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
                        navController.navigate(Home) {
                            popUpTo(0) { inclusive = true } // Clear backstack
                        }
                    }
                )
            }

            composable<Home>(
                enterTransition = { slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350)) },
                exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) {
                HomeScreen(
                    mainVm = mainVm,
                    onShowOnboarding = { navController.navigate(Onboarding) },
                    onOpenSettings = { navController.navigate(Settings) },
                    onShowDonation = { showDonationSheet = true }
                )
            }

            composable<Settings>(
                enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) {
                SettingsScreen(
                    mainVm = mainVm,
                    onOpenAbout = { navController.navigate(About) },
                    onOpenWebView = { url, title -> navController.navigate(WebPageRoute(pageUrl = url, pageTitle = title)) },
                    onDonate = { showDonationSheet = true },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<About>(
                enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<WebPageRoute>(
                enterTransition = { slideInHorizontally(tween(350)) { it } + fadeIn(tween(350)) },
                exitTransition = { slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300)) },
                popEnterTransition = { slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350)) },
                popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300)) }
            ) { backStackEntry ->
                val webPageRoute: WebPageRoute = backStackEntry.toRoute()
                WebViewScreen(
                    url = webPageRoute.pageUrl,
                    title = webPageRoute.pageTitle,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
