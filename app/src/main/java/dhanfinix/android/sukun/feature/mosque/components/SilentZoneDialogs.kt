package dhanfinix.android.sukun.feature.mosque.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight as FontWeightCompose
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.database.entity.SilentZone

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun SilentZoneEditDialog(
    zone: SilentZone?,
    onDismiss: () -> Unit,
    onSave: (name: String, radius: Float, isAutoSilent: Boolean, silenceDuration: Int?) -> Unit
) {
    var name by remember(zone) { mutableStateOf(zone?.name ?: "") }
    var radius by remember(zone) { mutableStateOf(zone?.radius?.toInt()?.toString() ?: "20") }
    var isAutoSilent by remember(zone) { mutableStateOf(zone?.isAutoSilent ?: true) }
    var silenceDuration by remember(zone) { mutableStateOf(zone?.silenceDuration ?: 30) }
    var showDurationDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                    onClick = onDismiss,
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
                    if (zone == null) stringResource(R.string.new_silent_zone)
                    else stringResource(R.string.edit_silent_zone),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeightCompose.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.zone_name)) },
                    placeholder = { Text(stringResource(R.string.zone_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Rounded.Label, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.GpsFixed,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(
                                    R.string.radius_format,
                                    radius.toIntOrNull() ?: 20
                                ),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        Slider(
                            value = (radius.toFloatOrNull() ?: 20f).coerceIn(10f, 100f),
                            onValueChange = { radius = it.toInt().toString() },
                            valueRange = 10f..100f,
                            steps = 17,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.setting_auto_silence),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = isAutoSilent,
                            onClick = { isAutoSilent = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text(stringResource(R.string.auto_mute))
                        }
                        SegmentedButton(
                            selected = !isAutoSilent,
                            onClick = { isAutoSilent = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text(stringResource(R.string.notify_only))
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = showDurationDropdown,
                    onExpandedChange = { showDurationDropdown = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when {
                            silenceDuration == 15 -> stringResource(R.string.duration_15_min)
                            silenceDuration == 30 -> stringResource(R.string.duration_30_min)
                            (silenceDuration ?: 30) % 60 == 0 -> stringResource(
                                R.string.duration_hours,
                                (silenceDuration ?: 30) / 60
                            )
                            else -> stringResource(R.string.duration_minutes, silenceDuration ?: 30)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.duration)) },
                        leadingIcon = { Icon(Icons.Rounded.Timer, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDurationDropdown) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = showDurationDropdown,
                        onDismissRequest = { showDurationDropdown = false }
                    ) {
                        listOf(15, 30, 60, 120, 180, 240, 360, 480, 720).forEach { dur ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when {
                                            dur == 15 -> stringResource(R.string.duration_15_min)
                                            dur == 30 -> stringResource(R.string.duration_30_min)
                                            dur % 60 == 0 -> stringResource(
                                                R.string.duration_hours,
                                                dur / 60
                                            )
                                            else -> stringResource(R.string.duration_minutes, dur)
                                        }
                                    )
                                },
                                onClick = {
                                    silenceDuration = dur
                                    showDurationDropdown = false
                                },
                                leadingIcon = {
                                    Icon(
                                        if (silenceDuration == dur) Icons.Rounded.CheckCircle
                                        else Icons.Rounded.Circle,
                                        null,
                                        tint = if (silenceDuration == dur) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline
                                    )
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
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
                        stringResource(R.string.silence_stops_on_exit),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name,
                        radius.toFloatOrNull() ?: 20f,
                        isAutoSilent,
                        silenceDuration
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.save_zone))
            }
        }
    )
}

@Composable
internal fun BackgroundLocationRationaleDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onOpenAppInfo: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bg_location_rationale_title)) },
        text = { Text(stringResource(R.string.bg_location_rationale_desc)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.btn_open_settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenAppInfo) {
                Text(stringResource(R.string.btn_app_info))
            }
        }
    )
}
