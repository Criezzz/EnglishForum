package com.example.englishforum.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.englishforum.R
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.core.ui.components.card.ForumContentCard
import com.example.englishforum.core.ui.components.card.ForumContentCardPlaceholder
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import com.example.englishforum.core.ui.toLabelResId

private const val SEARCH_RESULT_BODY_MAX_LINES = 5
private val SEARCH_RESULT_AVATAR_SIZE = 44.dp

@Composable
fun SearchRoute(
    modifier: Modifier = Modifier,
    onPostClick: (String) -> Unit = {},
    onCommentClick: (String) -> Unit = {},
    onMoreActionsClick: (String) -> Unit = {}
) {
    val appContainer = LocalAppContainer.current
    val viewModel: SearchViewModel = viewModel(
        factory = remember(appContainer) { SearchViewModelFactory(appContainer.searchRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    SearchScreen(
        modifier = modifier,
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onUpvote = viewModel::onUpvote,
        onDownvote = viewModel::onDownvote,
        onPostClick = onPostClick,
        onCommentClick = onCommentClick,
        onMoreActionsClick = onMoreActionsClick
    )
}

@Composable
private fun SearchScreen(
    modifier: Modifier = Modifier,
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onPostClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onMoreActionsClick: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                value = uiState.query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = {
                            onClearQuery()
                            keyboardController?.show()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.search_clear_content_description)
                            )
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    focusManager.clearFocus(force = true)
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        uiState.errorMessage?.let { message ->
            SearchErrorMessage(
                message = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(4.dp))

        when {
            uiState.isLoading && uiState.query.isBlank() -> {
                SearchLanding(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }

            uiState.isLoading -> {
                SearchLoadingList(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            }

            uiState.query.isBlank() -> {
                SearchLanding(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }

            uiState.results.isEmpty() -> {
                SearchEmptyState(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }

            else -> {
                SearchResultList(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    results = uiState.results,
                    onPostClick = onPostClick,
                    onCommentClick = onCommentClick,
                    onUpvote = onUpvote,
                    onDownvote = onDownvote,
                    onMoreActionsClick = onMoreActionsClick
                )
            }
        }
    }
}

@Composable
private fun SearchLanding(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                modifier = Modifier.padding(16.dp),
                imageVector = Icons.Filled.Search,
                contentDescription = null
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.search_intro_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.search_intro_supporting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SearchErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        tonalElevation = 0.dp
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun SearchEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Icon(
                modifier = Modifier.padding(16.dp),
                imageVector = Icons.Outlined.Search,
                contentDescription = null
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.search_empty_state_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.search_empty_state_supporting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SearchLoadingList(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(3) {
            ForumContentCardPlaceholder(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SearchResultList(
    modifier: Modifier = Modifier,
    results: List<SearchResultUi>,
    onPostClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onMoreActionsClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(results, key = { it.id }) { result ->
            SearchResultCard(
                result = result,
                onPostClick = onPostClick,
                onCommentClick = onCommentClick,
                onUpvote = onUpvote,
                onDownvote = onDownvote,
                onMoreActionsClick = onMoreActionsClick
            )
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResultUi,
    onPostClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onUpvote: (String) -> Unit,
    onDownvote: (String) -> Unit,
    onMoreActionsClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tagLabel = stringResource(result.tag.toLabelResId())
    ForumContentCard(
        modifier = modifier.fillMaxWidth(),
        meta = stringResource(
            R.string.home_post_meta_with_tag,
            result.authorName,
            result.relativeTimeText,
            tagLabel
        ),
        title = result.title.takeIf { it.isNotBlank() },
        body = result.body.takeIf { it.isNotBlank() },
        bodyMaxLines = SEARCH_RESULT_BODY_MAX_LINES,
        bodyOverflow = TextOverflow.Ellipsis,
        voteCount = result.voteCount,
        voteState = result.voteState,
        commentCount = result.commentCount,
        onCommentClick = { onCommentClick(result.id) },
        onUpvoteClick = { onUpvote(result.id) },
        onDownvoteClick = { onDownvote(result.id) },
        onMoreActionsClick = { onMoreActionsClick(result.id) },
        onCardClick = { onPostClick(result.id) },
        leadingContent = {
            SearchResultAvatar(
                name = result.authorName,
                modifier = Modifier.size(SEARCH_RESULT_AVATAR_SIZE)
            )
        },
        supportingContent = {
            if (result.previewImageUrl != null) {
                SearchResultImagePlaceholder()
            }
        }
    )
}

@Composable
private fun SearchResultAvatar(
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
    val safeHash = if (hash == Int.MIN_VALUE) 0 else kotlin.math.abs(hash)
    val (containerColor, contentColor) = palette[safeHash % palette.size]
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
private fun SearchResultImagePlaceholder(
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

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    val previewResults = listOf(
        SearchResultUi(
            id = "1",
            authorName = "kellytran",
            relativeTimeText = "3 phút trước",
            title = "Tổng hợp tài liệu ngữ pháp cơ bản",
            body = "Mình chia sẻ bộ tài liệu luyện ngữ pháp cho người mới bắt đầu.",
            voteCount = 42,
            voteState = VoteState.UPVOTED,
            commentCount = 12,
            tag = PostTag.Resource
        ),
        SearchResultUi(
            id = "2",
            authorName = "studybuddy",
            relativeTimeText = "2 giờ trước",
            title = "Looking for speaking partner",
            body = "We can practice trên Zoom 2-3h/tuần, ai quan tâm để lại comment nha!",
            voteCount = 17,
            voteState = VoteState.NONE,
            commentCount = 14,
            tag = PostTag.AskQuestion
        )
    )

    EnglishForumTheme {
        SearchScreen(
            uiState = SearchUiState(
                query = "speaking",
                isLoading = false,
                results = previewResults
            ),
            onQueryChange = {},
            onClearQuery = {},
            onUpvote = {},
            onDownvote = {},
            onPostClick = {},
            onCommentClick = {},
            onMoreActionsClick = {}
        )
    }
}
