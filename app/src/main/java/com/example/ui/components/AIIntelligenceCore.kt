package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.OrbState
import com.example.ui.theme.AuraCosmicBlack
import com.example.ui.theme.AuraCrisisCoral
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraDeepNavy
import com.example.ui.theme.AuraElectricBlue
import com.example.ui.theme.AuraIceCyan
import com.example.ui.theme.AuraIceWhite
import com.example.ui.theme.AuraLavender
import com.example.ui.theme.AuraViolet
import com.example.ui.theme.AuraWarningAmber
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// 3D Point in spherical world space
private data class Point3D(val x: Float, val y: Float, val z: Float)

// 3D Orbital Node
private data class OrbitalNode(
    val ringIndex: Int,
    val initialAngle: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

// 3D Procedural Particle
private data class Particle3D(
    val u: Float,
    val v: Float,
    val distance: Float,
    val size: Float,
    val speed: Float,
    val pulsePhase: Float
)

@Composable
fun AIIntelligenceCore(
    state: OrbState,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    audioLevel: Float = 0f,
    reducedMotion: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()

    // Interactive Drag-to-Rotate yaw and pitch
    var dragYaw by remember { mutableFloatStateOf(0f) }
    var dragPitch by remember { mutableFloatStateOf(0f) }
    val animatedDragYaw = remember { Animatable(0f) }
    val animatedDragPitch = remember { Animatable(0f) }

    // Smooth audio reactivity level
    val smoothAudio by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 100),
        label = "smooth_audio"
    )

    // Continuous time loop for 3D orbital dynamics & shell respiration
    val transition = rememberInfiniteTransition(label = "3d_intelligence_transition")

    // Slow ambient 3D rotation
    val baseRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (reducedMotion) 60000 else when (state) {
                    OrbState.PROCESSING -> 7000
                    OrbState.SPEAKING -> 12000
                    OrbState.LISTENING -> 16000
                    else -> 24000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "base_rotation"
    )

    // Nucleus breathing pulsation (calm 4-7-8 grounding rhythm in CRISIS)
    val breathingPulse by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (reducedMotion) 5000 else when (state) {
                    OrbState.LISTENING -> 1400
                    OrbState.PROCESSING -> 750
                    OrbState.SPEAKING -> 1500
                    OrbState.CRISIS -> 4000 // 4s grounding rhythm
                    OrbState.INTERRUPTED -> 600
                    else -> 2800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_pulse"
    )

    // Wave ripples radiating in SPEAKING and energy convergence in PROCESSING
    val energyWaveProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (reducedMotion) 4000 else 1800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "energy_wave"
    )

    // Geodesic energy pulse phase
    val geodesicPulsePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "geodesic_pulse"
    )

    // Pre-computed orbital nodes
    val orbitalNodes = remember {
        listOf(
            OrbitalNode(0, 0f, 1.0f, 3.2f, AuraIceCyan),
            OrbitalNode(0, 180f, 1.0f, 2.6f, AuraIceWhite),
            OrbitalNode(1, 45f, -0.75f, 3.0f, AuraViolet),
            OrbitalNode(1, 225f, -0.75f, 2.8f, AuraLavender),
            OrbitalNode(2, 90f, 1.35f, 2.5f, AuraCyanGlow)
        )
    }

    // Pre-computed 3D procedural particles
    val particles = remember {
        List(36) { i ->
            val u = (i * 0.1745f) % (2f * PI.toFloat())
            val v = (((i * 7) % 20) - 10) / 10f
            val distance = 1.25f + ((i % 5) * 0.12f)
            val size = 1.4f + (i % 3) * 0.9f
            val speed = 0.6f + (i % 4) * 0.3f
            val pulsePhase = (i * 30f) % 360f
            Particle3D(u, v, distance, size, speed, pulsePhase)
        }
    }

    // Dynamic State Color Scheme
    val (primaryColor, secondaryColor, coreColor, accentColor) = when (state) {
        OrbState.CRISIS -> Quadruple(AuraCrisisCoral, AuraWarningAmber, Color(0xFFFEE2E2), Color(0xFFEF4444))
        OrbState.INTERRUPTED -> Quadruple(AuraWarningAmber, AuraViolet, Color(0xFFFEF3C7), AuraIceCyan)
        OrbState.LISTENING -> Quadruple(AuraIceCyan, AuraCyanGlow, AuraIceWhite, AuraElectricBlue)
        OrbState.PROCESSING -> Quadruple(AuraLavender, AuraIceCyan, AuraIceWhite, AuraViolet)
        OrbState.SPEAKING -> Quadruple(AuraIceCyan, AuraViolet, AuraIceWhite, AuraElectricBlue)
        OrbState.IDLE -> Quadruple(AuraIceCyan, AuraViolet, AuraIceWhite, AuraLavender)
    }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragYaw += dragAmount.x * 0.5f
                        dragPitch = (dragPitch - dragAmount.y * 0.5f).coerceIn(-60f, 60f)
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            animatedDragYaw.snapTo(dragYaw)
                            animatedDragPitch.snapTo(dragPitch)
                            // Smoothly relax back to neutral alignment over time
                            launch { animatedDragYaw.animateTo(0f, tween(1500, easing = FastOutSlowInEasing)) }
                            launch { animatedDragPitch.animateTo(0f, tween(1500, easing = FastOutSlowInEasing)) }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2.7f) * breathingPulse * (1f + smoothAudio * 0.22f)

            // Combined rotation angles (pitch and yaw with subtle camera perspective)
            val effectiveYaw = Math.toRadians((baseRotation + dragYaw + animatedDragYaw.value).toDouble()).toFloat()
            val effectivePitch = Math.toRadians((22.0 + dragPitch + animatedDragPitch.value).toDouble()).toFloat()
            val cameraRoll = Math.toRadians(-15.0).toFloat()

            // 1. Ambient outer volumetric atmospheric haze
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.25f),
                        secondaryColor.copy(alpha = 0.08f),
                        AuraCosmicBlack.copy(alpha = 0.0f)
                    ),
                    center = center,
                    radius = baseRadius * 1.55f
                ),
                radius = baseRadius * 1.55f,
                center = center
            )

            // 2. Render REAR 3D elements (z < 0) before the opaque core
            drawOrbitalRings(
                center = center,
                radius = baseRadius,
                yaw = effectiveYaw,
                pitch = effectivePitch,
                roll = cameraRoll,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                renderFrontOnly = false
            )

            drawGeodesicMesh(
                center = center,
                radius = baseRadius,
                yaw = effectiveYaw,
                pitch = effectivePitch,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                pulsePhase = geodesicPulsePhase,
                renderFrontOnly = false
            )

            drawParticles3D(
                center = center,
                baseRadius = baseRadius,
                yaw = effectiveYaw,
                pitch = effectivePitch,
                particles = particles,
                baseRotation = baseRotation,
                state = state,
                audioLevel = smoothAudio,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                renderFrontOnly = false
            )

            // 3. Volumetric Spherical Core (Deep cobalt/indigo shell with Fresnel rim highlights)
            drawVolumetricCore(
                center = center,
                radius = baseRadius,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                coreColor = coreColor,
                state = state,
                audioLevel = smoothAudio,
                waveProgress = energyWaveProgress
            )

            // 4. Render FRONT 3D elements (z >= 0) in front of the core with crisp luminous depth
            drawGeodesicMesh(
                center = center,
                radius = baseRadius,
                yaw = effectiveYaw,
                pitch = effectivePitch,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                pulsePhase = geodesicPulsePhase,
                renderFrontOnly = true
            )

            drawOrbitalRings(
                center = center,
                radius = baseRadius,
                yaw = effectiveYaw,
                pitch = effectivePitch,
                roll = cameraRoll,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                renderFrontOnly = true
            )

            drawOrbitalNodes(
                center = center,
                radius = baseRadius,
                yaw = effectiveYaw,
                pitch = effectivePitch,
                roll = cameraRoll,
                nodes = orbitalNodes,
                timeAngle = baseRotation
            )

            drawParticles3D(
                center = center,
                baseRadius = baseRadius,
                yaw = effectiveYaw,
                pitch = effectivePitch,
                particles = particles,
                baseRotation = baseRotation,
                state = state,
                audioLevel = smoothAudio,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                renderFrontOnly = true
            )

            // 5. Specular refraction glint (cinematic top-left caustic)
            val glintOffset = Offset(center.x - baseRadius * 0.38f, center.y - baseRadius * 0.38f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        AuraIceWhite.copy(alpha = 0.75f),
                        primaryColor.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = glintOffset,
                    radius = baseRadius * 0.25f
                ),
                radius = baseRadius * 0.25f,
                center = glintOffset
            )
        }
    }
}

