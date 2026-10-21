package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.OrbState
import com.example.ui.theme.AuraCosmicBlack
import com.example.ui.theme.AuraCrisisCoral
import com.example.ui.theme.AuraCyanBright
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraIceCyan
import com.example.ui.theme.AuraLavender
import com.example.ui.theme.AuraViolet
import com.example.ui.theme.AuraWarningAmber
import kotlin.math.cos
import kotlin.math.sin

private data class Particle(
    val baseAngle: Float,
    val distanceRatio: Float,
    val size: Float,
    val speedFactor: Float,
    val alpha: Float
)

@Composable
fun HolographicOrb(
    state: OrbState,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    audioLevel: Float = 0f
) {
    val transition = rememberInfiniteTransition(label = "orb_animation")

    // Slow rotation for rings
    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.PROCESSING -> 3500
                    OrbState.SPEAKING -> 6000
                    OrbState.LISTENING -> 8000
                    else -> 12000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse
    val breathingPulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbState.LISTENING -> 1400
                    OrbState.PROCESSING -> 900
                    OrbState.SPEAKING -> 1800
                    OrbState.CRISIS -> 3200
                    else -> 2800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Secondary ripple
    val rippleProgress by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    // Audio level smoothing
    val smoothAudio by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = tween(120),
        label = "smooth_audio"
    )

    // Fixed particle positions
    val particles = remember {
        List(24) { i ->
            Particle(
                baseAngle = (i * 15f),
                distanceRatio = 0.55f + ((i % 5) * 0.08f),
                size = 1.8f + (i % 3) * 1.2f,
                speedFactor = 0.7f + ((i % 4) * 0.25f),
                alpha = 0.35f + (i % 4) * 0.18f
            )
        }
    }

    // Dynamic state palette
    val (primaryGlow, secondaryGlow, coreGlow) = when (state) {
        OrbState.CRISIS -> Triple(AuraCrisisCoral, AuraWarningAmber, Color(0xFFFEE2E2))
        OrbState.INTERRUPTED -> Triple(AuraWarningAmber, AuraViolet, Color(0xFFFEF3C7))
        OrbState.LISTENING -> Triple(AuraCyanBright, AuraIceCyan, Color(0xFFE0F2FE))
        OrbState.PROCESSING -> Triple(AuraLavender, AuraIceCyan, Color(0xFFF3E8FF))
        OrbState.SPEAKING -> Triple(AuraIceCyan, AuraViolet, Color(0xFFE0E7FF))
        OrbState.IDLE -> Triple(AuraIceCyan, AuraViolet, Color(0xFFE2E8F0))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2.3f) * breathingPulse * (1f + smoothAudio * 0.25f)

            // 1. Ambient outer aura halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.22f),
                        secondaryGlow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.35f
                ),
                radius = baseRadius * 1.35f,
                center = center
            )

            // 2. Active acoustic ripples (in SPEAKING and LISTENING states)
            if (state == OrbState.SPEAKING || state == OrbState.LISTENING) {
                val rippleRadius = baseRadius * (0.85f + rippleProgress * 0.45f)
                drawCircle(
                    color = primaryGlow.copy(alpha = (1f - rippleProgress) * 0.4f),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }

            // 3. Outer tilted concentric rings
            rotate(rotationAngle, center) {
                drawOval(
                    color = primaryGlow.copy(alpha = 0.55f),
                    topLeft = Offset(center.x - baseRadius * 0.95f, center.y - baseRadius * 0.45f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.9f, baseRadius * 0.9f),
                    style = Stroke(width = 1.8f)
                )
            }

            rotate(-rotationAngle * 0.75f + 45f, center) {
                drawOval(
                    color = secondaryGlow.copy(alpha = 0.65f),
                    topLeft = Offset(center.x - baseRadius * 0.75f, center.y - baseRadius * 0.95f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 1.5f, baseRadius * 1.9f),
                    style = Stroke(width = 1.5f)
                )
            }

            // 4. Subtle orbital particles
            particles.forEach { p ->
                val angleRad = Math.toRadians((p.baseAngle + rotationAngle * p.speedFactor).toDouble())
                val px = center.x + (baseRadius * p.distanceRatio * cos(angleRad)).toFloat()
                val py = center.y + (baseRadius * p.distanceRatio * sin(angleRad)).toFloat()
                drawCircle(
                    color = primaryGlow.copy(alpha = p.alpha),
                    radius = p.size,
                    center = Offset(px, py)
                )
            }

            // 5. Central glowing holographic sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreGlow.copy(alpha = 0.95f),
                        primaryGlow.copy(alpha = 0.80f),
                        secondaryGlow.copy(alpha = 0.45f),
                        AuraCosmicBlack.copy(alpha = 0.20f)
                    ),
                    center = Offset(center.x - baseRadius * 0.15f, center.y - baseRadius * 0.15f),
                    radius = baseRadius * 0.65f
                ),
                radius = baseRadius * 0.58f,
                center = center
            )

            // 6. Inner serene core light
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        coreGlow.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 0.28f
                ),
                radius = baseRadius * 0.28f,
                center = center
            )
        }
    }
}
