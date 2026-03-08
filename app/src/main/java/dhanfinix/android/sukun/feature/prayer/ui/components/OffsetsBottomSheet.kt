package dhanfinix.android.sukun.feature.prayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import dhanfinix.android.sukun.R
import kotlin.math.absoluteValue
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerInfo
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffsetsBottomSheet(
    prayers: List<PrayerInfo>,
    offsets: Map<PrayerName, Int>,
    onOffsetChange: (PrayerName, Int) -> Unit,
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
                stringResource(R.string.time_adjustments_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.time_adjustments_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            PrayerName.entries.forEach { prayer ->
                val currentOffset = offsets[prayer] ?: 0
                val prayerInfo = prayers.find { it.name == prayer }
                val timeStr = prayerInfo?.time ?: "--:--"
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(prayer.nameRes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = prayerInfo?.formattedTime?.time ?: timeStr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                prayerInfo?.formattedTime?.session?.let { session ->
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = session,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.7f),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Minus Button
                        IconButton(
                            onClick = { 
                                val newOffset = (currentOffset - 1).coerceAtLeast(-30)
                                onOffsetChange(prayer, newOffset)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = "Decrease",
                                modifier = Modifier.size(20.dp),
                                tint = if (currentOffset > -30) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        // Offset Value
                        val offsetText = when {
                            currentOffset == 0 -> java.lang.String.format(java.util.Locale.US, pluralStringResource(R.plurals.minutes_plural, 0), 0)
                            currentOffset > 0 -> "+${java.lang.String.format(java.util.Locale.US, pluralStringResource(R.plurals.minutes_plural, currentOffset), currentOffset)}"
                            else -> "-${java.lang.String.format(java.util.Locale.US, pluralStringResource(R.plurals.minutes_plural, currentOffset.absoluteValue), currentOffset.absoluteValue)}"
                        }
                        Text(
                            text = offsetText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentOffset != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.widthIn(min = 48.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        // Plus Button
                        IconButton(
                            onClick = { 
                                val newOffset = (currentOffset + 1).coerceAtMost(30)
                                onOffsetChange(prayer, newOffset)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Increase",
                                modifier = Modifier.size(20.dp),
                                tint = if (currentOffset < 30) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
                if (prayer != PrayerName.ISHA) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest, thickness = 1.dp)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