// Data holder for 4 colors
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * 3D Coordinate rotation and perspective projection
 */
private fun project3D(
    p: Point3D,
    yaw: Float,
    pitch: Float,
    roll: Float = 0f,
    focalLength: Float = 850f,
    cameraDistance: Float = 750f
): ProjectedPoint {
    // 1. Yaw rotation (Y axis)
    val cosY = cos(yaw)
    val sinY = sin(yaw)
    val x1 = p.x * cosY + p.z * sinY
    val y1 = p.y
    val z1 = -p.x * sinY + p.z * cosY

    // 2. Pitch rotation (X axis)
    val cosP = cos(pitch)
    val sinP = sin(pitch)
    val x2 = x1
    val y2 = y1 * cosP - z1 * sinP
    val z2 = y1 * sinP + z1 * cosP

    // 3. Roll rotation (Z axis)
    val cosR = cos(roll)
    val sinR = sin(roll)
    val x3 = x2 * cosR - y2 * sinR
    val y3 = x2 * sinR + y2 * cosR
    val z3 = z2

    // Perspective projection
    val zDepth = z3 + cameraDistance
    val scale = if (zDepth > 10f) focalLength / zDepth else 1f
    return ProjectedPoint(
        x = x3 * scale,
        y = y3 * scale,
        z = z3,
        scale = scale
    )
}

