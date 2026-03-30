package dhanfinix.android.sukun.feature.mosque

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.database.entity.SilentZone
import dhanfinix.android.sukun.feature.mosque.components.BackgroundLocationRationaleDialog
import dhanfinix.android.sukun.feature.mosque.components.CameraRequest
import dhanfinix.android.sukun.feature.mosque.components.OsmMapView
import dhanfinix.android.sukun.feature.mosque.components.SilentZoneEditDialog
import dhanfinix.android.sukun.feature.mosque.components.SilentZonesBottomOverlay
import dhanfinix.android.sukun.feature.mosque.components.SilentZonesSheetContent
import dhanfinix.android.sukun.feature.mosque.components.SilentZonesTopOverlay
import kotlinx.coroutines.delay

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

    var centerLat by remember(uiState.userLat != null) { mutableStateOf(uiState.userLat) }
    var centerLng by remember(uiState.userLng != null) { mutableStateOf(uiState.userLng) }
    var zoom by remember { mutableStateOf(18.0) }
    var editingZone by remember { mutableStateOf<SilentZone?>(null) }
    var focusedZoneId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var includeZones by remember { mutableStateOf(false) }
    var mapSuggestions by remember { mutableStateOf<List<MapSuggestion>>(emptyList()) }
    var searchPinLat by remember { mutableStateOf<Double?>(null) }
    var searchPinLng by remember { mutableStateOf<Double?>(null) }
    var searchPinLabel by remember { mutableStateOf<String?>(null) }
    var showBackgroundRationale by remember { mutableStateOf(false) }
    var cameraRequest by remember { mutableStateOf<CameraRequest?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var zoneToDelete by remember { mutableStateOf<SilentZone?>(null) }

    val zoneMatches = remember(uiState.zones, searchQuery, includeZones) {
        if (!includeZones || searchQuery.isBlank()) {
            emptyList()
        } else {
            uiState.zones.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    LaunchedEffect(searchQuery, searchExpanded) {
        if (!searchExpanded || searchQuery.isBlank()) {
            mapSuggestions = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        viewModel.searchLocationSuggestions(
            query = searchQuery,
            onResult = { mapSuggestions = it },
            onError = { mapSuggestions = emptyList() }
        )
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    val fineLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermissions()
        if (granted) viewModel.startLocationUpdates()
    }

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

    var hasCenteredOnce by remember { mutableStateOf(false) }
    var hasCenteredToFreshLocation by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userLat, uiState.userLng) {
        if (!hasCenteredOnce && uiState.userLat != null && uiState.userLng != null) {
            cameraRequest = CameraRequest(uiState.userLat!!, uiState.userLng!!, 18.0)
            hasCenteredOnce = true
        }
    }

    LaunchedEffect(uiState.userLat, uiState.userLng, uiState.isLocationFresh) {
        if (!hasCenteredToFreshLocation && uiState.isLocationFresh && uiState.userLat != null && uiState.userLng != null) {
            cameraRequest = CameraRequest(uiState.userLat!!, uiState.userLng!!, 18.0)
            hasCenteredToFreshLocation = true
        }
    }

    fun performMapSearch() {
        if (searchQuery.isBlank()) return
        viewModel.searchLocation(
            query = searchQuery,
            onResult = { lat, lng ->
                zoom = 16.0
                cameraRequest = CameraRequest(lat, lng, 16.0)
                focusedZoneId = -1L
                searchPinLat = lat
                searchPinLng = lng
                searchPinLabel = searchQuery
                searchExpanded = false
            },
            onError = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    val handleZoneMatchClick: (SilentZone) -> Unit = { zone ->
        focusedZoneId = zone.id
        zoom = 18.0
        cameraRequest = CameraRequest(zone.latitude, zone.longitude, 18.0)
        searchPinLat = null
        searchPinLng = null
        searchPinLabel = null
        searchExpanded = false
    }

    val handleMapSuggestionClick: (MapSuggestion) -> Unit = { suggestion ->
        zoom = 16.0
        cameraRequest = CameraRequest(suggestion.latitude, suggestion.longitude, 16.0)
        focusedZoneId = -1L
        searchPinLat = suggestion.latitude
        searchPinLng = suggestion.longitude
        searchPinLabel = suggestion.title
        searchExpanded = false
    }

    val handleSearchSubmit = {
        if (includeZones && zoneMatches.size == 1) {
            handleZoneMatchClick(zoneMatches.first())
        } else if (searchQuery.isNotBlank()) {
            performMapSearch()
        }
    }

    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    BottomSheetScaffold(
        scaffoldState = sheetState,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.location_silence_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(
                            if (searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        },
        sheetContent = {
            SilentZonesSheetContent(
                zones = uiState.zones,
                activeZoneId = uiState.activeZoneId,
                isLocationSilenceEnabled = uiState.isLocationSilenceEnabled,
                isAutoMosqueSilenceEnabled = uiState.isAutoMosqueSilenceEnabled,
                isAggressiveLocationEnabled = uiState.isAggressiveLocationEnabled,
                activeGeofenceCount = uiState.activeGeofenceCount,
                maxGeofenceSlots = uiState.maxGeofenceSlots,
                onFocusZone = { zone ->
                    focusedZoneId = zone.id
                    zoom = 18.0
                    cameraRequest = CameraRequest(zone.latitude, zone.longitude, 18.0)
                },
                onEditZone = { zone -> editingZone = zone },
                onDeleteZone = { zone ->
                    zoneToDelete = zone
                    showDeleteConfirmation = true
                },
                onToggleEnabled = { zone -> viewModel.toggleSilentZone(zone) },
                onToggleAutoSilent = { zone -> viewModel.toggleAutoSilent(zone) },
                onLocationSilenceToggled = { enabled -> viewModel.setLocationSilenceEnabled(enabled) },
                onAutoMosqueSilenceToggled = { enabled -> viewModel.setAutoMosqueSilenceEnabled(enabled) },
                onAggressiveLocationToggled = { enabled -> viewModel.setAggressiveLocationEnabled(enabled) },
                onRefreshMosques = { viewModel.refreshMosques() },
                onToggleMosquePin = { zone -> viewModel.toggleMosquePinned(zone) }
            )
        },
        sheetPeekHeight = 120.dp,
        sheetDragHandle = null,
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (uiState.userLat != null && uiState.userLng != null) {
                OsmMapView(
                    zones = uiState.zones,
                    userLat = uiState.userLat,
                    userLng = uiState.userLng,
                    isUserLocationFresh = uiState.isLocationFresh,
                    searchLat = searchPinLat,
                    searchLng = searchPinLng,
                    searchLabel = searchPinLabel,
                    centerLat = centerLat,
                    centerLng = centerLng,
                    zoom = zoom,
                    cameraRequest = cameraRequest,
                    focusedZoneId = focusedZoneId,
                    activeZoneId = uiState.activeZoneId,
                    onMapClick = {
                        focusedZoneId = null
                    },
                    onMapLongClick = { lat, lng ->
                        selectedLat = lat
                        selectedLng = lng
                        showAddDialog = true
                    },
                    onMarkerClick = { zone ->
                        focusedZoneId = zone.id
                    },
                    onCenterChanged = { lat, lng ->
                        centerLat = lat
                        centerLng = lng
                    },
                    onZoomChanged = {
                        zoom = it
                    },
                    onAddFromSearch = { label, lat, lng ->
                        selectedLat = lat
                        selectedLng = lng
                        searchPinLabel = label
                        editingZone = null
                        showAddDialog = true
                    }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.msg_requesting_location),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SilentZonesTopOverlay(
                modifier = Modifier.align(Alignment.TopCenter),
                searchExpanded = searchExpanded,
                searchQuery = searchQuery,
                includeZones = includeZones,
                zoneMatches = zoneMatches,
                mapSuggestions = mapSuggestions,
                hasZones = uiState.zones.isNotEmpty(),
                isBackgroundLocationGranted = uiState.isBackgroundLocationGranted,
                isDndAccessGranted = uiState.isDndAccessGranted,
                isLocating = uiState.isLoading,
                isLocationFresh = uiState.isLocationFresh,
                isLocationSilenceEnabled = uiState.isLocationSilenceEnabled,
                onSearchQueryChange = { searchQuery = it },
                onClearSearch = { searchQuery = "" },
                onToggleIncludeZones = { includeZones = it },
                onSearchSubmit = handleSearchSubmit,
                onZoneMatchClick = handleZoneMatchClick,
                onMapSuggestionClick = handleMapSuggestionClick,
                onSearchMapClick = { performMapSearch() },
                onBackgroundPermissionClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (uiState.isFineLocationGranted) {
                            showBackgroundRationale = true
                        } else {
                            fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                onDndPermissionClick = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            )

            // FABs float above the sheet peek area
            SilentZonesBottomOverlay(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 136.dp),
                isLocationFresh = uiState.isLocationFresh,
                isLocating = uiState.isLoading,
                onMyLocation = {
                    when {
                        !uiState.isFineLocationGranted -> {
                            Toast.makeText(context, R.string.err_location_permission_needed, Toast.LENGTH_SHORT).show()
                            fineLocationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                        uiState.userLat != null && uiState.userLng != null -> {
                            zoom = 18.0
                            cameraRequest = CameraRequest(uiState.userLat!!, uiState.userLng!!, 18.0)
                            viewModel.requestSingleLocationFix {
                                Toast.makeText(context, R.string.err_location_not_found_gps, Toast.LENGTH_SHORT).show()
                            }
                        }
                        else -> {
                            Toast.makeText(context, R.string.msg_requesting_location, Toast.LENGTH_SHORT).show()
                            viewModel.requestSingleLocationFix {
                                Toast.makeText(context, R.string.err_location_not_found_gps, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                onAddZone = {
                    selectedLat = uiState.userLat ?: centerLat ?: 0.0
                    selectedLng = uiState.userLng ?: centerLng ?: 0.0
                    showAddDialog = true
                }
            )
        }
    }

    if (showAddDialog || editingZone != null) {
        val zone = editingZone
        SilentZoneEditDialog(
            zone = zone,
            initialName = if (zone == null) searchPinLabel else null,
            onDismiss = {
                showAddDialog = false
                editingZone = null
            },
            onSave = { name, radius, isAutoSilent, silenceDuration ->
                if (zone == null) {
                    viewModel.addSilentZone(
                        name = name,
                        lat = selectedLat,
                        lng = selectedLng,
                        radius = radius,
                        isAutoSilent = isAutoSilent,
                        silenceDuration = silenceDuration
                    )
                } else {
                    viewModel.updateSilentZone(
                        zone.copy(
                            name = name,
                            radius = radius,
                            isAutoSilent = isAutoSilent,
                            silenceDuration = silenceDuration
                        )
                    )
                }
                showAddDialog = false
                editingZone = null
            }
        )
    }

    if (showBackgroundRationale) {
        BackgroundLocationRationaleDialog(
            onDismiss = { showBackgroundRationale = false },
            onConfirm = {
                showBackgroundRationale = false
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            },
            onOpenAppInfo = {
                showBackgroundRationale = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    }

    if (showDeleteConfirmation && zoneToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmation = false
                zoneToDelete = null
            },
            title = { Text(stringResource(R.string.delete_zone_confirmation_title)) },
            text = { Text(stringResource(R.string.delete_zone_confirmation_msg, zoneToDelete?.name ?: "")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        zoneToDelete?.let { viewModel.deleteSilentZone(it) }
                        showDeleteConfirmation = false
                        zoneToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    zoneToDelete = null
                }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
