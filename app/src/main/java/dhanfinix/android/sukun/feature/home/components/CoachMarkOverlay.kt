package dhanfinix.android.sukun.feature.home.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

enum class CoachMarkTarget {
    PRAYER_TOGGLES,
    NEXT_PRAYER,
    VOLUME_SLIDER,
    MANUAL_SILENCE_FAB
}

data class CoachMarkStep(
    val target: CoachMarkTarget,
    val title: String,
    val description: String,
    val icon: ImageVector
)

val HomeCoachMarkSteps = listOf(
    CoachMarkStep(
        target = CoachMarkTarget.PRAYER_TOGGLES,
        title = "Automatic Silencing",
        description = "Tap on any prayer tile to enable or disable automatic silencing. When enabled, your phone will mute automatically during that prayer time.",
        icon = Icons.Rounded.NotificationsOff
    ),
    CoachMarkStep(
        target = CoachMarkTarget.NEXT_PRAYER,
        title = "Accurate Timings",
        description = "Keep your location updated in the Next Prayer card to ensure the countdowns and prayer times are always precise.",
        icon = Icons.Rounded.LocationOn
    ),
    CoachMarkStep(
        target = CoachMarkTarget.VOLUME_SLIDER,
        title = "Manual Controls",
        description = "Need to quickly adjust the volume without waiting for a prayer? Use the slider at the bottom as a handy add-on tool.",
        icon = Icons.AutoMirrored.Rounded.VolumeUp
    ),
    CoachMarkStep(
        target = CoachMarkTarget.MANUAL_SILENCE_FAB,
        title = "Quick Silence",
        description = "In a meeting or entering a mosque right now? Tap this Floating Action Button to immediately trigger a manual silence countdown.",
        icon = Icons.AutoMirrored.Rounded.VolumeOff
    )
)

@Composable
fun CoachMarkOverlay(
    targets: Map<CoachMarkTarget, Rect>,
    onStepChange: (CoachMarkStep) -> Unit = {},
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val stepData = HomeCoachMarkSteps.getOrNull(currentStep)

    LaunchedEffect(stepData) {
        if (stepData != null) {
            onStepChange(stepData)
        }
    }

    if (stepData == null) {
        onDismiss()
        return
    }

    val targetRect = targets[stepData.target]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Consume all touch events so user must tap 'Next'
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f) // Required for BlendMode.Clear compositing
        ) {
            drawRect(Color.Black.copy(alpha = 0.8f))

            targetRect?.let { rect ->
                val padding = 16.dp.toPx()
                val radius = 24.dp.toPx()
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = rect.copy(
                                left = rect.left - padding,
                                top = rect.top - padding,
                                right = rect.right + padding,
                                bottom = rect.bottom + padding
                            ),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    )
                }
                drawPath(
                    path = path,
                    color = Color.Transparent,
                    blendMode = BlendMode.Clear
                )
            }
        }

        var cardSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
        val density = LocalDensity.current
        val config = LocalConfiguration.current
        
        val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
        val paddingPx = with(density) { 32.dp.toPx() }
        
        var yOffset by remember { mutableIntStateOf(0) }

        LaunchedEffect(targetRect, cardSize) {
            if (targetRect == null || cardSize.height == 0) return@LaunchedEffect
            
            val isTargetBottomHalf = targetRect.center.y > screenHeightPx / 2
            
            yOffset = if (isTargetBottomHalf) {
                // Place card above the target
                (targetRect.top - cardSize.height - paddingPx * 2).toInt().coerceAtLeast(paddingPx.toInt())
            } else {
                // Place card below the target
                (targetRect.bottom + paddingPx).toInt().coerceAtMost((screenHeightPx - cardSize.height - paddingPx).toInt())
            }
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .offset { IntOffset(0, yOffset) }
                .onGloballyPositioned { cardSize = it.size },
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    AnimatedContent(
                        targetState = stepData,
                        transitionSpec = {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                        },
                        label = "coachmark_anim"
                    ) { step ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = step.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Step indicators
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            HomeCoachMarkSteps.indices.forEach { index ->
                                val isSelected = index == currentStep
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 8.dp else 6.dp)
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (currentStep < HomeCoachMarkSteps.size - 1) {
                                    currentStep++
                                } else {
                                    onDismiss()
                                }
                            }
                        ) {
                            Text(if (currentStep < HomeCoachMarkSteps.size - 1) "Next" else "Got it!")
                        }
                    }
                }
            }
        }
    }
