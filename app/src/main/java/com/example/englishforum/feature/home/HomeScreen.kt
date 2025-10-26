package com.example.englishforum.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.englishforum.R
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.ui.components.ForumAuthorAvatar
import com.example.englishforum.core.ui.components.ForumAuthorLink
import com.example.englishforum.core.ui.components.ForumTagLabel
import com.example.englishforum.core.ui.components.card.ForumContentCardPlaceholder
import com.example.englishforum.core.ui.components.card.ForumContentCard
import com.example.englishforum.core.ui.components.image.ForumPostPreviewImage
import com.example.englishforum.core.ui.toLabelResId
import com.example.englishforum.core.ui.theme.EnglishForumTheme

private const val HOME_POST_BODY_MAX_LINES = 6
private const val HOME_PLACEHOLDER_COUNT = 3
private val HOME_POST_AVATAR_SIZE = 44.dp

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onPostClick: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {},
    onMoreActionsClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {}
) {
    val appContainer = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel(
        factory = remember(appContainer) { HomeViewModelFactory(appContainer.homeRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    HomeContent(
        modifier = modifier,
        uiState = uiState,
        onRefresh = viewModel::onRefresh,
        onFilterSelected = viewModel::onFilterSelected,
        onUpvote = viewModel::onUpvote,
        onDownvote = viewModel::onDownvote,
        onPostClick = onPostClick,
        onCommentClick = onCommentClick,
        onMoreActionsClick = onMoreActionsClick,
        onAuthorClick = onAuthorClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onFilterSelected: (HomeFeedFilter) -> Unit,
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onMoreActionsClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    val pullState = rememberPullToRefreshState()
    val feedContentState = remember(uiState.isLoading, uiState.isRefreshing, uiState.posts) {
        when {
            uiState.isLoading || uiState.isRefreshing -> HomeFeedContent.Loading
            uiState.posts.isEmpty() -> HomeFeedContent.Empty
            else -> HomeFeedContent.Data(uiState.posts)
        }
    }

    val listState = rememberLazyListState()

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullState,
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        HomeFeedList(
            state = listState,
            uiState = uiState,
            feedContentState = feedContentState,
            onFilterSelected = onFilterSelected,
            onPostClick = onPostClick,
            onCommentClick = onCommentClick,
            onUpvote = onUpvote,
            onDownvote = onDownvote,
            onMoreActionsClick = onMoreActionsClick,
            onAuthorClick = onAuthorClick
        )
    }
}

@Composable
private fun HomeFeedList(
    state: LazyListState,
    uiState: HomeUiState,
    feedContentState: HomeFeedContent,
    onFilterSelected: (HomeFeedFilter) -> Unit,
    onPostClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onMoreActionsClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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

        when (feedContentState) {
            HomeFeedContent.Loading -> {
                items(HOME_PLACEHOLDER_COUNT) {
                    ForumContentCardPlaceholder(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            HomeFeedContent.Empty -> {
                item {
                    HomeEmptyState(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp)
                    )
                }
            }

            is HomeFeedContent.Data -> {
                items(
                    items = feedContentState.posts,
                    key = { it.id }
                ) { post ->
                    val tagLabel = stringResource(post.tag.toLabelResId())
                    val authorClick = post.authorUsername?.let { username ->
                        { onAuthorClick(username) }
                    }
                    ForumContentCard(
                        modifier = Modifier.fillMaxWidth(),
                        meta = post.relativeTimeText,
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
                            ForumAuthorAvatar(
                                name = post.authorName,
                                avatarUrl = post.authorAvatarUrl,
                                modifier = Modifier.size(HOME_POST_AVATAR_SIZE)
                            )
                        },
                        headerContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ForumAuthorLink(
                                        name = post.authorName,
                                        onClick = authorClick,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(Modifier.weight(1f))
                                    ForumTagLabel(label = tagLabel)
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
                            val previewImageUrl = post.previewImageUrl
                            if (previewImageUrl != null) {
                                ForumPostPreviewImage(imageUrl = previewImageUrl)
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

private sealed interface HomeFeedContent {
    data object Loading : HomeFeedContent
    data object Empty : HomeFeedContent
    data class Data(val posts: List<HomePostUi>) : HomeFeedContent
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
                isRefreshing = false,
                posts = previewPosts,
                availableFilters = listOf(
                    HomeFeedFilter.Latest,
                    HomeFeedFilter.Trending,
                    HomeFeedFilter.Tag(PostTag.Tutorial),
                    HomeFeedFilter.Tag(PostTag.AskQuestion)
                ),
                selectedFilter = HomeFeedFilter.Latest
            ),
            onRefresh = {},
            onFilterSelected = {},
            onUpvote = {},
            onDownvote = {},
            onPostClick = {},
            onCommentClick = {},
            onMoreActionsClick = {},
            onAuthorClick = {}
        )
    }
}
