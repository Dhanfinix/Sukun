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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.database.entity.SilentZone
import dhanfinix.android.sukun.feature.mosque.components.BackgroundLocationRationaleDialog
import dhanfinix.android.sukun.feature.mosque.components.OsmMapView
import dhanfinix.android.sukun.feature.mosque.components.SilentZoneEditDialog
import dhanfinix.android.sukun.feature.mosque.components.SilentZonesBottomOverlay
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

    var centerLat by remember(uiState.userLat) { mutableStateOf(uiState.userLat) }
    var centerLng by remember(uiState.userLng) { mutableStateOf(uiState.userLng) }
    var zoom by remember { mutableStateOf(18.0) }
    var isListExpanded by remember { mutableStateOf(false) }
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
    ) { viewModel.refreshPermissions() }

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

    fun performMapSearch() {
        if (searchQuery.isBlank()) return
        viewModel.searchLocation(
            query = searchQuery,
            onResult = { lat, lng ->
                centerLat = lat
                centerLng = lng
                zoom = 16.0
                focusedZoneId = -1L
                searchPinLat = lat
                searchPinLng = lng
                searchPinLabel = searchQuery
                isListExpanded = false
                searchExpanded = false
            },
            onError = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        )
    }

    val handleZoneMatchClick: (SilentZone) -> Unit = { zone ->
        focusedZoneId = zone.id
        centerLat = zone.latitude
        centerLng = zone.longitude
        zoom = 18.0
        searchPinLat = null
        searchPinLng = null
        searchPinLabel = null
        searchExpanded = false
    }

    val handleMapSuggestionClick: (MapSuggestion) -> Unit = { suggestion ->
        centerLat = suggestion.latitude
        centerLng = suggestion.longitude
        zoom = 16.0
        focusedZoneId = -1L
        searchPinLat = suggestion.latitude
        searchPinLng = suggestion.longitude
        searchPinLabel = suggestion.title
        isListExpanded = false
        searchExpanded = false
    }

    val handleSearchSubmit = {
        if (includeZones && zoneMatches.size == 1) {
            handleZoneMatchClick(zoneMatches.first())
        } else if (searchQuery.isNotBlank()) {
            performMapSearch()
        }
    }

    Scaffold(
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
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OsmMapView(
                zones = uiState.zones,
                userLat = uiState.userLat,
                userLng = uiState.userLng,
                searchLat = searchPinLat,
                searchLng = searchPinLng,
                searchLabel = searchPinLabel,
                centerLat = centerLat,
                centerLng = centerLng,
                zoom = zoom,
                focusedZoneId = focusedZoneId,
                onMapClick = {
                    isListExpanded = false
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

            SilentZonesBottomOverlay(
                modifier = Modifier.align(Alignment.BottomCenter),
                zones = uiState.zones,
                activeZoneId = uiState.activeZoneId,
                isListExpanded = isListExpanded,
                onToggleList = { isListExpanded = !isListExpanded },
                onMyLocation = {
                    uiState.userLat?.let {
                        centerLat = it
                        zoom = 18.0
                    }
                    uiState.userLng?.let { centerLng = it }
                },
                onAddZone = {
                    selectedLat = uiState.userLat ?: centerLat ?: 0.0
                    selectedLng = uiState.userLng ?: centerLng ?: 0.0
                    showAddDialog = true
                },
                onFocusZone = { zone ->
                    focusedZoneId = zone.id
                    centerLat = zone.latitude
                    centerLng = zone.longitude
                    zoom = 18.0
                },
                onEditZone = { zone -> editingZone = zone },
                onDeleteZone = { zone -> viewModel.deleteSilentZone(zone) },
                onToggleEnabled = { zone -> viewModel.toggleSilentZone(zone) },
                onToggleAutoSilent = { zone -> viewModel.toggleAutoSilent(zone) }
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
}
