package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.ui.theme.AmberPrimaryLight
import com.example.ui.theme.GoldTertiaryLight
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.TealSecondaryLight
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Particle(
    val initialX: Float,
    val initialY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val color: Color,
    val size: Float,
    val rotationSpeed: Float,
    val isCircle: Boolean
)

/**
 * A delightful, lightweight celebratory particle explosion burst across the screen.
 */
@Composable
fun CelebrationParticleOverlay(
    trigger: Long,
    onComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (trigger <= 0L) return

    val progress = remember(trigger) { Animatable(0f) }

    val particles = remember(trigger) {
        val colors = listOf(
            AmberPrimaryLight,
            GoldTertiaryLight,
            TealSecondaryLight,
            StatusOptimal,
            Color(0xFFFFB300),
            Color(0xFFFF8A65),
            Color(0xFF81D4FA)
        )
        List(60) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 700f + 350f
            Particle(
                initialX = 0.5f,
                initialY = 0.45f,
                velocityX = cos(angle) * speed,
                velocityY = sin(angle) * speed - 200f, // Initial upward burst
                color = colors[it % colors.size],
                size = Random.nextFloat() * 12f + 6f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                isCircle = Random.nextBoolean()
            )
        }
    }

    LaunchedEffect(trigger) {
        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1800, easing = LinearEasing)
            )
            onComplete()
        }
    }

    if (progress.value < 1f) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val t = progress.value
            val gravity = 900f * t * t // Accelerating gravity

            particles.forEach { p ->
                val x = (size.width * p.initialX) + (p.velocityX * t)
                val y = (size.height * p.initialY) + (p.velocityY * t) + gravity

                if (x in 0f..size.width && y in 0f..size.height) {
                    val alpha = (1f - (t * 1.15f)).coerceIn(0f, 1f)
                    val particleColor = p.color.copy(alpha = alpha)

                    rotate(degrees = p.rotationSpeed * t, pivot = Offset(x, y)) {
                        if (p.isCircle) {
                            drawCircle(
                                color = particleColor,
                                radius = p.size / 2f,
                                center = Offset(x, y)
                            )
                        } else {
                            drawRect(
                                color = particleColor,
                                topLeft = Offset(x - p.size / 2f, y - p.size / 3f),
                                size = Size(p.size, p.size * 0.65f)
                            )
                        }
                    }
                }
            }
        }
    }
}
