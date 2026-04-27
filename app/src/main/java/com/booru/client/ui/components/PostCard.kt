package com.booru.client.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.booru.client.data.model.PostModel
import com.booru.client.ui.theme.ClayColors

/**
 * Grid item card for a single post.
 * Claymorphism styling with soft shadows and rounded organic shapes.
 */
@Composable
fun PostCard(
    post: PostModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cardPulse")
    val subtleGlow by infiniteTransition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subtleGlow"
    )

    Box(
        modifier = modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Clay card background with Canvas
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cornerPx = 16.dp.toPx()

            // Soft shadow
            drawRoundRect(
                color = ClayColors.ShadowDark.copy(alpha = 0.5f),
                topLeft = Offset(3.dp.toPx(), 4.dp.toPx()),
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerPx)
            )

            // Main body
            drawRoundRect(
                color = ClayColors.Surface,
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerPx)
            )

            // Subtle inner glow
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ClayColors.InnerGlow.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.3f
                ),
                size = Size(w, h),
                cornerRadius = CornerRadius(cornerPx)
            )
        }

        // Image
        AsyncImage(
            model = post.previewUrl,
            contentDescription = "Post ${post.postId}",
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )

        // Bottom gradient overlay for info
        Canvas(modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(16.dp))) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        ClayColors.TextPrimary.copy(alpha = 0.6f)
                    ),
                    startY = size.height * 0.5f,
                    endY = size.height
                )
            )
        }

        // Post info at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            androidx.compose.material3.Text(
                text = "#${post.postId ?: "—"}",
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(
                    color = Color.White.copy(alpha = 0.9f)
                )
            )
            if (post.score != null && post.score > 0) {
                androidx.compose.material3.Text(
                    text = "★ ${post.score}",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = ClayColors.AccentLight
                    )
                )
            }
        }

        // Rating badge (top-right)
        post.rating?.let { rating ->
            val badgeColor = when (rating) {
                "safe" -> ClayColors.Success
                "questionable" -> ClayColors.Warning
                "explicit" -> ClayColors.Error
                else -> ClayColors.TextTertiary
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.size(6.dp)) {
                    drawCircle(color = badgeColor)
                }
            }
        }
    }
}
