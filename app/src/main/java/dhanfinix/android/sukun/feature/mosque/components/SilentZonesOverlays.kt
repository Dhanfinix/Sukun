package dhanfinix.android.sukun.feature.mosque.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight as FontWeightCompose
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.database.entity.SilentZone
import dhanfinix.android.sukun.core.database.entity.SilentZoneSource
import dhanfinix.android.sukun.core.utils.withIsolate
import dhanfinix.android.sukun.feature.mosque.MapSuggestion

@Composable
internal fun SilentZonesTopOverlay(
    modifier: Modifier = Modifier,
    searchExpanded: Boolean,
    searchQuery: String,
    includeZones: Boolean,
    zoneMatches: List<SilentZone>,
    mapSuggestions: List<MapSuggestion>,
    hasZones: Boolean,
    isBackgroundLocationGranted: Boolean,
    isDndAccessGranted: Boolean,
    isLocating: Boolean,
    isLocationFresh: Boolean,
    isLocationSilenceEnabled: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleIncludeZones: (Boolean) -> Unit,
    onSearchSubmit: () -> Unit,
    onZoneMatchClick: (SilentZone) -> Unit,
    onMapSuggestionClick: (MapSuggestion) -> Unit,
    onSearchMapClick: () -> Unit,
    onBackgroundPermissionClick: () -> Unit,
    onDndPermissionClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(
            visible = isLocating || !isLocationFresh,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 6.dp
            ) {
                Text(
                    text = if (isLocating) stringResource(R.string.msg_requesting_location)
                    else stringResource(R.string.label_last_location_used),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp
        ) {
            Text(
                stringResource(R.string.hold_to_add_zone),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }

        AnimatedVisibility(
            visible = searchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SilentZonesSearchPanel(
                searchQuery = searchQuery,
                includeZones = includeZones,
                zoneMatches = zoneMatches,
                mapSuggestions = mapSuggestions,
                hasZones = hasZones,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
                onToggleIncludeZones = onToggleIncludeZones,
                onSearchSubmit = onSearchSubmit,
                onZoneMatchClick = onZoneMatchClick,
                onMapSuggestionClick = onMapSuggestionClick,
                onSearchMapClick = onSearchMapClick
            )
        }

        AnimatedVisibility(
            visible = !isBackgroundLocationGranted || !isDndAccessGranted,
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
                            stringResource(R.string.permission_setup_required_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }

                    if (!isBackgroundLocationGranted) {
                        PermissionWarningItem(
                            text = stringResource(R.string.permission_background_location_title),
                            onClick = onBackgroundPermissionClick
                        )
                    }

                    if (!isDndAccessGranted) {
                        PermissionWarningItem(
                            text = stringResource(R.string.permission_dnd_title),
                            onClick = onDndPermissionClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SilentZonesSearchPanel(
    searchQuery: String,
    includeZones: Boolean,
    zoneMatches: List<SilentZone>,
    mapSuggestions: List<MapSuggestion>,
    hasZones: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleIncludeZones: (Boolean) -> Unit,
    onSearchSubmit: () -> Unit,
    onZoneMatchClick: (SilentZone) -> Unit,
    onMapSuggestionClick: (MapSuggestion) -> Unit,
    onSearchMapClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = onClearSearch) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_clear_search))
                        }
                    }
                },
                placeholder = { Text(stringResource(R.string.search_location_placeholder)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = {
                    onSearchSubmit()
                })
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        stringResource(R.string.include_zones),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        stringResource(R.string.include_zones_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = includeZones,
                    onCheckedChange = onToggleIncludeZones
                )
            }

            if (hasZones) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (includeZones && zoneMatches.isNotEmpty()) {
                        Text(
                            stringResource(R.string.suggestions_zones),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        zoneMatches.take(4).forEach { zone ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onZoneMatchClick(zone) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(zone.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    if (searchQuery.isNotBlank() && mapSuggestions.isNotEmpty()) {
                        Text(
                            stringResource(R.string.suggestions_map),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        mapSuggestions.take(3).forEach { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMapSuggestionClick(suggestion) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Map,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        suggestion.title,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (!suggestion.subtitle.isNullOrBlank()) {
                                        Text(
                                            suggestion.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    } else if (searchQuery.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSearchMapClick() }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Map,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.suggestion_search_map, searchQuery),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SilentZonesBottomOverlay(
    modifier: Modifier = Modifier,
    isLocationFresh: Boolean,
    isLocating: Boolean,
    onMyLocation: () -> Unit,
    onAddZone: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = onMyLocation,
                containerColor = when {
                    isLocating -> MaterialTheme.colorScheme.primaryContainer
                    isLocationFresh -> MaterialTheme.colorScheme.surface
                    else -> MaterialTheme.colorScheme.errorContainer
                },
                contentColor = when {
                    isLocating -> MaterialTheme.colorScheme.onPrimaryContainer
                    isLocationFresh -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = if (isLocating) Icons.Rounded.GpsFixed else Icons.Rounded.MyLocation,
                    contentDescription = stringResource(R.string.cd_my_location)
                )
            }
            Spacer(Modifier.height(16.dp))
            FloatingActionButton(
                onClick = onAddZone,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_add_zone))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SilentZonesSheetContent(
    zones: List<SilentZone>,
    activeZoneId: Long?,
    isLocationSilenceEnabled: Boolean,
    isAutoMosqueSilenceEnabled: Boolean,
    isAggressiveLocationEnabled: Boolean,
    activeGeofenceCount: Int = 0,
    maxGeofenceSlots: Int = 10,
    onFocusZone: (SilentZone) -> Unit,
    onEditZone: (SilentZone) -> Unit,
    onDeleteZone: (SilentZone) -> Unit,
    onToggleEnabled: (SilentZone) -> Unit,
    onToggleAutoSilent: (SilentZone) -> Unit,
    onLocationSilenceToggled: (Boolean) -> Unit,
    onAutoMosqueSilenceToggled: (Boolean) -> Unit,
    onAggressiveLocationToggled: (Boolean) -> Unit,
    onRefreshMosques: () -> Unit,
    onToggleMosquePin: (SilentZone) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Drag handle
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(50)
            ) {
                Spacer(Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
            }
        }

        // Tab row
        SecondaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        "${stringResource(R.string.tab_custom)} (${java.lang.String.format(java.util.Locale.US, "%d", zones.count { it.source == SilentZoneSource.MANUAL })})"
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        "${stringResource(R.string.tab_mosques)} (${java.lang.String.format(java.util.Locale.US, "%d", zones.count { it.source == SilentZoneSource.AUTO_MOSQUE })})"
                    )
                }
            )
        }

        // Feature toggle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
        ) {
            Surface(
                onClick = {
                    if (selectedTab == 0) {
                        onLocationSilenceToggled(!isLocationSilenceEnabled)
                    } else {
                        onAutoMosqueSilenceToggled(!isAutoMosqueSilenceEnabled)
                    }
                },
                shape = MaterialTheme.shapes.medium,
                color = if (if (selectedTab == 0) isLocationSilenceEnabled else isAutoMosqueSilenceEnabled)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedTab == 0) {
                            if (isLocationSilenceEnabled) Icons.Rounded.GpsFixed else Icons.Rounded.LocationOff
                        } else {
                            if (isAutoMosqueSilenceEnabled) Icons.Rounded.GpsFixed else Icons.Rounded.LocationOff
                        },
                        contentDescription = null,
                        tint = if (if (selectedTab == 0) isLocationSilenceEnabled else isAutoMosqueSilenceEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (selectedTab == 0) stringResource(R.string.location_silence_title) else stringResource(R.string.auto_mosque_silence_title),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeightCompose.Bold
                        )
                        Text(
                            if (if (selectedTab == 0) isLocationSilenceEnabled else isAutoMosqueSilenceEnabled)
                                if (selectedTab == 0) stringResource(R.string.location_silence_desc) else stringResource(R.string.auto_mosque_silence_desc)
                            else
                                stringResource(R.string.status_location_silence_disabled),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = if (selectedTab == 0) isLocationSilenceEnabled else isAutoMosqueSilenceEnabled,
                        onCheckedChange = if (selectedTab == 0) onLocationSilenceToggled else onAutoMosqueSilenceToggled,
                        modifier = Modifier.scale(0.8f)
                    )
                }
            }

            // Instant Geofence toggle — only on Mosque tab
            AnimatedVisibility(
                visible = selectedTab == 1 && isAutoMosqueSilenceEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    onClick = { onAggressiveLocationToggled(!isAggressiveLocationEnabled) },
                    shape = MaterialTheme.shapes.medium,
                    color = Color.Transparent,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GpsFixed,
                            contentDescription = null,
                            tint = if (isAggressiveLocationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.aggressive_location_mode_title),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isAggressiveLocationEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                stringResource(R.string.aggressive_location_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isAggressiveLocationEnabled,
                            onCheckedChange = onAggressiveLocationToggled,
                            modifier = Modifier.scale(0.7f)
                        )
                    }
                }
            }
        }

        // Zone list
        val filteredZones = zones.filter {
            if (selectedTab == 0) it.source == SilentZoneSource.MANUAL
            else it.source == SilentZoneSource.AUTO_MOSQUE
        }

        if (filteredZones.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (selectedTab == 0) stringResource(R.string.no_zones_added) else stringResource(R.string.no_mosques_detected),
                    color = MaterialTheme.colorScheme.secondary
                )
                if (selectedTab == 1) {
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = onRefreshMosques,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_refresh))
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedTab == 1) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                stringResource(R.string.label_detected_mosques),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeightCompose.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val slotColor = if (activeGeofenceCount >= maxGeofenceSlots)
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.primaryContainer
                                val slotTextColor = if (activeGeofenceCount >= maxGeofenceSlots)
                                    MaterialTheme.colorScheme.onErrorContainer
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                Surface(
                                    color = slotColor,
                                    shape = RoundedCornerShape(50),
                                    tonalElevation = 0.dp
                                ) {
                                    Text(
                                        stringResource(R.string.geofence_counter, activeGeofenceCount, maxGeofenceSlots),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = slotTextColor
                                    )
                                }
                                TextButton(
                                    onClick = onRefreshMosques,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.btn_refresh), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                items(filteredZones) { zone ->
                    val isActive = activeZoneId == zone.id
                    SilentZoneCard(
                        zone = zone,
                        isActive = isActive,
                        isGlobalEnabled = if (selectedTab == 0) isLocationSilenceEnabled else isAutoMosqueSilenceEnabled,
                        onToggleEnabled = { onToggleEnabled(zone) },
                        onToggleAutoSilent = { onToggleAutoSilent(zone) },
                        onFocus = { onFocusZone(zone) },
                        onEdit = { onEditZone(zone) },
                        onDelete = { onDeleteZone(zone) },
                        onTogglePin = if (zone.source == SilentZoneSource.AUTO_MOSQUE) {
                            { onToggleMosquePin(zone) }
                        } else null
                    )
                }
            }
        }
    }
}


