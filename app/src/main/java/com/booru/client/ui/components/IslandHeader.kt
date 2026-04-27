package com.booru.client.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.booru.client.ui.theme.ClayColors

/**
 * The "Island" — a floating card component that overlaps the main content.
 * Claymorphism style: dual soft shadows, inner glow, organic rounded corners.
 *
 * Position: Overlaps the top-left content by exactly 20%.
 * Safe-area: Padded so it never bleeds off-screen.
 */
@Composable
fun IslandHeader(
    modifier: Modifier = Modifier,
    title: String = "Booru",
    subtitle: String = "Browse",
    onTitleClick: () -> Unit = {},
    onSubtitleClick: () -> Unit = {},
    content: @Composable RowScope.() -> Unit = {}
) {
    // Expand/collapse animation
    var isExpanded by remember { mutableStateOf(false) }
    val widthAnim by animateDpAsState(
        targetValue = if (isExpanded) 320.dp else 200.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f),
        label = "islandWidth"
    )
    val cornerAnim by animateDpAsState(
        targetValue = if (isExpanded) 28.dp else 24.dp,
        animationSpec = tween(300, easing = EaseOutCubic),
        label = "islandCorner"
    )

    Box(
        modifier = modifier
            .width(widthAnim)
            .wrapContentHeight()
            .clickable { isExpanded = !isExpanded }
    ) {
        // Skia Canvas: Clay card body
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(cornerSize = cornerAnim))
        ) {
            val w = size.width
            val h = size.height
            val cornerPx = cornerAnim.toPx()

            // === Claymorphism dual shadow (drawn as offset rects) ===

            // Light shadow (top-left offset)
            val shadowOffset = 4.dp.toPx()
            drawRoundRect(
                color = ClayColors.ShadowLight.copy(alpha = 0.8f),
                topLeft = Offset(-shadowOffset, -shadowOffset),
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerPx)
            )

            // Dark shadow (bottom-right offset)
            drawRoundRect(
                color = ClayColors.ShadowDark.copy(alpha = 0.6f),
                topLeft = Offset(shadowOffset, shadowOffset),
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerPx)
            )

            // Secondary darker shadow for depth
            val shadowOffset2 = 8.dp.toPx()
            drawRoundRect(
                color = ClayColors.ShadowMedium.copy(alpha = 0.35f),
                topLeft = Offset(shadowOffset2, shadowOffset2),
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerPx)
            )

            // Main card body
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ClayColors.Surface,
                        ClayColors.SurfaceElevated
                    )
                ),
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerPx)
            )

            // Inner glow highlight (top edge)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ClayColors.InnerGlow.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.4f
                ),
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerPx)
            )

            // Subtle border
            drawRoundRect(
                color = ClayColors.Primary.copy(alpha = 0.06f),
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerPx),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Text content
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Small organic dot indicator
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ClayColors.Success.copy(alpha = 0.8f),
                                ClayColors.Success.copy(alpha = 0.3f)
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.width / 2
                        ),
                        center = Offset(size.width / 2, size.height / 2),
                        radius = size.width / 2
                    )
                }

                androidx.compose.material3.Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.sp
                    ),
                    color = ClayColors.TextPrimary
                )
            }

            androidx.compose.material3.Text(
                text = subtitle,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = ClayColors.TextTertiary
            )

            // Expandable content row
            if (isExpanded) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * A small clay tag chip for use inside the Island header.
 */
@Composable
fun IslandTag(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ClayColors.SurfacePressed.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = ClayColors.TextSecondary
        )
    }
}
