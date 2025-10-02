package com.example.englishforum.feature.postdetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.englishforum.R
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.ui.components.VoteIconButton
import com.example.englishforum.core.ui.components.card.CommentPillPlacement
import com.example.englishforum.core.ui.components.card.ForumContentCard
import com.example.englishforum.core.ui.theme.EnglishForumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailRoute(
    modifier: Modifier = Modifier,
    postId: String,
    commentId: String? = null,
    onBackClick: () -> Unit,
    onNavigateToAiPractice: (String) -> Unit = {}
) {
    val appContainer = LocalAppContainer.current
    val viewModel: PostDetailViewModel = viewModel(
        factory = remember(postId, appContainer) {
            PostDetailViewModelFactory(
                postId = postId,
                repository = appContainer.postDetailRepository,
                aiPracticeRepository = appContainer.aiPracticeRepository
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    PostDetailScreen(
        modifier = modifier,
        uiState = uiState,
        targetCommentId = commentId,
        onBackClick = onBackClick,
        onUpvotePost = viewModel::onUpvotePost,
        onDownvotePost = viewModel::onDownvotePost,
        onUpvoteComment = viewModel::onUpvoteComment,
        onDownvoteComment = viewModel::onDownvoteComment,
        onOpenAiPracticeClick = {
            viewModel.onAiPracticeClick(onNavigateToAiPractice)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    uiState: PostDetailUiState,
    onBackClick: () -> Unit,
    onUpvotePost: () -> Unit,
    onDownvotePost: () -> Unit,
    onUpvoteComment: (String) -> Unit,
    onDownvoteComment: (String) -> Unit,
    onOpenAiPracticeClick: () -> Unit,
    modifier: Modifier = Modifier,
    targetCommentId: String? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var highlightedCommentId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle scrolling to specific comment
    LaunchedEffect(targetCommentId, uiState.comments) {
        if (targetCommentId != null && uiState.comments.isNotEmpty()) {
            val commentIndex = uiState.comments.indexOfFirst { it.id == targetCommentId }
            if (commentIndex != -1) {
                // Scroll to comment (index + 1 because the first item is the post)
                // Add delay to ensure the LazyColumn has been laid out
                delay(100)
                listState.animateScrollToItem(commentIndex + 2) // +2 because post and comments header
                highlightedCommentId = targetCommentId
                // Remove highlight after 3 seconds
                delay(3000)
                highlightedCommentId = null
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.auth_back_action)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.post_detail_options_content_description)
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!uiState.isAiPracticeChecking) {
                        onOpenAiPracticeClick()
                    }
                },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                if (uiState.isAiPracticeChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = stringResource(R.string.post_detail_ai_practice_content_description)
                    )
                }
            }
        }
    ) { innerPadding ->
        val topPadding = innerPadding.calculateTopPadding()
        val bottomPadding = innerPadding.calculateBottomPadding()

        when {
            uiState.isLoading && uiState.post == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.post == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.post_detail_missing_post),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp + topPadding,
                        bottom = 120.dp + bottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ForumContentCard(
                            meta = stringResource(
                                R.string.home_post_meta,
                                uiState.post.authorName,
                        uiState.post.relativeTimeText
                            ),
                            voteCount = uiState.post.voteCount,
                            title = uiState.post.title,
                            body = uiState.post.body,
                            voteState = uiState.post.voteState,
                            commentCount = uiState.post.commentCount,
                            onUpvoteClick = onUpvotePost,
                            onDownvoteClick = onDownvotePost,
                            showMoreActions = false,
                            commentPillPlacement = CommentPillPlacement.End
                        )
                    }

                    item {
                        Text(
                            text = stringResource(R.string.post_detail_comments_header),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (uiState.comments.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.post_detail_empty_state),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 32.dp)
                            )
                        }
                    } else {
                        items(
                            items = uiState.comments,
                            key = { it.id }
                        ) { comment ->
                            PostCommentItem(
                                comment = comment,
                                onUpvote = { onUpvoteComment(comment.id) },
                                onDownvote = { onDownvoteComment(comment.id) },
                                isHighlighted = highlightedCommentId == comment.id
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostCommentItem(
    comment: PostCommentUi,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val targetContainerColor = when {
        isHighlighted -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        comment.isAuthor -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    
    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = tween(durationMillis = 300),
        label = "containerColor"
    )

    val targetBorderColor = when {
        isHighlighted -> MaterialTheme.colorScheme.secondary
        comment.isAuthor -> MaterialTheme.colorScheme.outlineVariant
        else -> null
    }
    
    val borderColor by animateColorAsState(
        targetValue = targetBorderColor ?: MaterialTheme.colorScheme.outline,
        animationSpec = tween(durationMillis = 300),
        label = "borderColor"
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = if (comment.isAuthor) 2.dp else if (isHighlighted) 1.dp else 0.dp,
        border = if (targetBorderColor != null) {
            BorderStroke(1.dp, borderColor)
        } else null
    ) {
        val metaText = stringResource(
            R.string.home_post_meta,
            comment.authorName,
            comment.relativeTimeText
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (comment.isAuthor) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(R.string.post_detail_author_badge),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = comment.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                VoteIconButton(
                    icon = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = null,
                    selected = comment.voteState == VoteState.UPVOTED,
                    onClick = onUpvote
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = comment.voteCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                VoteIconButton(
                    icon = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    selected = comment.voteState == VoteState.DOWNVOTED,
                    onClick = onDownvote
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PostDetailScreenPreview() {
    val previewState = PostDetailUiState(
        isLoading = false,
        post = PostDetailUi(
            id = "post-1",
            authorName = "Jane_Doe",
            relativeTimeText = "6 giờ trước",
            title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
            body = "Donec dictum rhoncus eros, eget fermentum dui laoreet a.",
            voteCount = 17,
            voteState = VoteState.UPVOTED,
            commentCount = 5
        ),
        comments = listOf(
            PostCommentUi(
                id = "comment-1",
                authorName = "crystal",
                relativeTimeText = "2 giờ trước",
                body = "Etiam vitae ex massa. Sed vulputate tellus magna.",
                voteCount = 6,
                voteState = VoteState.NONE,
                isAuthor = false
            ),
            PostCommentUi(
                id = "comment-2",
                authorName = "Jane_Doe",
                relativeTimeText = "1 giờ trước",
                body = "Cảm ơn mọi người đã chia sẻ nhé!",
                voteCount = 12,
                voteState = VoteState.DOWNVOTED,
                isAuthor = true
            )
        )
    )

    EnglishForumTheme {
        PostDetailScreen(
            uiState = previewState,
            onBackClick = {},
            onUpvotePost = {},
            onDownvotePost = {},
            onUpvoteComment = {},
            onDownvoteComment = {},
            onOpenAiPracticeClick = {}
        )
    }
}