private data class ProjectedPoint(val x: Float, val y: Float, val z: Float, val scale: Float)

/**
 * Draw 3D Geodesic Latitude and Longitude Wireframe with depth attenuation
 */
/**
 * Draw 3D Geodesic Latitude, Longitude, and Diagonal Wireframe with dynamic energy pulses
 */
private fun DrawScope.drawGeodesicMesh(
    center: Offset,
    radius: Float,
    yaw: Float,
    pitch: Float,
    primaryColor: Color,
    secondaryColor: Color,
    pulsePhase: Float,
    renderFrontOnly: Boolean
) {
    val latitudes = floatArrayOf(-50f, -25f, 0f, 25f, 50f)
    val longitudes = floatArrayOf(0f, 45f, 90f, 135f)
    val pulseRad = Math.toRadians(pulsePhase.toDouble()).toFloat()

    // 1. Latitude circles
    latitudes.forEachIndexed { index, latDeg ->
        val latRad = Math.toRadians(latDeg.toDouble()).toFloat()
        val rLat = radius * cos(latRad)
        val yLat = radius * sin(latRad)

        val path = Path()
        var first = true
        val steps = 36

        for (i in 0..steps) {
            val theta = (i * 2f * PI / steps).toFloat()
            val p3 = Point3D(rLat * cos(theta), yLat, rLat * sin(theta))
            val proj = project3D(p3, yaw, pitch)

            val isFront = proj.z >= -radius * 0.15f
            if (isFront == renderFrontOnly) {
                val screenX = center.x + proj.x
                val screenY = center.y + proj.y

                if (first) {
                    path.moveTo(screenX, screenY)
                    first = false
                } else {
                    path.lineTo(screenX, screenY)
                }
            }
        }

        val isEquator = latDeg == 0f
        val pulseIntensity = ((sin(pulseRad + index * 0.9f) + 1f) / 2f) * 0.25f
        val alpha = if (renderFrontOnly) {
            (if (isEquator) 0.65f else 0.38f) + pulseIntensity
        } else {
            0.10f
        }
        val strokeWidth = if (renderFrontOnly) {
            if (isEquator) 1.8f else 1.1f
        } else {
            0.7f
        }

        drawPath(
            path = path,
            color = if (index % 2 == 0) primaryColor.copy(alpha = alpha.coerceIn(0f, 1f)) else secondaryColor.copy(alpha = alpha.coerceIn(0f, 1f)),
            style = Stroke(width = strokeWidth)
        )
    }

    // 2. Longitude meridians
    longitudes.forEachIndexed { lonIdx, lonDeg ->
        val lonRad = Math.toRadians(lonDeg.toDouble()).toFloat()
        val path = Path()
        var first = true
        val steps = 36

        for (i in 0..steps) {
            val phi = (i * 2f * PI / steps).toFloat()
            val px = radius * sin(phi) * cos(lonRad)
            val py = radius * cos(phi)
            val pz = radius * sin(phi) * sin(lonRad)

            val proj = project3D(Point3D(px, py, pz), yaw, pitch)
            val isFront = proj.z >= -radius * 0.15f

            if (isFront == renderFrontOnly) {
                val screenX = center.x + proj.x
                val screenY = center.y + proj.y

                if (first) {
                    path.moveTo(screenX, screenY)
                    first = false
                } else {
                    path.lineTo(screenX, screenY)
                }
            }
        }

        val lonPulse = ((sin(pulseRad + lonIdx * 1.2f) + 1f) / 2f) * 0.20f
        val alpha = if (renderFrontOnly) 0.35f + lonPulse else 0.10f
        drawPath(
            path = path,
            color = primaryColor.copy(alpha = alpha.coerceIn(0f, 1f)),
            style = Stroke(width = if (renderFrontOnly) 1.2f else 0.7f)
        )
    }

    // 3. Diagonal Geodesic Cross-Struts (adds realistic holographic geodesic structure)
    if (renderFrontOnly) {
        val strutAngles = floatArrayOf(20f, 65f, 110f, 155f, 200f, 245f, 290f, 335f)
        strutAngles.forEachIndexed { sIdx, angDeg ->
            val angRad1 = Math.toRadians(angDeg.toDouble()).toFloat()
            val angRad2 = Math.toRadians((angDeg + 30f).toDouble()).toFloat()

            val pTop = Point3D(radius * 0.85f * cos(angRad1), -radius * 0.40f, radius * 0.85f * sin(angRad1))
            val pBot = Point3D(radius * 0.85f * cos(angRad2), radius * 0.40f, radius * 0.85f * sin(angRad2))

            val proj1 = project3D(pTop, yaw, pitch)
            val proj2 = project3D(pBot, yaw, pitch)

            if (proj1.z >= -radius * 0.15f && proj2.z >= -radius * 0.15f) {
                val pulse = ((sin(pulseRad + sIdx * 0.8f) + 1f) / 2f)
                val alpha = (0.15f + pulse * 0.30f).coerceIn(0.1f, 0.7f)
                drawLine(
                    color = if (sIdx % 2 == 0) primaryColor.copy(alpha = alpha) else secondaryColor.copy(alpha = alpha),
                    start = Offset(center.x + proj1.x, center.y + proj1.y),
                    end = Offset(center.x + proj2.x, center.y + proj2.y),
                    strokeWidth = 0.9f
                )
            }
        }
    }
}

