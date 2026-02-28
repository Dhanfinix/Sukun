package dhanfinix.android.sukun.feature.prayer.ui
import dhanfinix.android.sukun.feature.prayer.ui.components.*
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import dhanfinix.android.sukun.feature.home.components.CoachMarkTarget
import dhanfinix.android.sukun.feature.prayer.data.model.LocationSuggestion
import dhanfinix.android.sukun.core.designsystem.shimmer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mosque
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerInfo
import dhanfinix.android.sukun.feature.prayer.data.model.PrayerName

/**
 * Stateless Prayer section.
 * Designed to be placed inside a parent Column (single-screen layout).
 * Receives [PrayerUiState], emits [PrayerEvent] via [onEvent] — pure UDF composable.
 */
@Composable
fun PrayerSection(
    state: PrayerUiState,
    onEvent: (PrayerEvent) -> Unit,
    modifier: Modifier = Modifier,
    onTargetPositioned: ((CoachMarkTarget, Rect) -> Unit)? = null
) {
    val context = LocalContext.current
    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            Toast.makeText(context, state.errorMessage, Toast.LENGTH_LONG).show()
            onEvent(PrayerEvent.ErrorMessageConsumed)
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val granted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (granted) onEvent(PrayerEvent.DetectLocation)
        }
    )

    var showSilenceSheet by remember { mutableStateOf(false) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }

    val currentOnEvent by rememberUpdatedState(onEvent)
    
    val onLocationClick: () -> Unit = remember(locationPermissionLauncher) {
        {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }

    val onSilenceClick: () -> Unit = remember { { showSilenceSheet = true } }
    val onMethodClick: () -> Unit = remember { { showMethodSheet = true } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Integrated Next Prayer & Location Dashboard ──
        NextPrayerCard(
            currentTime = state.currentTime,
            nextPrayerName = state.nextPrayerName,
            countdown = state.nextPrayerCountdown,
            locationName = state.locationName ?: "${state.latitude}, ${state.longitude}",
            isDetectingLocation = state.isDetectingLocation,
            onLocationClick = onLocationClick,
            onSearchClick = { showSearchDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    onTargetPositioned?.invoke(CoachMarkTarget.NEXT_PRAYER, coordinates.boundsInWindow())
                }
        )

        // ── Prayer Times Row ──
        val onTogglePrayer: (PrayerName) -> Unit = remember {
            { name -> currentOnEvent(PrayerEvent.TogglePrayer(name)) }
        }

        val displayPrayers = state.prayers.ifEmpty {
            PrayerName.entries.map { PrayerInfo(it, "--:--", true) }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    onTargetPositioned?.invoke(CoachMarkTarget.PRAYER_TOGGLES, coordinates.boundsInWindow())
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            displayPrayers.forEach { prayer ->
                key(prayer.name) {
                    PrayerTile(
                        prayer = prayer,
                        isNext = (prayer.name.displayName == state.nextPrayerName),
                        onToggle = onTogglePrayer,
                        isLoading = state.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // ── Compact Settings Row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Duration Card
            SettingCard(
                label = "Silence",
                currentValue = "${state.silenceDurationMin}m",
                icon = Icons.Rounded.Timer,
                onClick = onSilenceClick,
                modifier = Modifier.weight(1f)
            )

            // Method Card
            val methods = listOf(
                20 to "Kemenag", 4 to "Umm al-Qura", 2 to "ISNA",
                3 to "MWL", 1 to "Karachi", 5 to "Egypt"
            )
            SettingCard(
                label = "Method",
                currentValue = methods.find { it.first == state.method }?.second ?: "Auto",
                icon = Icons.Rounded.Calculate,
                onClick = onMethodClick,
                modifier = Modifier.weight(1f)
            )
        }
    }

    // ── Bottom Sheets ──
    if (showSilenceSheet) {
        val options = listOf(10, 15, 20, 30)
        SelectionBottomSheet(
            title = "Silence Duration",
            description = "How long your phone stays muted after each prayer time.",
            options = options.map { it to "${it} Minutes" },
            selectedValue = state.silenceDurationMin,
            onSelect = { 
                onEvent(PrayerEvent.DurationSelected(it))
                showSilenceSheet = false
            },
            onDismiss = { showSilenceSheet = false }
        )
    }

    if (showMethodSheet) {
        val methods = listOf(
            20 to "Kemenag", 4 to "Umm al-Qura", 2 to "ISNA",
            3 to "MWL", 1 to "Karachi", 5 to "Egypt"
        )
        SelectionBottomSheet(
            title = "Calculation Method",
            description = "Different organizations calculate prayer times using slightly different conventions. Choose the one commonly used in your region.",
            options = methods,
            selectedValue = state.method,
            onSelect = { 
                onEvent(PrayerEvent.MethodChanged(it))
                showMethodSheet = false
            },
            onDismiss = { showMethodSheet = false }
        )
    }

    if (showSearchDialog) {
        ManualLocationBottomSheet(
            suggestions = state.locationSuggestions,
            isSearching = state.isSearchingSuggestions,
            onQueryChange = { query -> onEvent(PrayerEvent.SearchQueryChanged(query)) },
            onSuggestionSelect = { suggestion ->
                onEvent(PrayerEvent.SuggestionSelected(suggestion))
                showSearchDialog = false
            },
            onSearch = { query ->
                onEvent(PrayerEvent.SearchLocation(query))
                showSearchDialog = false
            },
            onDismiss = { 
                showSearchDialog = false
                onEvent(PrayerEvent.ClearSuggestions)
            }
        )
    }
}

