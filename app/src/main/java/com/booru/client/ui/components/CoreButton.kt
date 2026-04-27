package com.booru.client.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.booru.client.ui.theme.ClayColors
import kotlinx.coroutines.delay

/**
 * Large centered circular "Core" button for primary actions.
 * Features:
 * - Claymorphism shadow (dual-layer: light top-left, dark bottom-right)
 * - Inner glow on press
 * - Organic pulse animation when idle
 * - Skia-backed Canvas for custom drawing of shadows and glow rings
 */
@Composable
fun CoreButton(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = { CoreSearchIcon() },
    label: String = "Search",
    onClick: () -> Unit = {}
) {
    var isPressed by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "corePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "pressScale"
    )

    val coreSize = 88.dp
    val totalSize = 120.dp // includes shadow spread

    Box(
        modifier = modifier
            .size(totalSize)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Skia Canvas: outer ambient glow ring
        Canvas(
            modifier = Modifier
                .size(coreSize * pulseScale * pressScale)
                .clip(CircleShape)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2

            // Outer soft glow (clay ambient)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ClayColors.ActiveGlow.copy(alpha = glowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 1.5f
                ),
                center = center,
                radius = maxRadius * 1.5f
            )

            // Main clay body
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        ClayColors.Surface,
                        ClayColors.SurfaceElevated
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                center = center,
                radius = maxRadius
            )

            // Inner glow highlight (top-left)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ClayColors.InnerGlow.copy(alpha = 0.6f),
                        Color.Transparent
                    ),
                    center = Offset(maxRadius * 0.6f, maxRadius * 0.6f),
                    radius = maxRadius * 0.7f
                ),
                center = Offset(maxRadius * 0.6f, maxRadius * 0.6f),
                radius = maxRadius * 0.7f
            )

            // Press state: inner shadow
            if (isPressed) {
                drawCircle(
                    color = ClayColors.PressedShadow.copy(alpha = 0.3f),
                    center = center,
                    radius = maxRadius
                )
            }

            // Subtle border ring
            drawCircle(
                color = ClayColors.Primary.copy(alpha = 0.08f),
                center = center,
                radius = maxRadius - 1.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Icon and label
        Column(
            modifier = Modifier.size(coreSize * pulseScale * pressScale),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            androidx.compose.material3.Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                color = ClayColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Default search magnifier icon drawn with Skia Canvas.
 */
@Composable
fun CoreSearchIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val strokeWidth = 2.5.dp.toPx()
        val pad = 4.dp.toPx()

        // Glass circle
        drawCircle(
            color = ClayColors.Primary,
            center = Offset(size.width / 2, size.height / 2 - 2.dp.toPx()),
            radius = (size.minDimension - pad * 2) / 2.8f,
            style = Stroke(width = strokeWidth)
        )

        // Handle line
        val angle = Math.toRadians(45.0)
        val cx = size.width / 2 + (size.minDimension - pad * 2) / 2.8f * kotlin.math.cos(angle).toFloat() * 0.7f
        val cy = (size.height / 2 - 2.dp.toPx()) + (size.minDimension - pad * 2) / 2.8f * kotlin.math.sin(angle).toFloat() * 0.7f
        drawLine(
            color = ClayColors.Primary,
            start = Offset(cx, cy),
            end = Offset(cx + 6.dp.toPx(), cy + 6.dp.toPx()),
            strokeWidth = strokeWidth,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