/**
 * Draw 3D Tilted Elliptical Orbital Rings
 */
private fun DrawScope.drawOrbitalRings(
    center: Offset,
    radius: Float,
    yaw: Float,
    pitch: Float,
    roll: Float,
    primaryColor: Color,
    secondaryColor: Color,
    renderFrontOnly: Boolean
) {
    // Ring specifications: (radiusScale, pitchOffset, rollOffset, color)
    val ringSpecs = listOf(
        Triple(1.36f, 0.45f, -0.60f),
        Triple(1.58f, -0.65f, 0.80f),
        Triple(1.22f, 0.10f, 0.25f)
    )

    ringSpecs.forEachIndexed { idx, spec ->
        val ringR = radius * spec.first
        val path = Path()
        var first = true
        val steps = 48

        for (i in 0..steps) {
            val theta = (i * 2f * PI / steps).toFloat()
            // Point in ring local plane
            val lx = ringR * cos(theta)
            val ly = 0f
            val lz = ringR * sin(theta)

            // Rotate into orbital tilt
            val p3 = Point3D(lx, ly, lz)
            val proj = project3D(p3, yaw, pitch + spec.second, roll + spec.third)

            val isFront = proj.z >= -radius * 0.2f
            if (isFront == renderFrontOnly) {
                val screenX = center.x + proj.x
                val screenY = center.y + proj.y

                if (first) {
                    path.moveTo(screenX, screenY)
                    first = false
                } else {
                    path.lineTo(screenX, screenY)
                }
            }
        }

        val alpha = if (renderFrontOnly) 0.55f else 0.14f
        val strokeW = if (renderFrontOnly) 1.6f else 0.9f
        val color = if (idx % 2 == 0) primaryColor else secondaryColor

        drawPath(
            path = path,
            color = color.copy(alpha = alpha),
            style = Stroke(width = strokeW)
        )
    }
}

