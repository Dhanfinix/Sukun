package dhanfinix.android.sukun.feature.landing

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dhanfinix.android.sukun.MainViewModel
import dhanfinix.android.sukun.R
import dhanfinix.android.sukun.core.datastore.AppLanguage

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LandingScreen(
    mainVm: MainViewModel,
    onGetStarted: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    val appLanguage by mainVm.appLanguage.collectAsState()
    
    LaunchedEffect(Unit) { visible = true }

    val features = listOf(
        Triple(Icons.AutoMirrored.Rounded.VolumeOff, stringResource(R.string.feature_silence_title),
            stringResource(R.string.feature_silence_desc)),
        Triple(Icons.Rounded.LocationOn, stringResource(R.string.feature_location_title),
            stringResource(R.string.feature_location_desc)),
        Triple(Icons.Rounded.Timer, stringResource(R.string.feature_timing_title),
            stringResource(R.string.feature_timing_desc)),
        Triple(Icons.Rounded.Tune, stringResource(R.string.feature_custom_title),
            stringResource(R.string.feature_custom_desc))
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 48.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // ── Hero Icon — shared element from Splash ──
        with(sharedTransitionScope) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier
                    .size(96.dp)
                    .sharedElement(
                        state = rememberSharedContentState(key = "app_icon"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        with(sharedTransitionScope) {
            Text(
                text = stringResource(R.string.landing_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "tagline"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── Feature Cards ──
        features.forEachIndexed { index, (icon, title, desc) ->
            AnimatedVisibility(
                visible = visible,
                enter = slideInHorizontally(tween(400, delayMillis = 250 + index * 80)) { -it / 2 } +
                        fadeIn(tween(400, delayMillis = 250 + index * 80))
            ) {
                FeatureRow(icon = icon, title = title, description = desc)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── CTA Button ──
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            ) { it / 2 } + fadeIn(tween(400, delayMillis = 600))
        ) {
            Button(
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null,
                    modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.btn_get_started),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
        }
        }

        // Language Button at top right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(
                onClick = { showLanguageSheet = true },
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Rounded.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (appLanguage) {
                        AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                        else -> appLanguage.name
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showLanguageSheet) {
        LanguageSelectionSheet(
            currentLanguage = appLanguage,
            onLanguageSelected = { mainVm.setLanguage(it) },
            onDismiss = { showLanguageSheet = false }
        )
    }
}


@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectionSheet(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.app_language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
            )

            val options = listOf(
                AppLanguage.SYSTEM to stringResource(R.string.language_system),
                AppLanguage.EN to stringResource(R.string.language_english),
                AppLanguage.ID to stringResource(R.string.language_indonesian)
            )

            options.forEach { (value, label) ->
                val isSelected = value == currentLanguage
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { 
                            onLanguageSelected(value)
                            onDismiss()
                        },
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