@Composable
private fun SilentZoneCard(
    zone: SilentZone,
    isActive: Boolean,
    isGlobalEnabled: Boolean,
    onToggleEnabled: () -> Unit,
    onToggleAutoSilent: () -> Unit,
    onFocus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFocus() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = when {
                !isGlobalEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                isActive -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = if (isGlobalEnabled) 2.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            zone.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeightCompose.Bold,
                            color = when {
                                !isGlobalEnabled -> MaterialTheme.colorScheme.onSurfaceVariant
                                isActive -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (isActive && isGlobalEnabled) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    stringResource(R.string.status_active_caps),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    // Zone radius
                    Text(
                        java.lang.String.format(
                            java.util.Locale.US,
                            stringResource(R.string.radius_value_format),
                            zone.radius.toInt()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isGlobalEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    // Pinning / Geofence status badge for Auto-Mosque zones
                    if (onTogglePin != null) {
                        Spacer(Modifier.height(8.dp))
                        val (badgeText, badgeColor, badgeTextColor) = when {
                            zone.isUserPinned -> Triple(
                                stringResource(R.string.geofence_pinned),
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            zone.hasActiveGeofence -> Triple(
                                stringResource(R.string.geofence_active),
                                MaterialTheme.colorScheme.tertiaryContainer,
                                MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            else -> Triple(
                                stringResource(R.string.geofence_inactive),
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(8.dp),
                            onClick = onTogglePin,
                            enabled = isGlobalEnabled
                        ) {
                            Text(
                                badgeText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeTextColor
                            )
                        }
                    }
                }

                Switch(
                    checked = zone.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    enabled = isGlobalEnabled
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = if (isGlobalEnabled) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.auto_silent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isGlobalEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (zone.isAutoSilent) stringResource(R.string.status_enabled)
                        else stringResource(R.string.status_notification_only),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isGlobalEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.cd_edit_zone),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.cd_delete_zone),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionWarningItem(
    text: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeightCompose.SemiBold
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