/**
 * Draw moving 3D luminous orbital nodes
 */
private fun DrawScope.drawOrbitalNodes(
    center: Offset,
    radius: Float,
    yaw: Float,
    pitch: Float,
    roll: Float,
    nodes: List<OrbitalNode>,
    timeAngle: Float
) {
    val ringScales = floatArrayOf(1.36f, 1.58f, 1.22f)
    val ringTilts = listOf(
        Pair(0.45f, -0.60f),
        Pair(-0.65f, 0.80f),
        Pair(0.10f, 0.25f)
    )

    nodes.forEach { node ->
        val ringR = radius * ringScales[node.ringIndex]
        val tilt = ringTilts[node.ringIndex]

        val currentAngleRad = Math.toRadians((node.initialAngle + timeAngle * node.speed).toDouble()).toFloat()
        val lx = ringR * cos(currentAngleRad)
        val lz = ringR * sin(currentAngleRad)

        val proj = project3D(Point3D(lx, 0f, lz), yaw, pitch + tilt.first, roll + tilt.second)

        // Only draw node if in front of sphere or partially visible at edge
        if (proj.z >= -radius * 0.25f) {
            val screenPos = Offset(center.x + proj.x, center.y + proj.y)
            val nodeSize = node.size * (proj.scale * 1.1f).coerceIn(0.6f, 1.8f)

            // Luminous bloom glow
            drawCircle(
                color = node.color.copy(alpha = 0.45f),
                radius = nodeSize * 2.8f,
                center = screenPos
            )
            // Crisp high-intensity node core
            drawCircle(
                color = AuraIceWhite,
                radius = nodeSize,
                center = screenPos
            )
        }
    }
}

/**
 * Draw 3D Procedural Particles with state-driven dynamics and depth attenuation
 */
