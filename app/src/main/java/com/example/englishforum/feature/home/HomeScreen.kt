package com.example.englishforum.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.englishforum.R
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.ui.components.card.ForumContentCard
import com.example.englishforum.core.ui.components.card.ForumContentCardPlaceholder
import com.example.englishforum.core.ui.toLabelResId
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import kotlin.math.abs

private const val HOME_POST_BODY_MAX_LINES = 6
private val HOME_POST_AVATAR_SIZE = 44.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onPostClick: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {},
    onMoreActionsClick: (String) -> Unit = {}
) {
    val appContainer = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        factory = remember(appContainer) { HomeViewModelFactory(appContainer.homeRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    HomeContent(
        modifier = modifier,
        uiState = uiState,
        onFilterSelected = viewModel::onFilterSelected,
        onUpvote = viewModel::onUpvote,
        onDownvote = viewModel::onDownvote,
        onPostClick = onPostClick,
        onCommentClick = onCommentClick,
        onMoreActionsClick = onMoreActionsClick
    )
}

@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    onFilterSelected: (HomeFeedFilter) -> Unit,
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onMoreActionsClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.availableFilters.isNotEmpty()) {
            item {
                HomeFilterChips(
                    filters = uiState.availableFilters,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = onFilterSelected
                )
            }
        }

        when {
            uiState.isLoading -> {
                items(3) {
                    ForumContentCardPlaceholder(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            uiState.posts.isEmpty() -> {
                item {
                    HomeEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp)
                    )
                }
            }

            else -> {
                items(
                    items = uiState.posts,
                    key = { it.id }
                ) { post ->
                    val tagLabel = stringResource(post.tag.toLabelResId())
                    ForumContentCard(
                        modifier = Modifier.fillMaxWidth(),
                        meta = stringResource(
                            R.string.home_post_meta_with_tag,
                            post.authorName,
                            post.relativeTimeText,
                            tagLabel
                        ),
                        voteCount = post.voteCount,
                        title = null,
                        body = null,
                        voteState = post.voteState,
                        commentCount = post.commentCount,
                        onCardClick = { onPostClick(post.id) },
                        onCommentClick = { onCommentClick(post.id) },
                        onUpvoteClick = { onUpvote(post.id) },
                        onDownvoteClick = { onDownvote(post.id) },
                        onMoreActionsClick = { onMoreActionsClick(post.id) },
                        leadingContent = {
                            HomePostAvatar(
                                name = post.authorName,
                                modifier = Modifier.size(HOME_POST_AVATAR_SIZE)
                            )
                        },
                        headerContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = post.authorName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.weight(1f))
                                    HomePostTagLabel(label = tagLabel)
                                }
                                Text(
                                    text = post.relativeTimeText,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        bodyContent = {
                            var previousDisplayed = false
                            if (post.title.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = post.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                previousDisplayed = true
                            }
                            if (post.body.isNotBlank()) {
                                Spacer(Modifier.height(if (previousDisplayed) 4.dp else 6.dp))
                                Text(
                                    text = post.body,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = HOME_POST_BODY_MAX_LINES,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        supportingContent = {
                            if (post.previewImageUrl != null) {
                                HomePostImagePlaceholder()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeFilterChips(
    filters: List<HomeFeedFilter>,
    selectedFilter: HomeFeedFilter,
    onFilterSelected: (HomeFeedFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filters, key = { it.id }) { filter ->
            val label = stringResource(filter.labelResId())
            val isSelected = filter.id == selectedFilter.id
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = label) },
                shape = MaterialTheme.shapes.large,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@Composable
private fun HomePostAvatar(
    name: String,
    modifier: Modifier = Modifier
) {
    val palette = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    )
    val hash = name.hashCode()
    val safeHash = if (hash == Int.MIN_VALUE) 0 else abs(hash)
    val index = safeHash % palette.size
    val (containerColor, contentColor) = palette[index]
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun HomePostImagePlaceholder(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HomePostTagLabel(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HomeEmptyState(
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.home_empty_state),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@StringRes
private fun HomeFeedFilter.labelResId(): Int {
    return when (this) {
        HomeFeedFilter.Latest -> R.string.home_filter_latest
        HomeFeedFilter.Trending -> R.string.home_filter_trending
        is HomeFeedFilter.Tag -> tag.toLabelResId()
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val previewPosts = listOf(
        HomePostUi(
            id = "1",
            authorName = "Jane_Doe",
            relativeTimeText = "12 phút trước",
            title = "Cách luyện phát âm tiếng Anh abc zxy dsa asd asd as dsa sa?",
            body = "Mọi người có mẹo nào luyện phát âm hiệu quả khi không có người chỉnh lỗi không?",
            voteCount = 23,
            voteState = VoteState.NONE,
            commentCount = 8,
            tag = PostTag.Tutorial,
            previewImageUrl = "mock://preview/jane"
        ),
        HomePostUi(
            id = "2",
            authorName = "studybuddy",
            relativeTimeText = "2 giờ trước",
            title = "Looking for speaking partner",
            body = "We can practice trên Zoom 2-3h/tuần, ai quan tâm để lại comment nha!",
            voteCount = 17,
            voteState = VoteState.UPVOTED,
            commentCount = 14,
            tag = PostTag.AskQuestion
        )
    )

    EnglishForumTheme {
        HomeContent(
            uiState = HomeUiState(
                isLoading = false,
                posts = previewPosts,
                availableFilters = listOf(
                    HomeFeedFilter.Latest,
                    HomeFeedFilter.Trending,
                    HomeFeedFilter.Tag(PostTag.Tutorial),
                    HomeFeedFilter.Tag(PostTag.AskQuestion)
                ),
                selectedFilter = HomeFeedFilter.Latest
            ),
            onFilterSelected = {},
            onUpvote = {},
            onDownvote = {},
            onPostClick = {},
            onCommentClick = {},
            onMoreActionsClick = {}
        )
    }
}
