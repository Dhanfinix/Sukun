package dhanfinix.android.sukun.feature.onboarding
import dhanfinix.android.sukun.core.reliability.ReliabilityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel()
) {
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val reliabilityManager = remember(context) { ReliabilityManager(context) }

    var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) }
    var hasDndPermission by remember { mutableStateOf(checkDndPermission(context)) }
    var hasLocationPermission by remember { mutableStateOf(checkLocationPermission(context)) }
    var hasExactAlarmPermission by remember { mutableStateOf(reliabilityManager.isExactAlarmPermissionGranted()) }
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(reliabilityManager.isIgnoringBatteryOptimizations()) }

    // Re-check permissions when returning from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Initial check
                hasNotificationPermission = checkNotificationPermission(context)
                hasDndPermission = checkDndPermission(context)
                hasLocationPermission = checkLocationPermission(context)
                hasExactAlarmPermission = reliabilityManager.isExactAlarmPermissionGranted()
                isIgnoringBatteryOptimizations = reliabilityManager.isIgnoringBatteryOptimizations()
                
                // Pulse re-check for 3 seconds (helps with slow system updates on some devices)
                scope.launch {
                    repeat(3) {
                        delay(1000)
                        hasNotificationPermission = checkNotificationPermission(context)
                        hasDndPermission = checkDndPermission(context)
                        hasLocationPermission = checkLocationPermission(context)
                        hasExactAlarmPermission = reliabilityManager.isExactAlarmPermissionGranted()
                        isIgnoringBatteryOptimizations = reliabilityManager.isIgnoringBatteryOptimizations()
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (onBack != null) {
                CenterAlignedTopAppBar(
                    title = { Text("Permissions", style = MaterialTheme.typography.titleMedium) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "App Reliability",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Configure permissions and settings to ensure Sukun works reliably in the background.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 1. Post Notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                    icon = Icons.Rounded.Notifications,
                    title = "Notifications",
                    description = "Required to show a countdown banner when silence is active.",
                    isGranted = hasNotificationPermission,
                    onRequest = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                )
            }

            // 2. Do Not Disturb Access
            PermissionCard(
                icon = Icons.Rounded.DoNotDisturbOn,
                title = "Do Not Disturb",
                description = "Required to instantly mute your phone when Adhan time arrives.",
                isGranted = hasDndPermission,
                onRequest = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            )

            // 3. Location Access
            PermissionCard(
                icon = Icons.Rounded.LocationOn,
                title = "Location",
                description = "Required to automatically fetch accurate prayer times for your city.",
                isGranted = hasLocationPermission,
                onRequest = {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
            )

            // 4. Exact Alarms
            PermissionCard(
                icon = Icons.Rounded.Timer,
                title = "Exact Alarms",
                description = "Required for precise silence timing on Android 12+.",
                isGranted = hasExactAlarmPermission,
                onRequest = { reliabilityManager.openExactAlarmSettings() }
            )

            // 5. Battery Optimization
            PermissionCard(
                icon = Icons.Rounded.BatteryChargingFull,
                title = "Battery Optimization",
                description = "Prevent the system from killing the app in the background.",
                isGranted = isIgnoringBatteryOptimizations,
                onRequest = { reliabilityManager.requestIgnoreBatteryOptimizations() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            val allGranted = hasNotificationPermission && hasDndPermission && 
                hasLocationPermission && hasExactAlarmPermission && isIgnoringBatteryOptimizations
            
            Button(
                onClick = { 
                    viewModel.markOnboardingCompleted()
                    onBack?.invoke() 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                AnimatedContent(
                    targetState = allGranted,
                    label = "start_button_text"
                ) { granted ->
                    Text(
                        text = if (granted) "Start Using Sukun" else "Skip For Now",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { if (!isGranted) onRequest() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            AnimatedContent(
                targetState = isGranted,
                label = "permission_status"
            ) { granted ->
                if (granted) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Granted",
                        tint = Color(0xFF4CAF50), // Green check
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "FIX",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun checkDndPermission(context: Context): Boolean {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    return notificationManager.isNotificationPolicyAccessGranted
}

private fun checkLocationPermission(context: Context): Boolean {
    val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return fine || coarse
}