private fun DrawScope.drawParticles3D(
    center: Offset,
    baseRadius: Float,
    yaw: Float,
    pitch: Float,
    particles: List<Particle3D>,
    baseRotation: Float,
    state: OrbState,
    audioLevel: Float,
    primaryColor: Color,
    secondaryColor: Color,
    renderFrontOnly: Boolean
) {
    particles.forEach { p ->
        val speedMultiplier = when (state) {
            OrbState.PROCESSING -> 2.6f
            OrbState.SPEAKING -> 1.5f
            OrbState.INTERRUPTED -> 3.0f
            else -> 1.0f
        }
        val currentU = p.u + Math.toRadians((baseRotation * p.speed * speedMultiplier).toDouble()).toFloat()

        // State-driven particle distances:
        // LISTENING: Small particles travel inward toward the sphere
        // PROCESSING: Energy traces converge tightly toward the core
        // SPEAKING: Particles radiate subtly in sync with speech amplitude
        // INTERRUPTED: Sharp inward snap
        val stateDistFactor = when (state) {
            OrbState.LISTENING -> p.distance * 0.88f - (audioLevel * 0.12f)
            OrbState.PROCESSING -> p.distance * 0.78f
            OrbState.INTERRUPTED -> p.distance * 0.72f
            OrbState.SPEAKING -> p.distance + (audioLevel * 0.22f)
            else -> p.distance
        }
        val r = baseRadius * stateDistFactor

        val cosV = sqrt((1f - p.v * p.v).coerceAtLeast(0f))
        val px = r * cosV * cos(currentU)
        val py = r * p.v
        val pz = r * cosV * sin(currentU)

        val proj = project3D(Point3D(px, py, pz), yaw, pitch)
        val isFront = proj.z >= -baseRadius * 0.2f

        if (isFront == renderFrontOnly) {
            val screenPos = Offset(center.x + proj.x, center.y + proj.y)
            val depthAlpha = if (isFront) {
                ((proj.z + baseRadius) / (2f * baseRadius)).coerceIn(0.25f, 0.85f)
            } else {
                0.12f
            }
            val pSize = p.size * (proj.scale * 1.05f).coerceIn(0.6f, 1.6f)

            drawCircle(
                color = if (p.pulsePhase > 180f) primaryColor.copy(alpha = depthAlpha) else secondaryColor.copy(alpha = depthAlpha),
                radius = pSize,
                center = screenPos
            )
        }
    }
}

/**
 * Draw Volumetric Spherical Core with Fresnel Rim Highlights & Cyan-White Nucleus
 */
private fun DrawScope.drawVolumetricCore(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    secondaryColor: Color,
    coreColor: Color,
    state: OrbState,
    audioLevel: Float,
    waveProgress: Float
) {
    // 1. Outer Spherical Shell with Fresnel Rim Lighting (dark cobalt interior, bright glowing rim)
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color(0xFF071126).copy(alpha = 0.88f), // Deep cobalt/indigo interior
            0.65f to Color(0xFF0A1838).copy(alpha = 0.92f),
            0.88f to primaryColor.copy(alpha = 0.65f),      // Fresnel cyan glow rim
            0.98f to secondaryColor.copy(alpha = 0.85f),    // Rim highlight
            1.0f to Color.Transparent,
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    // 2. Active acoustic ripples in SPEAKING and LISTENING states
    if (state == OrbState.SPEAKING || state == OrbState.LISTENING) {
        val rippleRadius = radius * (0.45f + waveProgress * 0.55f)
        val rippleAlpha = (1f - waveProgress) * (0.55f + audioLevel * 0.35f)
        drawCircle(
            color = primaryColor.copy(alpha = rippleAlpha.coerceIn(0f, 1f)),
            radius = rippleRadius,
            center = center,
            style = Stroke(width = 1.8f)
        )
    }

    // 3. Concentrated Cyan-White Energy Nucleus
    val nucleusRadius = radius * (0.35f + audioLevel * 0.08f)
    drawCircle(
        brush = Brush.radialGradient(
            0.0f to Color.White.copy(alpha = 0.95f),
            0.35f to coreColor.copy(alpha = 0.90f),
            0.70f to primaryColor.copy(alpha = 0.65f),
            1.0f to Color.Transparent,
            center = center,
            radius = nucleusRadius
        ),
        radius = nucleusRadius,
        center = center
    )

    // 4. Inner heart nucleus light
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = nucleusRadius * 0.35f,
        center = center
    )
}
