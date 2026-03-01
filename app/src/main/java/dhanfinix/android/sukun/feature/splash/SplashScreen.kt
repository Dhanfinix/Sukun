package dhanfinix.android.sukun.feature.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dhanfinix.android.sukun.R
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun SplashScreen(
    isReady: Boolean,
    onSplashFinished: () -> Unit
) {
    // ── Entry animations ──
    val logoScale = remember { Animatable(0f) }
    val logoAlpha = remember { Animatable(0f) }
    val nameAlpha = remember { Animatable(0f) }
    val dividerScale = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    // ── Infinite animations ──
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val ring1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing)),
        label = "ring1"
    )
    val ring2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, delayMillis = 1067, easing = LinearEasing)),
        label = "ring2"
    )
    val ring3 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, delayMillis = 2133, easing = LinearEasing)),
        label = "ring3"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glow"
    )

    val particleDrift by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "particles"
    )

    // ── Static particle data (stable across recompositions) ──
    val particles = remember {
        List(60) { i ->
            Triple(
                (i * 7 % 97) / 100f,   // x fraction — deterministic "random"
                (i * 13 % 97) / 100f,  // y fraction
                (1.5f + (i % 5) * 0.8f) // radius
            )
        }
    }

    // Track whether animations have finished (can we exit?)
    var animsCompleted by remember { mutableStateOf(false) }

    // ── Start animations immediately, regardless of isReady ──
    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(300))
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
        delay(200)
        nameAlpha.animateTo(1f, tween(500))
        dividerScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        delay(250)
        taglineAlpha.animateTo(1f, tween(600))
        delay(600) // hold the finished state briefly
        animsCompleted = true
    }

    // ── Only exit when BOTH animations are done AND app is ready ──
    LaunchedEffect(isReady, animsCompleted) {
        if (isReady && animsCompleted) {
            onSplashFinished()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackground = MaterialTheme.colorScheme.onBackground
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentAlignment = Alignment.Center
    ) {

        // ── Layer 1: Canvas — particles + rings + glow ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Particles
            particles.forEachIndexed { i, (fx, fy, r) ->
                val drift = sin((particleDrift + i * 0.05f) * 2f * Math.PI.toFloat()) * 12f
                val px = fx * size.width
                val py = fy * size.height + drift
                val particleAlpha = 0.08f + (i % 7) * 0.025f
                drawCircle(
                    color = primaryColor.copy(alpha = particleAlpha),
                    radius = r,
                    center = Offset(px, py)
                )
            }

            // Radial ambient glow beneath logo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = glowAlpha * 0.45f),
                        primaryColor.copy(alpha = glowAlpha * 0.1f),
                        Color.Transparent
                    ),
                    radius = 320f,
                    center = Offset(cx, cy)
                ),
                radius = 320f,
                center = Offset(cx, cy)
            )

            // Pulsing concentric rings (silence sound-wave metaphor)
            listOf(ring1, ring2, ring3).forEachIndexed { idx, progress ->
                val maxRadius = size.width * 0.52f
                val radius = progress * maxRadius
                // Alpha: 0.5 = at 0 progress → 0 at full expansion
                val alpha = ((1f - progress) * 0.55f).coerceIn(0f, 1f)
                val strokeWidth = 1.5f + (1f - progress) * 1.5f
                drawCircle(
                    color = primaryColor.copy(alpha = alpha),
                    radius = radius.coerceAtLeast(1f),
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeWidth)
                )
            }
        }

        // ── Layer 2: Content ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo with spring bounce
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Sukun Logo",
                modifier = Modifier
                    .size(148.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App name — wide letter spacing, bold reveal
            Text(
                text = "Sukun",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 14.sp,
                color = onBackground,
                modifier = Modifier
                    .alpha(nameAlpha.value)
                    .padding(start = 14.dp) // compensate for letter-spacing indent
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Animated divider line — grows from center
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .scale(scaleX = dividerScale.value, scaleY = 1f)
                    .height(1.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                primaryColor.copy(alpha = 0.6f),
                                primaryColor.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tagline
            Text(
                text = "Pray in peace, undisturbed.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(taglineAlpha.value)
                    .padding(horizontal = 48.dp)
            )
        }
    }
}
