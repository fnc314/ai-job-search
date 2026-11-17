package dev.fnc314.aijobtracker.frontend.core.widgets

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.fnc314.aijobtracker.frontend.core.designsystem.BrandPalette

/**
 * Brand logo rendering the supersonic Delta Glider, orbital radar arc, and AI North Star spark.
 *
 * Fully supports optional idle animations (orbit rotation, glider float, spark shimmer).
 *
 * @param modifier The layout modifier for sizing and positioning.
 * @param size Target dimension (square) for the logo.
 * @param animated Whether to animate the orbital arc, glider hover, and spark pulse.
 * @param backgroundColor Optional background fill color (null for transparent).
 */
@Composable
public fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    animated: Boolean = true,
    backgroundColor: Color? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BrandLogoTransition")

    val orbitAngle by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "orbitAngle",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val gliderHoverOffsetY by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -0.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "gliderHover",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    val sparkScale by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "sparkScale",
        )
    } else {
        androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size.minDimension
            val scale = canvasSize / 64f

            // Optional background
            backgroundColor?.let { bg ->
                drawRect(color = bg, size = this.size)
            }

            // Draw orbital compass arc
            drawOrbitalArc(scale, orbitAngle)

            // Draw glider
            drawGlider(scale, gliderHoverOffsetY)

            // Draw 4-point AI spark
            drawAiSpark(scale, sparkScale)
        }
    }
}

private fun DrawScope.drawOrbitalArc(scale: Float, angle: Float) {
    val center = Offset(32f * scale, 32f * scale)
    val radius = 24f * scale
    val arcColor = BrandPalette.GliderArcCyan.copy(alpha = 0.45f)

    drawArc(
        color = arcColor,
        startAngle = angle,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(
            width = 1.8f * scale,
            cap = StrokeCap.Round,
        ),
    )
}

private fun DrawScope.drawGlider(scale: Float, hoverRatio: Float) {
    val dy = hoverRatio * 64f * scale

    // Underside Keel (39, 21) -> (26, 42) -> (30, 39)
    val keelPath = Path().apply {
        moveTo(39f * scale, 21f * scale + dy)
        lineTo(26f * scale, 42f * scale + dy)
        lineTo(30f * scale, 39f * scale + dy)
        close()
    }
    drawPath(keelPath, BrandPalette.GliderKeelSlate)

    // Left Wing (White top illuminated) (39, 21) -> (17, 34) -> (30, 39)
    val leftWing = Path().apply {
        moveTo(39f * scale, 21f * scale + dy)
        lineTo(17f * scale, 34f * scale + dy)
        lineTo(30f * scale, 39f * scale + dy)
        close()
    }
    drawPath(leftWing, Color.White)

    // Right Wing (Slate shaded) (39, 21) -> (30, 39) -> (43, 35)
    val rightWing = Path().apply {
        moveTo(39f * scale, 21f * scale + dy)
        lineTo(30f * scale, 39f * scale + dy)
        lineTo(43f * scale, 35f * scale + dy)
        close()
    }
    drawPath(rightWing, BrandPalette.GliderWingSlate)
}

private fun DrawScope.drawAiSpark(scale: Float, sparkScale: Float) {
    val sparkCenter = Offset(46f * scale, 16f * scale)
    val baseRadius = 4f * scale * sparkScale
    val sparkColor = BrandPalette.AmberDraft

    val sparkPath = Path().apply {
        moveTo(sparkCenter.x, sparkCenter.y - baseRadius)
        cubicTo(
            sparkCenter.x, sparkCenter.y - baseRadius * 0.4f,
            sparkCenter.x + baseRadius * 0.4f, sparkCenter.y,
            sparkCenter.x + baseRadius, sparkCenter.y,
        )
        cubicTo(
            sparkCenter.x + baseRadius * 0.4f, sparkCenter.y,
            sparkCenter.x, sparkCenter.y + baseRadius * 0.4f,
            sparkCenter.x, sparkCenter.y + baseRadius,
        )
        cubicTo(
            sparkCenter.x, sparkCenter.y + baseRadius * 0.4f,
            sparkCenter.x - baseRadius * 0.4f, sparkCenter.y,
            sparkCenter.x - baseRadius, sparkCenter.y,
        )
        cubicTo(
            sparkCenter.x - baseRadius * 0.4f, sparkCenter.y,
            sparkCenter.x, sparkCenter.y - baseRadius * 0.4f,
            sparkCenter.x, sparkCenter.y - baseRadius,
        )
        close()
    }
    drawPath(sparkPath, sparkColor)
}
