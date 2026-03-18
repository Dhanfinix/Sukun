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
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight as FontWeightCompose
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.database.entity.SilentZone
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

@Composable
internal fun SilentZonesBottomOverlay(
    modifier: Modifier = Modifier,
    zones: List<SilentZone>,
    activeZoneId: Long?,
    isLocationFresh: Boolean,
    isLocating: Boolean,
    isListExpanded: Boolean,
    onToggleList: () -> Unit,
    onMyLocation: () -> Unit,
    onAddZone: () -> Unit,
    onFocusZone: (SilentZone) -> Unit,
    onEditZone: (SilentZone) -> Unit,
    onDeleteZone: (SilentZone) -> Unit,
    onToggleEnabled: (SilentZone) -> Unit,
    onToggleAutoSilent: (SilentZone) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleList() }
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
                            text = "${stringResource(R.string.action_list)} (${java.lang.String.format(java.util.Locale.US, "%d", zones.size)})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeightCompose.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (isListExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (isListExpanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand)
                    )
                }

                if (isListExpanded) {
                    if (zones.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(R.string.no_zones_added),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxHeight(0.6f)
                        ) {
                            items(zones) { zone ->
                                val isActive = activeZoneId == zone.id
                                SilentZoneCard(
                                    zone = zone,
                                    isActive = isActive,
                                    onToggleEnabled = { onToggleEnabled(zone) },
                                    onToggleAutoSilent = { onToggleAutoSilent(zone) },
                                    onFocus = { onFocusZone(zone) },
                                    onEdit = { onEditZone(zone) },
                                    onDelete = { onDeleteZone(zone) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SilentZoneCard(
    zone: SilentZone,
    isActive: Boolean,
    onToggleEnabled: () -> Unit,
    onToggleAutoSilent: () -> Unit,
    onFocus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFocus() },
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
                                    stringResource(R.string.status_active_caps),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Text(
                        java.lang.String.format(
                            java.util.Locale.US,
                            stringResource(R.string.radius_value_format),
                            zone.radius.toInt()
                        ),
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
                        stringResource(R.string.auto_silent),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        if (zone.isAutoSilent) stringResource(R.string.status_enabled)
                        else stringResource(R.string.status_notification_only),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        if (zone.silenceDuration != null && zone.silenceDuration >= 60) {
                            java.lang.String.format(
                                java.util.Locale.US,
                                stringResource(R.string.ends_in_hours),
                                zone.silenceDuration / 60
                            )
                        } else {
                            java.lang.String.format(
                                java.util.Locale.US,
                                stringResource(R.string.ends_in_mins),
                                zone.silenceDuration ?: 30
                            )
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeightCompose.Bold
                    )
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.cd_edit_zone),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.cd_delete_zone),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
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
