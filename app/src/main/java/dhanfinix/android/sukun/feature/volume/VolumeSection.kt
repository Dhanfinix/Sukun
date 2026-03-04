package dhanfinix.android.sukun.feature.volume

import dhanfinix.android.sukun.feature.volume.components.*
import androidx.compose.ui.res.stringResource
import dhanfinix.android.sukun.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import dhanfinix.android.sukun.feature.home.components.CoachMarkTarget
import kotlinx.coroutines.launch

@Composable
fun VolumeSection(
    state: VolumeUiState,
    onEvent: (VolumeEvent) -> Unit,
    showManualSilenceSheet: Boolean,
    onManualSilenceDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onTargetPositioned: ((CoachMarkTarget, Rect) -> Unit)? = null
) {
    // ── Overwrite Confirmation Dialog ──
    if (state.pendingOverwriteDurationMin != null) {
        AlertDialog(
            onDismissRequest = { onEvent(VolumeEvent.DismissOverwrite) },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text(stringResource(R.string.silence_already_active_title)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.confirm_overwrite_silence,
                        state.sukunLabel ?: stringResource(R.string.label_unknown),
                        state.pendingOverwriteDurationMin ?: 0
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { onEvent(VolumeEvent.ConfirmOverwrite) }) {
                    Text(stringResource(R.string.btn_yes_overwrite))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(VolumeEvent.DismissOverwrite) }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = stringResource(R.string.volume_controls),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp)
        )
        
        // ── Media Volume ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    onTargetPositioned?.invoke(CoachMarkTarget.VOLUME_SLIDER, coordinates.boundsInWindow())
                }
        ) {
            VolumeCard(
                icon = Icons.Rounded.MusicNote,
                label = stringResource(R.string.media_volume),
                tooltip = stringResource(R.string.media_volume_desc),
                value = state.mediaVolume,
                maxSteps = state.maxMedia,
                onValueChange = { onEvent(VolumeEvent.MediaChanged(it)) },
                enabled = !state.isSukunActive
            )
        }

        // ── Ringer Volume ──
        VolumeCard(
            icon = Icons.Rounded.RingVolume,
            label = if (state.isNotifLinked) stringResource(R.string.ringer_notif_volume) else stringResource(R.string.ring_volume),
            tooltip = stringResource(R.string.ring_volume_desc),
            value = state.ringVolume,
            maxSteps = state.maxRing,
            onValueChange = { onEvent(VolumeEvent.RingChanged(it)) },
            enabled = state.hasDndPermission && !state.isSukunActive
        )

        // ── Notification Volume ──
        AnimatedVisibility(
            visible = !state.isNotifLinked,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            VolumeCard(
                icon = Icons.Rounded.Notifications,
                label = stringResource(R.string.notification_volume),
                tooltip = stringResource(R.string.notification_volume_desc),
                value = state.notificationVolume,
                maxSteps = state.maxNotif,
                onValueChange = { onEvent(VolumeEvent.NotificationChanged(it)) },
                enabled = state.hasDndPermission && !state.isSukunActive
            )
        }

        // ── Alarm Volume ──
        VolumeCard(
            icon = Icons.Rounded.Alarm,
            label = stringResource(R.string.alarm_volume),
            tooltip = stringResource(R.string.alarm_volume_desc),
            value = state.alarmVolume,
            maxSteps = state.maxAlarm,
            onValueChange = { onEvent(VolumeEvent.AlarmChanged(it)) },
            enabled = !state.isSukunActive
        )


        if (!state.isSystemLinked) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.link_ringer_notif),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.sync_streams_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.isNotifLinked,
                    onCheckedChange = { onEvent(VolumeEvent.NotifLinkToggled) },
                    enabled = !state.isSukunActive
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = stringResource(R.string.vibrate_in_silence),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.vibrate_in_silence_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.silenceMode == dhanfinix.android.sukun.core.datastore.SilenceMode.VIBRATE,
                onCheckedChange = { isVibrate -> 
                    val mode = if (isVibrate) dhanfinix.android.sukun.core.datastore.SilenceMode.VIBRATE else dhanfinix.android.sukun.core.datastore.SilenceMode.SILENT
                    onEvent(VolumeEvent.SilenceModeChanged(mode))
                },
                enabled = !state.isSukunActive
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showManualSilenceSheet) {
        ManualSilenceBottomSheet(
            onDismiss = onManualSilenceDismiss,
            onStart = { duration ->
                onEvent(VolumeEvent.StartManualSilence(duration))
                onManualSilenceDismiss()
            }
        )
    }
}
