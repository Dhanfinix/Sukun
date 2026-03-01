package dhanfinix.android.sukun.feature.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mainVm: MainViewModel,
    onShowOnboarding: () -> Unit,
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
    var showAboutDialog by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }

    val appTheme by mainVm.appTheme.collectAsState()
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
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = "سكون  Sukun",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { showThemeSheet = true }) {
                            Icon(Icons.Rounded.Palette, contentDescription = "Change Theme")
                        }
                        IconButton(onClick = { showAboutDialog = true }) {
                            Icon(Icons.Rounded.Info, contentDescription = "About Sukun")
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
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeOff, contentDescription = "Manual Silence")
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PrayerSection(
                    state = prayerState,
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
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Text(
                                text = "Enable permissions to ensure all features work correctly",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tap to re-configure location, DND, and notifications",
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

                if (showAboutDialog) {
                    AboutDialog(
                        onDismiss = { showAboutDialog = false },
                        onShowTutorial = { mainVm.setCoachmarkShown(false) }
                    )
                }
                
                if (showThemeSheet) {
                    ThemeSelectionSheet(
                        currentTheme = appTheme,
                        onThemeSelect = { theme ->
                            mainVm.setTheme(theme)
                            showThemeSheet = false
                        },
                        onDismiss = { showThemeSheet = false }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        if (!hasSeenCoachmark) {
            CoachMarkOverlay(
                targets = coachMarkTargets,
                onStepChange = { step ->
                    val rect = coachMarkTargets[step.target]
                    if (rect != null) {
                        val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
                        // If target is offscreen at bottom (give some padding)
                        if (rect.bottom > screenHeightPx - 100f) {
                            val delta = rect.bottom - screenHeightPx + 300f 
                            coroutineScope.launch { scrollState.animateScrollBy(delta) }
                        } else if (rect.top < 200f) {
                            // If target is offscreen at top (account for app bar)
                            val delta = rect.top - 200f
                            coroutineScope.launch { scrollState.animateScrollBy(delta) }
                        }
                    }
                },
                onDismiss = { mainVm.setCoachmarkShown(true) }
            )
        }
    }
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onShowTutorial: (() -> Unit)? = null
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            if (onShowTutorial != null) {
                androidx.compose.material3.TextButton(onClick = {
                    onShowTutorial()
                    onDismiss()
                }) {
                    Text("Show Tutorial")
                }
            }
        },
        icon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("About Sukun", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Sukun is your companion for focused prayer. It helps you maintain tranquility by automatically managing your device's volume during prayer times.",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text("Key Features", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "• Automatic prayer time updates based on location.\n" +
                           "• Precise background silencing using exact alarms.\n" +
                           "• Manual silence mode for extra-long focus sessions.\n" +
                           "• Centralized volume dashboard (Media & Ringer).",
                    style = MaterialTheme.typography.bodySmall
                )

                Text("How it Works", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Sukun schedules exact alarms for each prayer time. When the time arrives, it records your current volume levels, enters Do Not Disturb mode, and mutes your phone for the selected duration. Once finished, it restores everything exactly as it was.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionSheet(
    currentTheme: AppTheme,
    onThemeSelect: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "App Theme",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
            )

            val options = listOf(
                AppTheme.SYSTEM to "System Default",
                AppTheme.LIGHT to "Light Mode",
                AppTheme.DARK to "Dark Mode"
            )

            options.forEach { (value, label) ->
                val isSelected = value == currentTheme
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onThemeSelect(value) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
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
