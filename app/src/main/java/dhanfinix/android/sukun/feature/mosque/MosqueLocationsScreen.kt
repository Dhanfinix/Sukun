package dhanfinix.android.sukun.feature.mosque

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dhanfinix.android.sukun.core.database.entity.MosqueLocation
import dhanfinix.android.sukun.core.designsystem.components.TopSnackbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosqueLocationsScreen(
    onBack: () -> Unit,
    viewModel: MosqueLocationsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    // Snackbar message (combines success + error into one)  
    val snackbarMessage = uiState.successMessage ?: uiState.errorMessage

    var showAddDialog by remember { mutableStateOf(false) }
    var showListSheet by remember { mutableStateOf(false) }
    var selectedMosque by remember { mutableStateOf<MosqueLocation?>(null) }
    
    // State to center map on a specific location
    var centerLat by remember { mutableStateOf<Double?>(null) }
    var centerLng by remember { mutableStateOf<Double?>(null) }
    
    // Actual GPS user location received from the map
    var userLat by remember { mutableStateOf<Double?>(null) }
    var userLng by remember { mutableStateOf<Double?>(null) }
    
    // Coordinate to pre-fill the add dialog
    var longPressLat by remember { mutableStateOf<Double?>(null) }
    var longPressLng by remember { mutableStateOf<Double?>(null) }

    // Initial center on user cached location
    var initialCenterSet by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.userLat, uiState.userLng) {
        if (!initialCenterSet && uiState.userLat != null && uiState.userLng != null) {
            centerLat = uiState.userLat
            centerLng = uiState.userLng
            initialCenterSet = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 4.dp,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            "Mosque Locations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                },
                navigationIcon = {
                    FilledIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        },
        // floatingActionButton and snackbarHost moved inside the Box content for better control
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Background Map ────────────────────────────────────────────
            dhanfinix.android.sukun.feature.mosque.components.OsmMapView(
                locations = uiState.mosques,
                userLat = uiState.userLat,
                userLng = uiState.userLng,
                centerLat = centerLat,
                centerLng = centerLng,
                onMarkerClick = { mosque ->
                    selectedMosque = mosque
                    centerLat = mosque.latitude
                    centerLng = mosque.longitude
                },
                onMapClick = {
                    selectedMosque = null
                },
                onMapLongClick = { lat, lng ->
                    longPressLat = lat
                    longPressLng = lng
                    showAddDialog = true
                },
                onCenterChanged = { lat, lng ->
                    // Only accept the new center if it is meaningfully different 
                    // from the current center to prevent fractional loops
                    val lat1 = centerLat
                    val lng1 = centerLng
                    if (lat1 != null && lng1 != null) {
                        val distance = FloatArray(1)
                        android.location.Location.distanceBetween(
                            lat1, lng1,
                            lat, lng,
                            distance
                        )
                        if (distance[0] > 10f) {
                            centerLat = lat
                            centerLng = lng
                        }
                    } else {
                        centerLat = lat
                        centerLng = lng
                    }
                },
                onMyLocationUpdate = { lat, lng ->
                    userLat = lat
                    userLng = lng
                }
            )

            // ── Overlay Controls ──────────────────────────────────────────

            // TopSnackbar at the top
            TopSnackbar(
                message = snackbarMessage,
                onDismiss = { viewModel.clearMessage() },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // FAB Column at the bottom right
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = if (selectedMosque != null) 200.dp else 32.dp),
                horizontalAlignment = Alignment.End
            ) {
                // My Location Button
                SmallFloatingActionButton(
                    onClick = { 
                        (userLat ?: uiState.userLat)?.let { centerLat = it }
                        (userLng ?: uiState.userLng)?.let { centerLng = it }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = "My Location")
                }
                Spacer(Modifier.height(12.dp))
                // Refresh Button
                SmallFloatingActionButton(
                    onClick = { 
                        val clat = centerLat
                        val clng = centerLng
                        if (clat != null && clng != null) {
                            viewModel.fetchGlobalMosques(clat, clng)
                        } else {
                            viewModel.fetchGlobalMosques() 
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Add Button
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add custom mosque")
                }
            }
            
            // Bottom Left: List Toggle
            Surface(
                onClick = { showListSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 32.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Mosque List", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Bottom Selected Card
            selectedMosque?.let { mosque ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    MosqueItem(
                        mosque = mosque,
                        onDelete = {
                            viewModel.deleteLocation(mosque.id)
                            selectedMosque = null
                        },
                        onClick = {}
                    )
                }
            }
        }
    }

    if (showListSheet) {
        ModalBottomSheet(
            onDismissRequest = { showListSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            MosqueListSheetContent(
                uiState = uiState,
                onMosqueClick = { mosque ->
                    selectedMosque = mosque
                    centerLat = mosque.latitude
                    centerLng = mosque.longitude
                    showListSheet = false
                },
                onDeleteMosque = { id ->
                    viewModel.deleteLocation(id)
                    if (selectedMosque?.id == id) selectedMosque = null
                },
                onRefresh = { viewModel.fetchGlobalMosques() }
            )
        }
    }

    if (showAddDialog) {
        AddCustomMosqueDialog(
            initialLat = longPressLat,
            initialLng = longPressLng,
            onDismiss = { 
                showAddDialog = false 
                longPressLat = null
                longPressLng = null
            },
            onConfirm = { name, lat, lng, address ->
                viewModel.addCustomMosque(name, lat, lng, address)
                showAddDialog = false
                longPressLat = null
                longPressLng = null
            }
        )
    }
}

@Composable
private fun MosqueListSheetContent(
    uiState: MosqueLocationsUiState,
    onMosqueClick: (MosqueLocation) -> Unit,
    onDeleteMosque: (Long) -> Unit,
    onRefresh: () -> Unit
) {
    val globalMosques = uiState.mosques.filter { it.type == "GLOBAL" }
    val customMosques = uiState.mosques.filter { it.type == "CUSTOM" }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Mosques nearby",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        // Global Section
        item {
            SectionHeader(
                title = "Global",
                count = globalMosques.size,
                action = {
                    TextButton(onClick = onRefresh) {
                        Text("Refresh")
                    }
                }
            )
        }

        if (globalMosques.isEmpty()) {
            item { EmptySection("No nearby mosques.", Icons.Rounded.LocationSearching) }
        } else {
            items(globalMosques) { mosque ->
                MosqueItem(mosque, onDelete = { onDeleteMosque(mosque.id) }, onClick = { onMosqueClick(mosque) })
            }
        }

        // Custom Section
        item {
            SectionHeader("Custom", count = customMosques.size)
        }

        if (customMosques.isEmpty()) {
            item { EmptySection("No custom mosques.", Icons.Rounded.Place) }
        } else {
            items(customMosques) { mosque ->
                MosqueItem(mosque, onDelete = { onDeleteMosque(mosque.id) }, onClick = { onMosqueClick(mosque) })
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(6.dp))
            if (count > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
        action?.invoke()
    }
}

// ── Mosque list item ──────────────────────────────────────────────────────────

@Composable
private fun MosqueItem(
    mosque: MosqueLocation,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mosque.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!mosque.address.isNullOrBlank()) {
                    Text(
                        text = mosque.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    TypeChip(mosque.type)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "%.5f, %.5f".format(mosque.latitude, mosque.longitude),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Remove Mosque?") },
            text = { Text("\"${mosque.name}\" will be removed from the list.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Type chip ─────────────────────────────────────────────────────────────────

@Composable
private fun TypeChip(type: String) {
    val isGlobal = type == "GLOBAL"
    val bgColor = if (isGlobal)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.tertiaryContainer
    val textColor = if (isGlobal)
        MaterialTheme.colorScheme.onSecondaryContainer
    else
        MaterialTheme.colorScheme.onTertiaryContainer

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (isGlobal) "Global" else "Custom",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptySection(message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ── Add custom mosque dialog ──────────────────────────────────────────────────

@Composable
private fun AddCustomMosqueDialog(
    initialLat: Double? = null,
    initialLng: Double? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, lat: Double, lng: Double, address: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var latText by remember { mutableStateOf(initialLat?.toString() ?: "") }
    var lngText by remember { mutableStateOf(initialLng?.toString() ?: "") }
    var address by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf<String?>(null) }
    var latError by remember { mutableStateOf<String?>(null) }
    var lngError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        var valid = true
        nameError = if (name.isBlank()) { valid = false; "Name is required" } else null
        val lat = latText.toDoubleOrNull()
        latError = when {
            latText.isBlank() -> { valid = false; "Required" }
            lat == null -> { valid = false; "Invalid number" }
            lat < -90 || lat > 90 -> { valid = false; "Must be −90 to 90" }
            else -> null
        }
        val lng = lngText.toDoubleOrNull()
        lngError = when {
            lngText.isBlank() -> { valid = false; "Required" }
            lng == null -> { valid = false; "Invalid number" }
            lng < -180 || lng > 180 -> { valid = false; "Must be −180 to 180" }
            else -> null
        }
        return valid
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Rounded.AddLocationAlt, contentDescription = null)
        },
        title = { Text("Add Custom Mosque") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Map Preview
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    val plat = latText.toDoubleOrNull() ?: 0.0
                    val plng = lngText.toDoubleOrNull() ?: 0.0
                    dhanfinix.android.sukun.feature.mosque.components.OsmMapView(
                        centerLat = plat,
                        centerLng = plng,
                        zoom = 12.0
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null },
                    label = { Text("Mosque Name") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it; latError = null },
                        label = { Text("Latitude") },
                        singleLine = true,
                        isError = latError != null,
                        supportingText = latError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it; lngError = null },
                        label = { Text("Longitude") },
                        singleLine = true,
                        isError = lngError != null,
                        supportingText = lngError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (validate()) {
                    onConfirm(name, latText.toDouble(), lngText.toDouble(), address)
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
