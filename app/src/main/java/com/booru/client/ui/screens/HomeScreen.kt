package com.booru.client.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.booru.client.ui.components.*
import com.booru.client.ui.theme.ClayColors
import com.booru.client.viewmodel.BooruViewModel

/**
 * Home screen composing the Island header, Core button, and Post grid.
 * All elements are constrained within safe-area bounds.
 */
@Composable
fun HomeScreen(
    viewModel: BooruViewModel,
    modifier: Modifier = Modifier
) {
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    // Safe-area insets
    val density = LocalDensity.current
    val windowInsets = WindowInsets.safeDrawing
    val topInset = with(density) { windowInsets.getTop(density).toDp() }
    val bottomInset = with(density) { windowInsets.getBottom(density).toDp() }
    val sideSafePadding = 12.dp

    // Search overlay state
    var showSearch by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClayColors.Background)
            .padding(horizontal = sideSafePadding)
    ) {
        // === LAYER 0: Ambient background texture (Skia) ===
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ClayColors.SurfaceElevated.copy(alpha = 0.3f),
                        ClayColors.Background,
                        ClayColors.BackgroundDeep.copy(alpha = 0.4f)
                    ),
                    center = Offset(size.width * 0.3f, size.height * 0.2f),
                    radius = size.width * 1.2f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ClayColors.Accent.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.8f),
                    radius = size.width * 0.7f
                )
            )
        }

        // === LAYER 1: Post Grid (full content, offset for Island overlap) ===
        // Island height ~80dp, 20% overlap = 16dp
        val gridTopPadding = 64.dp + topInset

        PostGrid(
            posts = posts,
            modifier = Modifier.fillMaxSize(),
            columns = 2,
            contentPadding = PaddingValues(
                start = 4.dp,
                end = 4.dp,
                top = gridTopPadding,
                bottom = 110.dp
            ),
            isLoadingMore = isLoadingMore,
            onLoadMore = { viewModel.loadNextPage() },
            onPostClick = { }
        )

        // === LAYER 2: Loading overlay ===
        AnimatedVisibility(
            visible = isLoading && posts.isEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ClayColors.Background.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = ClayColors.Primary.copy(alpha = 0.5f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scraping...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ClayColors.TextTertiary
                    )
                }
            }
        }

        // === LAYER 3: Island Header (floating, overlaps grid by 20%) ===
        Box(
            modifier = Modifier.padding(top = topInset)
        ) {
            IslandHeader(
                title = "Booru",
                subtitle = if (searchQuery.isBlank()) "Discover" else "\"$searchQuery\"",
                onTitleClick = { showSearch = !showSearch },
                onSubtitleClick = { showSearch = !showSearch },
                content = {
                    IslandTag(text = "Safe", onClick = { viewModel.search("rating:safe") })
                    IslandTag(text = "Popular", onClick = { viewModel.search("sort:score") })
                }
            )
        }

        // === LAYER 4: Core Button (bottom-center) ===
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp + bottomInset)
        ) {
            CoreButton(
                label = "Search",
                onClick = { showSearch = !showSearch }
            )
        }

        // === LAYER 5: Search overlay ===
        AnimatedVisibility(
            visible = showSearch,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ClayColors.TextPrimary.copy(alpha = 0.25f))
                    .clickable(onClick = { showSearch = false })
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                ClaySearchCard(
                    initialText = searchQuery,
                    onSearch = { query ->
                        viewModel.search(query)
                        showSearch = false
                    },
                    onDismiss = { showSearch = false }
                )
            }
        }
    }
}

/**
 * Clay-styled search input card.
 */
@Composable
private fun ClaySearchCard(
    initialText: String,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = false) { } // Consume clicks
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val c = 24.dp.toPx()

            drawRoundRect(
                color = ClayColors.ShadowDark.copy(alpha = 0.6f),
                topLeft = Offset(4.dp.toPx(), 6.dp.toPx()),
                size = Size(w, h),
                cornerRadius = CornerRadius(c)
            )
            drawRoundRect(
                color = ClayColors.Surface,
                size = Size(w, h),
                cornerRadius = CornerRadius(c)
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ClayColors.InnerGlow.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = h * 0.3f
                ),
                size = Size(w, h),
                cornerRadius = CornerRadius(c)
            )
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Search Tags",
                style = MaterialTheme.typography.titleMedium,
                color = ClayColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text("e.g. blue_sky landscape", color = ClayColors.TextTertiary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = ClayColors.SurfaceElevated,
                    unfocusedContainerColor = ClayColors.SurfaceElevated,
                    focusedIndicatorColor = ClayColors.Primary.copy(alpha = 0.3f),
                    unfocusedIndicatorColor = ClayColors.TextTertiary.copy(alpha = 0.2f),
                    cursorColor = ClayColors.Primary
                ),
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = ClayColors.TextTertiary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onSearch(text.trim()) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClayColors.Primary,
                        contentColor = ClayColors.TextOnPrimary
                    )
                ) {
                    Text("Scrape")
                }
            }
        }
    }
}
