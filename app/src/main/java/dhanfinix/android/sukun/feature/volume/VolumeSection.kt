package dhanfinix.android.sukun.feature.volume

import dhanfinix.android.sukun.feature.volume.components.*
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
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Countdown Overlay ──
        AnimatedVisibility(
            visible = state.isSukunActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            var remainingTimeStr by remember { mutableStateOf("") }

            LaunchedEffect(state.sukunEndTime) {
                while (state.isSukunActive && state.sukunEndTime > System.currentTimeMillis()) {
                    val remainingMs = state.sukunEndTime - System.currentTimeMillis()
                    val totalSecs = (remainingMs / 1000).toInt()
                    
                    val hours = totalSecs / 3600
                    val minutes = (totalSecs % 3600) / 60
                    val seconds = totalSecs % 60
                    
                    remainingTimeStr = if (hours > 0) {
                        String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format("%02d:%02d", minutes, seconds)
                    }
                    
                    kotlinx.coroutines.delay(1000)
                }
            }

            SilenceCountdownOverlay(
                label = state.sukunLabel ?: "Active",
                remainingTimeStr = remainingTimeStr,
                onStop = { onEvent(VolumeEvent.StopSilence) }
            )
        }
        
        Text(
            text = "Volume Controls",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
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
                label = "Media",
                tooltip = "Music, videos, games, and other media",
                value = state.mediaVolume,
                maxSteps = state.maxMedia,
                onValueChange = { onEvent(VolumeEvent.MediaChanged(it)) },
                enabled = !state.isSukunActive
            )
        }

        // ── Ringer Volume ──
        VolumeCard(
            icon = Icons.Rounded.RingVolume,
            label = if (state.isNotifLinked) "Ring & Notifications" else "Ring",
            tooltip = "Incoming phone calls and SMS tones",
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
                label = "Notifications",
                tooltip = "App notifications, emails, and chat messages",
                value = state.notificationVolume,
                maxSteps = state.maxNotif,
                onValueChange = { onEvent(VolumeEvent.NotificationChanged(it)) },
                enabled = state.hasDndPermission && !state.isSukunActive
            )
        }

        // ── Alarm Volume ──
        VolumeCard(
            icon = Icons.Rounded.Alarm,
            label = "Alarm",
            tooltip = "Scheduled alarms and timers",
            value = state.alarmVolume,
            maxSteps = state.maxAlarm,
            onValueChange = { onEvent(VolumeEvent.AlarmChanged(it)) },
            enabled = !state.isSukunActive
        )

        // ── Advanced Options ──
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // ── Silence Mode Selection ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Silence Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Choose how Sukun mutes your phone during prayers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                val options = dhanfinix.android.sukun.core.datastore.SilenceMode.values()
                options.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { onEvent(VolumeEvent.SilenceModeChanged(mode)) },
                        selected = state.silenceMode == mode
                    ) {
                        Text(
                            text = when (mode) {
                                dhanfinix.android.sukun.core.datastore.SilenceMode.DND -> "DND"
                                dhanfinix.android.sukun.core.datastore.SilenceMode.SILENT -> "Silent"
                                dhanfinix.android.sukun.core.datastore.SilenceMode.VIBRATE -> "Vibrate"
                            }
                        )
                    }
                }
            }
        }

        if (!state.isSystemLinked) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Link Ringer & Notification",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Synchronize both volume streams",
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
