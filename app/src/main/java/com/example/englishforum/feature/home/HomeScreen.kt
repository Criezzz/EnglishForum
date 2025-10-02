package com.example.englishforum.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.ui.components.card.ForumContentCard
import com.example.englishforum.core.ui.components.card.ForumContentCardPlaceholder
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
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onMoreActionsClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HomeSearchBar(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                onClear = onClearSearch
            )
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
                    ForumContentCard(
                        modifier = Modifier.fillMaxWidth(),
                        meta = stringResource(
                            R.string.home_post_meta,
                            post.authorName,
                            post.relativeTimeText
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
            commentCount = 8
        ),
        HomePostUi(
            id = "2",
            authorName = "studybuddy",
            relativeTimeText = "2 giờ trước",
            title = "Looking for speaking partner",
            body = "We can practice trên Zoom 2-3h/tuần, ai quan tâm để lại comment nha!",
            voteCount = 17,
            voteState = VoteState.UPVOTED,
            commentCount = 14
        )
    )

    EnglishForumTheme {
        HomeContent(
            uiState = HomeUiState(
                isLoading = false,
                searchQuery = "",
                posts = previewPosts
            ),
            onSearchQueryChange = {},
            onClearSearch = {},
            onUpvote = {},
            onDownvote = {},
            onPostClick = {},
            onCommentClick = {},
            onMoreActionsClick = {}
        )
    }
}
