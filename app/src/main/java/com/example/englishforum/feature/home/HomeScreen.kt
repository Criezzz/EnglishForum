package com.example.englishforum.feature.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onClearSearch = viewModel::onClearSearchQuery,
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
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
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
        item {
            HomeSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                onClear = onClearSearch
            )
        }

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
                        title = post.title,
                        body = post.body,
                        voteState = post.voteState,
                        commentCount = post.commentCount,
                        onCardClick = { onPostClick(post.id) },
                        onCommentClick = { onCommentClick(post.id) },
                        onUpvoteClick = { onUpvote(post.id) },
                        onDownvoteClick = { onDownvote(post.id) },
                        onMoreActionsClick = { onMoreActionsClick(post.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    TextField(
        modifier = Modifier.fillMaxWidth(),
        value = query,
        onValueChange = onQueryChange,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.home_search_placeholder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.home_clear_search_content_description)
                    )
                }
            }
        },
        placeholder = { Text(stringResource(R.string.home_search_placeholder)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = MaterialTheme.shapes.extraLarge
    )
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
            title = "Cách luyện phát âm tiếng Anh?",
            body = "Mọi người có mẹo nào luyện phát âm hiệu quả khi không có người chỉnh lỗi không?",
            voteCount = 23,
            voteState = VoteState.NONE,
            commentCount = 8,
            tag = PostTag.Tutorial
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
                searchQuery = "",
                posts = previewPosts,
                availableFilters = listOf(
                    HomeFeedFilter.Latest,
                    HomeFeedFilter.Trending,
                    HomeFeedFilter.Tag(PostTag.Tutorial),
                    HomeFeedFilter.Tag(PostTag.AskQuestion)
                ),
                selectedFilter = HomeFeedFilter.Latest
            ),
            onSearchQueryChange = {},
            onClearSearch = {},
            onFilterSelected = {},
            onUpvote = {},
            onDownvote = {},
            onPostClick = {},
            onCommentClick = {},
            onMoreActionsClick = {}
        )
    }
}
