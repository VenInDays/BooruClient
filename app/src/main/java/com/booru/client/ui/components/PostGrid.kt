package com.booru.client.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import com.booru.client.data.model.PostModel
import com.booru.client.ui.theme.ClayColors

/**
 * Post grid with fluid scroll behavior and momentum dampening.
 */
@Composable
fun PostGrid(
    posts: List<PostModel>,
    modifier: Modifier = Modifier,
    columns: Int = 2,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    onPostClick: (PostModel) -> Unit = {}
) {
    val edgeResistanceConnection = remember {
        object : NestedScrollConnection {
            override suspend fun onPreFling(available: Velocity): Velocity {
                return Velocity(available.x * 0.85f, available.y * 0.85f)
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(edgeResistanceConnection),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = posts,
            key = { it.postId ?: System.identityHashCode(it) }
        ) { post ->
            PostCard(
                post = post,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem(),
                onClick = { onPostClick(post) }
            )
        }

        // Loading more indicator
        if (isLoadingMore) {
            item(span = { GridItemSpan(columns) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ClayColors.Primary.copy(alpha = 0.4f),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Trigger load more when reaching the end
        item(span = { GridItemSpan(columns) }) {
            LaunchedEffect(Unit) {
                if (posts.isNotEmpty() && !isLoadingMore) {
                    onLoadMore()
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
