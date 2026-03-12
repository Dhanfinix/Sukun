package dhanfinix.android.sukun.feature.home.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.feature.volume.VolumeUiState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import dhanfinix.android.sukun.feature.home.components.CoachMarkTarget

@Composable
fun LocationSilenceSection(
    state: VolumeUiState,
    onManageClick: () -> Unit,
    onSilenceNowClick: () -> Unit,
    modifier: Modifier = Modifier,
    onTargetPositioned: ((CoachMarkTarget, Rect) -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onTargetPositioned?.invoke(CoachMarkTarget.LOCATION_SILENCE, coordinates.boundsInWindow())
            },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (state.activeSilentZoneName != null) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (state.activeSilentZoneName != null) Icons.Rounded.GpsFixed else Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = if (state.activeSilentZoneName != null) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.location_silence_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            state.activeSilentZoneName != null && state.isSukunActive -> stringResource(R.string.status_protection_active)
                            state.activeSilentZoneName != null && !state.isSukunActive -> stringResource(R.string.status_inside_protection_zone)
                            state.silentZoneCount > 0 -> stringResource(R.string.status_zones_active, state.silentZoneCount)
                            else -> stringResource(R.string.desc_location_silence)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = onManageClick) {
                    Text(stringResource(R.string.action_manage))
                }
            }

            AnimatedVisibility(
                visible = state.activeSilentZoneName != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (state.isSukunActive) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (state.isSukunActive) Icons.Rounded.NearMe else Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = if (state.isSukunActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (state.isSukunActive)
                                stringResource(R.string.status_silenced_at, state.activeSilentZoneName ?: "")
                            else
                                stringResource(R.string.status_nearby, state.activeSilentZoneName ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (state.isSukunActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (!state.isSukunActive) {
                        Button(
                            onClick = onSilenceNowClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Rounded.NotificationsOff, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_silence_now), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
