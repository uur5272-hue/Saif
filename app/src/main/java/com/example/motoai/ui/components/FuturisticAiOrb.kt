package com.example.motoai.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MotoCyan
import com.example.ui.theme.MotoGreen
import com.example.ui.theme.MotoPurpleGlow
import com.example.ui.theme.MotoRed
import com.example.ui.theme.MotoViolet
import com.example.motoai.voice.MicState
import kotlin.math.sin

/**
 * High-performance futuristic animated orb & AI robot core.
 * Features rotating holographic rings, dynamic audio reactive pulsation,
 * and status glow.
 */
@Composable
fun FuturisticAiOrb(
    micState: MicState,
    soundLevel: Float,
    isThinking: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RingRotation"
    )

    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "CounterRotation"
    )

    // Dynamic color depending on state
    val (primaryGlow, secondaryGlow) = when {
        micState == MicState.LISTENING -> Pair(MotoGreen, MotoCyan)
        isThinking -> Pair(MotoPurpleGlow, MotoViolet)
        isSpeaking -> Pair(MotoCyan, MotoViolet)
        micState == MicState.ERROR -> Pair(MotoRed, MotoViolet)
        else -> Pair(MotoCyan, MotoViolet)
    }

    val dynamicSoundBoost = if (micState == MicState.LISTENING) soundLevel * 0.25f else 0f
    val currentScale = (pulseScale + dynamicSoundBoost).coerceIn(0.85f, 1.4f)

    Box(
        modifier = modifier
            .size(240.dp)
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag("moto_ai_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = (size.minDimension / 2f) * 0.75f * currentScale

            // Outer ethereal radial glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.45f),
                        secondaryGlow.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.35f
                ),
                radius = baseRadius * 1.35f,
                center = center
            )

            // Inner Orb core gradient
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        primaryGlow,
                        secondaryGlow,
                        Color(0xFF090D16)
                    ),
                    center = center,
                    radius = baseRadius
                ),
                radius = baseRadius * 0.78f,
                center = center
            )

            // Rotating Cybernetic Ring 1
            rotate(ringRotation, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(primaryGlow, Color.Transparent, secondaryGlow, Color.Transparent, primaryGlow)
                    ),
                    radius = baseRadius * 0.92f,
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Counter-rotating Tech Ring 2
            rotate(counterRotation, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(secondaryGlow, Color.Transparent, primaryGlow, Color.Transparent, secondaryGlow)
                    ),
                    radius = baseRadius * 1.05f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Center Robot / Assistant Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF0D1424).copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            when {
                micState == MicState.LISTENING -> {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Listening",
                        tint = MotoGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
                micState == MicState.ERROR -> {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Mic Inactive",
                        tint = MotoRed,
                        modifier = Modifier.size(36.dp)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "MOTO AI Robot Core",
                        tint = primaryGlow,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }
    }
}
