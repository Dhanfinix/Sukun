package dhanfinix.android.sukun.feature.home

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import dhanfinix.android.sukun.core.datastore.AppTheme
import dhanfinix.android.sukun.MainViewModel
import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.animateScrollBy
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import dhanfinix.android.sukun.R
import androidx.lifecycle.viewmodel.compose.viewModel
import dhanfinix.android.sukun.feature.prayer.ui.PrayerEvent
import dhanfinix.android.sukun.feature.prayer.ui.PrayerSection
import dhanfinix.android.sukun.feature.prayer.ui.PrayerViewModel
import dhanfinix.android.sukun.feature.home.components.CoachMarkOverlay
import dhanfinix.android.sukun.feature.home.components.CoachMarkTarget
import dhanfinix.android.sukun.feature.volume.VolumeEvent
import dhanfinix.android.sukun.feature.volume.VolumeSection
import dhanfinix.android.sukun.feature.volume.VolumeViewModel
import dhanfinix.android.sukun.core.reliability.ReliabilityManager
import dhanfinix.android.sukun.core.designsystem.components.TopSnackbar
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mainVm: MainViewModel,
    onShowOnboarding: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val volumeVm: VolumeViewModel = viewModel()
    val volumeState by volumeVm.uiState.collectAsState()

    val prayerVm: PrayerViewModel = viewModel()
    val prayerState by prayerVm.uiState.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasAllPermissions by remember { mutableStateOf(checkAllPermissions(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAllPermissions = checkAllPermissions(context)
                volumeVm.onEvent(VolumeEvent.OnResume)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var showManualSilenceSheet by remember { mutableStateOf(false) }

    val appTheme by mainVm.appTheme.collectAsState()
    val useDynamicColor by mainVm.useDynamicColor.collectAsState()
    val hasSeenCoachmark by mainVm.hasSeenHomeCoachmark.collectAsState()
    val coachMarkTargets = remember { mutableStateMapOf<CoachMarkTarget, Rect>() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val config = LocalConfiguration.current

    PullToRefreshBox(
        isRefreshing = prayerState.isLoading,
        onRefresh = { prayerVm.onEvent(PrayerEvent.RefreshTimes) },
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.content_desc_settings))
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = true, // Force always visible for now, or link to state
                    enter = scaleIn(),
                    exit = scaleOut()
                ) {
                    FloatingActionButton(
                        onClick = { showManualSilenceSheet = true },
                        modifier = Modifier.onGloballyPositioned { coordinates ->
                            coachMarkTargets[CoachMarkTarget.MANUAL_SILENCE_FAB] = coordinates.boundsInWindow()
                        }.padding(
                            bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
                                .asPaddingValues()
                                .calculateBottomPadding()
                        ),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeOff, contentDescription = stringResource(R.string.manual_silence))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(
                        bottom = androidx.compose.foundation.layout.WindowInsets.navigationBars
                            .asPaddingValues()
                            .calculateBottomPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrayerSection(
                    state = prayerState,
                    isSukunActive = volumeState.isSukunActive,
                    sukunEndTime = volumeState.sukunEndTime,
                    sukunLabel = volumeState.sukunLabel,
                    onStopSilence = { volumeVm.onEvent(VolumeEvent.StopSilence) },
                    onEvent = prayerVm::onEvent,
                    onTargetPositioned = { target, rect -> coachMarkTargets[target] = rect }
                )

                // ── Permission Banner ──
                AnimatedVisibility(
                    visible = !hasAllPermissions,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Button(
                        onClick = onShowOnboarding,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.enable_permissions_banner_title),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.enable_permissions_banner_desc),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // ── Divider ──
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                VolumeSection(
                    state = volumeState,
                    onEvent = volumeVm::onEvent,
                    showManualSilenceSheet = showManualSilenceSheet,
                    onManualSilenceDismiss = { showManualSilenceSheet = false },
                    onTargetPositioned = { target, rect -> coachMarkTargets[target] = rect }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (!hasSeenCoachmark) {
            CoachMarkOverlay(
                targets = coachMarkTargets,
                onStepChange = { step ->
                    if (step.target == CoachMarkTarget.MANUAL_SILENCE_FAB) return@CoachMarkOverlay
                    
                    val rect = coachMarkTargets[step.target]
                    if (rect != null) {
                        val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
                        // If target is offscreen at bottom (give some padding)
                        if (rect.bottom > screenHeightPx - 100f) {
                            val delta = rect.bottom - screenHeightPx + 300f 
                            coroutineScope.launch { 
                                delay(300)
                                scrollState.animateScrollBy(delta) 
                            }
                        } else if (rect.top < 200f) {
                            // If target is offscreen at top (account for app bar)
                            val delta = rect.top - 200f
                            coroutineScope.launch { 
                                delay(300)
                                scrollState.animateScrollBy(delta) 
                            }
                        }
                    }
                },
                onDismiss = { mainVm.setCoachmarkShown(true) }
            )
        }

        TopSnackbar(
            message = prayerState.snackbarMessage ?: volumeState.snackbarMessage,
            onDismiss = { 
                if (prayerState.snackbarMessage != null) {
                    prayerVm.onEvent(PrayerEvent.SnackbarMessageConsumed)
                }
                if (volumeState.snackbarMessage != null) {
                    volumeVm.onEvent(VolumeEvent.SnackbarMessageConsumed)
                }
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private fun checkAllPermissions(context: Context): Boolean {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val hasDnd = nm.isNotificationPolicyAccessGranted

    val hasLocation = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else true

    return hasDnd && hasLocation && hasNotif &&
            ReliabilityManager(context).isExactAlarmPermissionGranted() &&
            ReliabilityManager(context).isIgnoringBatteryOptimizations()
}
