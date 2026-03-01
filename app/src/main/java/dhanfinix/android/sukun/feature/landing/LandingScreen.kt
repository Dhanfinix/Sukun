package dhanfinix.android.sukun.feature.landing

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dhanfinix.android.sukun.R

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LandingScreen(
    onGetStarted: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val features = listOf(
        Triple(Icons.AutoMirrored.Rounded.VolumeOff, "Automatic Silence",
            "Phone silences precisely at Adhan time and restores when prayer ends."),
        Triple(Icons.Rounded.LocationOn, "Location-Aware",
            "Prayer times calculated from your exact location, always accurate."),
        Triple(Icons.Rounded.Timer, "Precise Scheduling",
            "Exact alarms ensure silence triggers on time, even when the app is closed."),
        Triple(Icons.Rounded.Tune, "Fully Customizable",
            "Choose which prayers to silence and for how long. You're in control.")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 32.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 32.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Hero Icon — shared element from Splash ──
        with(sharedTransitionScope) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Sukun App Icon",
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
                text = "Pray in peace. undisturbed.",
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
                Text("Start Pray in Peace",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
            }
        }
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
