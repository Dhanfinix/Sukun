package dhanfinix.android.sukun.feature.prayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.utils.localizeDigits
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationsBottomSheet(
    durations: Map<PrayerName, Int>,
    isUniform: Boolean,
    onDurationChange: (PrayerName, Int) -> Unit,
    onUniformChange: (Boolean) -> Unit,
    onAllDurationsChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                stringResource(R.string.feature_silence_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.uniform_silence_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.uniform_silence),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isUniform,
                    onCheckedChange = onUniformChange
                )
            }
            Spacer(Modifier.height(16.dp))

            if (isUniform) {
                val currentDur = durations.values.firstOrNull() ?: 15
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.all_prayers),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = pluralStringResource(R.plurals.minutes_plural, currentDur, currentDur).localizeDigits(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(48.dp)
                    )

                    Slider(
                        value = currentDur.toFloat(),
                        onValueChange = { onAllDurationsChange(it.toInt()) },
                        valueRange = 5f..120f,
                        steps = 22,
                        modifier = Modifier.weight(2f)
                    )
                }
            } else {
                PrayerName.entries.forEach { prayer ->
                    val currentDur = durations[prayer] ?: if (prayer == PrayerName.JUMUAH) 45 else 15
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(prayer.nameRes),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(
                            text = pluralStringResource(R.plurals.minutes_plural, currentDur, currentDur).localizeDigits(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(48.dp)
                        )

                        Slider(
                            value = currentDur.toFloat(),
                            onValueChange = { onDurationChange(prayer, it.toInt()) },
                            valueRange = 5f..120f,
                            steps = 22, // Steps between 5 and 120 every 5 mins
                            modifier = Modifier.weight(2f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
