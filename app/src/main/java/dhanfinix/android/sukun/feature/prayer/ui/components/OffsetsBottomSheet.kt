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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dhanfinix.android.sukun.R
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
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
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
                        val signText = if (currentOffset > 0) "+" else ""
                        Text(
                            text = "$signText${currentOffset}m",
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
