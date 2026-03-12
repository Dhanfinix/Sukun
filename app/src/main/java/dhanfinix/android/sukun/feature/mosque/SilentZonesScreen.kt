package dhanfinix.android.sukun.feature.mosque

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight as FontWeightCompose
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dhanfinix.android.sukun.feature.mosque.components.OsmMapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SilentZonesScreen(
    onBack: () -> Unit,
    viewModel: SilentZonesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedLat by remember { mutableStateOf(0.0) }
    var selectedLng by remember { mutableStateOf(0.0) }
    
    var centerLat by remember { mutableStateOf<Double?>(null) }
    var centerLng by remember { mutableStateOf<Double?>(null) }
    var zoom by remember { mutableStateOf(18.0) }
    var isListExpanded by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<dhanfinix.android.sukun.core.database.entity.SilentZone?>(null) }

    // Permission launchers
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        viewModel.refreshPermissions()
    }

    // Lifecycle observer to refresh permissions when returning from settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Initial center
    var hasCenteredToFreshLocation by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.userLat, uiState.userLng, uiState.isLocationFresh) {
        if (!hasCenteredToFreshLocation && uiState.isLocationFresh && uiState.userLat != null && uiState.userLng != null) {
            centerLat = uiState.userLat
            centerLng = uiState.userLng
            hasCenteredToFreshLocation = true
        } else if (centerLat == null && uiState.userLat != null) {
            centerLat = uiState.userLat
            centerLng = uiState.userLng
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location Silent Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Full Screen Map
            OsmMapView(
                zones = uiState.zones,
                userLat = uiState.userLat,
                userLng = uiState.userLng,
                centerLat = centerLat,
                centerLng = centerLng,
                zoom = zoom,
                onMapLongClick = { lat, lng ->
                    selectedLat = lat
                    selectedLng = lng
                    showAddDialog = true
                },
                onCenterChanged = { lat, lng ->
                    centerLat = lat
                    centerLng = lng
                },
                onZoomChanged = { 
                    zoom = it
                }
            )

            // Top Overlay Column (Instructions + Permissions)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 4.dp
                ) {
                    Text(
                        "Long-press on map to drop a silent pin",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Floating Permission Banners
                AnimatedVisibility(
                    visible = !uiState.isBackgroundLocationGranted || !uiState.isDndAccessGranted,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Setup required for triggers",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            
                            if (!uiState.isBackgroundLocationGranted) {
                                PermissionWarningItem(
                                    text = "Background Location (Select 'Allow all the time')",
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", context.packageName, null)
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                        }
                                    }
                                )
                            }
                            
                            if (!uiState.isDndAccessGranted) {
                                PermissionWarningItem(
                                    text = "Do Not Disturb access",
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Overlay (FABs + Floating List)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // FABs shifted correctly
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    SmallFloatingActionButton(
                        onClick = {
                            uiState.userLat?.let { 
                                centerLat = it 
                                zoom = 18.0
                            }
                            uiState.userLng?.let { centerLng = it }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "My Location")
                    }
                    Spacer(Modifier.height(16.dp))
                    FloatingActionButton(
                        onClick = {
                            // Use live GPS location as priority, fall back to map center if GPS not ready
                            selectedLat = uiState.userLat ?: centerLat ?: 0.0
                            selectedLng = uiState.userLng ?: centerLng ?: 0.0
                            showAddDialog = true
                        },
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Zone")
                    }
                }

                // Floating List Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isListExpanded = !isListExpanded }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Saved Zones (${uiState.zones.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeightCompose.SemiBold
                                )
                            }
                            Icon(
                                imageVector = if (isListExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (isListExpanded) "Collapse" else "Expand"
                            )
                        }

                        // Content
                        if (isListExpanded) {
                            if (uiState.zones.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No silent zones added yet", color = MaterialTheme.colorScheme.secondary)
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxHeight(0.6f) // Don't cover whole map when expanded
                                ) {
                                    items(uiState.zones) { zone ->
                                        val isActive = uiState.activeZoneId == zone.id
                                        SilentZoneCard(
                                            zone = zone,
                                            isActive = isActive,
                                            onToggleEnabled = { viewModel.toggleSilentZone(zone) },
                                            onToggleAutoSilent = { viewModel.toggleAutoSilent(zone) },
                                            onEdit = { editingZone = zone },
                                            onDelete = { viewModel.deleteSilentZone(zone) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingZone != null) {
        val zone = editingZone
        var name by remember(showAddDialog, editingZone) { mutableStateOf(zone?.name ?: "") }
        var radius by remember(showAddDialog, editingZone) { mutableStateOf(zone?.radius?.toInt()?.toString() ?: "20") }
        var isAutoSilent by remember(showAddDialog, editingZone) { mutableStateOf(zone?.isAutoSilent ?: true) }
        var silenceDuration by remember(showAddDialog, editingZone) { mutableStateOf(zone?.silenceDuration ?: 30) }
        
        var showDurationDropdown by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false
                editingZone = null
            },
            shape = RoundedCornerShape(32.dp),
            icon = {
                Box(Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Rounded.LocationOn,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp).align(Alignment.Center)
                    )

                    IconButton(
                        onClick = {
                            showAddDialog = false
                            editingZone = null
                        },
                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-12).dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
           },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (zone == null) "New Silent Zone" else "Edit Silent Zone",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeightCompose.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // --- General Info Section ---
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Zone Name") },
                            placeholder = { Text("e.g. My Mosque") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Rounded.Label, null) },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )
                        
                        // Radius Selection with Slider
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.GpsFixed, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text("Radius: ${radius}m", style = MaterialTheme.typography.labelLarge)
                            }
                            Slider(
                                value = (radius.toFloatOrNull() ?: 20f).coerceIn(10f, 100f),
                                onValueChange = { radius = it.toInt().toString() },
                                valueRange = 10f..100f,
                                steps = 17, // ~5m steps from 10 to 100
                                modifier = Modifier.fillMaxWidth().height(32.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // --- Silence Logic Section ---
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { isAutoSilent = !isAutoSilent }
                        ) {
                            Icon(
                                if (isAutoSilent) Icons.Rounded.AutoMode else Icons.Rounded.NotificationsActive, 
                                null, 
                                modifier = Modifier.size(20.dp),
                                tint = if (isAutoSilent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-Silent", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeightCompose.SemiBold)
                                Text(
                                    if (isAutoSilent) "Starts automatically" else "Notification only",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Switch(
                                checked = isAutoSilent, 
                                onCheckedChange = { isAutoSilent = it },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        // Duration Picker
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text("Silence Duration", style = MaterialTheme.typography.labelLarge)
                            }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedCard(
                                    onClick = { showDurationDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            when {
                                                silenceDuration == 15 -> "15 Minutes"
                                                silenceDuration == 30 -> "30 Minutes"
                                                (silenceDuration ?: 30) % 60 == 0 -> "${(silenceDuration ?: 30) / 60} Hour${if ((silenceDuration ?: 30) / 60 > 1) "s" else ""}"
                                            else -> "$silenceDuration Minutes"
                                            },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Icon(Icons.Rounded.ArrowDropDown, null)
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = showDurationDropdown,
                                    onDismissRequest = { showDurationDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.6f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    listOf(15, 30, 60, 120, 180, 240, 360, 480, 720).forEach { dur ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(when {
                                                    dur == 15 -> "15 Minutes"
                                                    dur == 30 -> "30 Minutes"
                                                    dur % 60 == 0 -> "${dur / 60} Hour${if (dur / 60 > 1) "s" else ""}"
                                                else -> "$dur Minutes"
                                                })
                                            },
                                            onClick = {
                                                silenceDuration = dur
                                                showDurationDropdown = false
                                            },
                                            leadingIcon = { 
                                                Icon(
                                                    if (silenceDuration == dur) Icons.Rounded.CheckCircle else Icons.Rounded.Circle,
                                                    null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = if (silenceDuration == dur) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom tiny info
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.Info, 
                            null, 
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Silence stops when you leave the area.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (zone == null) {
                            viewModel.addSilentZone(
                                name = name, 
                                lat = selectedLat, 
                                lng = selectedLng, 
                                radius = radius.toFloatOrNull() ?: 20f,
                                isAutoSilent = isAutoSilent,
                                silenceDuration = silenceDuration
                            )
                        } else {
                            viewModel.updateSilentZone(
                                zone.copy(
                                    name = name,
                                    radius = radius.toFloatOrNull() ?: 20f,
                                    isAutoSilent = isAutoSilent,
                                    silenceDuration = silenceDuration
                                )
                            )
                        }
                        showAddDialog = false
                        editingZone = null
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Zone")
                }
            }
        )
    }
}

@Composable
private fun SilentZoneCard(
    zone: dhanfinix.android.sukun.core.database.entity.SilentZone,
    isActive: Boolean,
    onToggleEnabled: () -> Unit,
    onToggleAutoSilent: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            zone.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeightCompose.Bold
                        )
                        if (isActive) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "ACTIVE",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Text(
                        "${zone.radius.toInt()}m radius",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Switch(
                    checked = zone.isEnabled,
                    onCheckedChange = { onToggleEnabled() }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.5f))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Auto-Silent",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (zone.isAutoSilent) "Enabled" else "Notification Only",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        "Ends in ${if (zone.silenceDuration != null && zone.silenceDuration!! >= 60) "${zone.silenceDuration!! / 60}h" else "${zone.silenceDuration ?: 30}m"} (or on Exit)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeightCompose.Bold
                    )
                }
                
                Switch(
                    checked = zone.isAutoSilent,
                    onCheckedChange = { onToggleAutoSilent() },
                    thumbContent = {
                        if (zone.isAutoSilent) {
                            Icon(Icons.Rounded.SmartButton, null, Modifier.size(16.dp))
                        } else {
                            Icon(Icons.Rounded.NotificationsActive, null, Modifier.size(16.dp))
                        }
                    }
                )
                
                Spacer(Modifier.width(8.dp))
                
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionWarningItem(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
