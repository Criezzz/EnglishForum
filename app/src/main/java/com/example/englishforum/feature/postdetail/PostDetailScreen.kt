package com.example.englishforum.feature.postdetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.SavedStateHandle
import com.example.englishforum.core.ui.components.image.AuthenticatedRemoteImage
import com.example.englishforum.core.ui.components.image.rememberAuthenticatedImageRequest
import com.example.englishforum.R
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.ui.components.ForumAuthorAvatar
import com.example.englishforum.core.ui.components.ForumAuthorLink
import com.example.englishforum.core.ui.components.VoteIconButton
import com.example.englishforum.core.ui.components.card.CommentPillPlacement
import com.example.englishforum.core.ui.components.card.ForumContentCard
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import coil.request.ImageRequest

private val CommentThreadIndent = 20.dp

private data class PostImageResource(
    val url: String,
    val request: ImageRequest
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailRoute(
    modifier: Modifier = Modifier,
    postId: String,
    commentId: String? = null,
    onBackClick: () -> Unit,
    savedStateHandle: SavedStateHandle,
    onNavigateToAiPractice: (String) -> Unit = {},
    onEditPostClick: (String) -> Unit,
    onPostDeleted: () -> Unit = onBackClick,
    onAuthorClick: (String) -> Unit = {}
) {
    val appContainer = LocalAppContainer.current
    val viewModel: PostDetailViewModel = viewModel(
        factory = remember(postId, appContainer) {
            PostDetailViewModelFactory(
                postId = postId,
                repository = appContainer.postDetailRepository,
                aiPracticeRepository = appContainer.aiPracticeRepository,
                userSessionRepository = appContainer.userSessionRepository
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    val postEditResult by savedStateHandle.getStateFlow("post_edit_result", false)
        .collectAsState(initial = false)

    LaunchedEffect(postEditResult) {
        if (postEditResult) {
            viewModel.onPostUpdatedExternally()
            savedStateHandle["post_edit_result"] = false
        }
    }

    PostDetailScreen(
        modifier = modifier,
        uiState = uiState,
        targetCommentId = commentId,
        onBackClick = onBackClick,
        onUpvotePost = viewModel::onUpvotePost,
        onDownvotePost = viewModel::onDownvotePost,
        onUpvoteComment = viewModel::onUpvoteComment,
        onDownvoteComment = viewModel::onDownvoteComment,
        onCommentDraftChanged = viewModel::onCommentDraftChanged,
        onSubmitComment = viewModel::onSubmitComment,
        onCancelReplyTarget = viewModel::onCancelReplyTarget,
        onReplyToComment = viewModel::onReplyToComment,
        onOpenAiPracticeClick = {
            viewModel.onAiPracticeClick(onNavigateToAiPractice)
        },
        onReportPost = viewModel::onReportPost,
        onEditPostClick = { onEditPostClick(postId) },
        onDeletePost = {
            viewModel.onDeletePost()
        },
        onEditComment = viewModel::onEditComment,
        onDeleteComment = viewModel::onDeleteComment,
        onUserMessageShown = viewModel::onUserMessageShown,
        onNewCommentHighlightShown = viewModel::onNewCommentHighlightShown,
        onPostDeletionHandled = viewModel::onPostDeletionHandled,
        onPostDeleted = onPostDeleted,
        onRefresh = viewModel::onRefresh,
        onAuthorClick = onAuthorClick
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
    onCommentDraftChanged: (String) -> Unit,
    onSubmitComment: () -> Unit,
    onCancelReplyTarget: () -> Unit,
    onReplyToComment: (String, String, String?) -> Unit,
    onOpenAiPracticeClick: () -> Unit,
    onReportPost: (String) -> Unit,
    onEditPostClick: () -> Unit,
    onDeletePost: () -> Unit,
    onEditComment: (String, String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onUserMessageShown: () -> Unit,
    onNewCommentHighlightShown: () -> Unit,
    onPostDeletionHandled: () -> Unit,
    onPostDeleted: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    targetCommentId: String? = null,
    onAuthorClick: (String) -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var highlightedCommentId by remember { mutableStateOf<String?>(null) }
    var isOptionsExpanded by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val post = uiState.post
    val pullRefreshState = rememberPullToRefreshState()

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(uiState.userMessage) {
        val message = uiState.userMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onUserMessageShown()
        }
    }

    // Handle highlighting newly posted comment
    LaunchedEffect(uiState.newlyPostedCommentId, uiState.comments) {
        val newCommentId = uiState.newlyPostedCommentId
        if (newCommentId != null && uiState.comments.isNotEmpty()) {
            val commentIndex = uiState.comments.indexOfFirst { it.id == newCommentId }
            if (commentIndex != -1) {
                // Scroll to the new comment (index + 2 because post and comments header)
                delay(100)
                listState.animateScrollToItem(commentIndex + 2)
                highlightedCommentId = newCommentId
                // Remove highlight after 3 seconds
                delay(3000)
                highlightedCommentId = null
                onNewCommentHighlightShown()
            }
        }
    }

    LaunchedEffect(uiState.isPostDeleted) {
        if (uiState.isPostDeleted) {
            onPostDeleted()
            onPostDeletionHandled()
        }
    }

    LaunchedEffect(uiState.isPerformingAction) {
        if (uiState.isPerformingAction) {
            isOptionsExpanded = false
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

    LaunchedEffect(uiState.commentComposer.replyTarget) {
        if (uiState.commentComposer.replyTarget == null && targetCommentId == null) {
            highlightedCommentId = null
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val titleText = post?.title.orEmpty()
                    if (titleText.isNotBlank()) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.auth_back_action)
                        )
                    }
                },
                actions = {
                    if (post != null) {
                        Box {
                            IconButton(
                                onClick = { isOptionsExpanded = true },
                                enabled = !uiState.isPerformingAction
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.post_detail_options_content_description)
                                )
                            }
                            PostDetailOptionsMenu(
                                expanded = isOptionsExpanded,
                                showReport = !uiState.isCurrentUserPostOwner,
                                isOwner = uiState.isCurrentUserPostOwner,
                                enabled = !uiState.isPerformingAction,
                                onDismissRequest = { isOptionsExpanded = false },
                                onReportClick = {
                                    isOptionsExpanded = false
                                    showReportDialog = true
                                },
                                onEditClick = {
                                    isOptionsExpanded = false
                                    onEditPostClick()
                                },
                                onDeleteClick = {
                                    isOptionsExpanded = false
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        snackbarHost = { 
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                SnackbarHost(hostState = snackbarHostState)
            }
        },
        bottomBar = {
            PostDetailCommentComposer(
                state = uiState.commentComposer,
                enabled = uiState.post != null && !uiState.isPostDeleted,
                onDraftChange = onCommentDraftChanged,
                onSendClick = onSubmitComment,
                onDismissReply = {
                    highlightedCommentId = null
                    onCancelReplyTarget()
                }
            )
        },
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

        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            state = pullRefreshState,
            isRefreshing = uiState.isRefreshing,
            onRefresh = {
                if (!uiState.isLoading) {
                    onRefresh()
                }
            },
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = topPadding)
                )
            }
        ) {
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
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            val postAuthorClick = uiState.post.authorUsername?.let { username ->
                                { onAuthorClick(username) }
                            }
                            ForumContentCard(
                                meta = uiState.post.relativeTimeText,
                                voteCount = uiState.post.voteCount,
                                title = uiState.post.title,
                                body = uiState.post.body,
                                voteState = uiState.post.voteState,
                                commentCount = uiState.post.commentCount,
                                onUpvoteClick = onUpvotePost,
                                onDownvoteClick = onDownvotePost,
                                showMoreActions = false,
                                leadingContent = {
                                    ForumAuthorAvatar(
                                        name = uiState.post.authorName,
                                        avatarUrl = uiState.post.authorAvatarUrl,
                                        modifier = Modifier.size(48.dp)
                                    )
                                },
                                headerContent = {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        ForumAuthorLink(
                                            name = uiState.post.authorName,
                                            onClick = postAuthorClick,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = uiState.post.relativeTimeText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                commentPillPlacement = CommentPillPlacement.End,
                                supportingContent = {
                                    val galleryImages = uiState.post.galleryImages
                                    val previewImageUrl = uiState.post.previewImageUrl
                                    var showFullScreenViewer by remember { mutableStateOf(false) }
                                    var selectedImageIndex by remember { mutableIntStateOf(0) }

                                    val galleryImageResources = galleryImages?.map { url ->
                                        PostImageResource(
                                            url = url,
                                            request = rememberAuthenticatedImageRequest(url)
                                        )
                                    }
                                    val previewImageResource = previewImageUrl?.let { url ->
                                        PostImageResource(
                                            url = url,
                                            request = rememberAuthenticatedImageRequest(url)
                                        )
                                    }

                                    when {
                                        !galleryImageResources.isNullOrEmpty() -> {
                                            val images = galleryImageResources
                                            PostImageGallery(
                                                images = images,
                                                onImageClick = { index ->
                                                    selectedImageIndex = index
                                                    showFullScreenViewer = true
                                                }
                                            )

                                            if (showFullScreenViewer) {
                                                FullScreenImageViewer(
                                                    images = images,
                                                    initialPage = selectedImageIndex,
                                                    onDismiss = { showFullScreenViewer = false }
                                                )
                                            }
                                        }

                                        previewImageResource != null -> {
                                            PostSingleImage(
                                                image = previewImageResource,
                                                onClick = {
                                                    selectedImageIndex = 0
                                                    showFullScreenViewer = true
                                                }
                                            )

                                            if (showFullScreenViewer) {
                                                FullScreenImageViewer(
                                                    images = listOf(previewImageResource),
                                                    initialPage = 0,
                                                    onDismiss = { showFullScreenViewer = false }
                                                )
                                            }
                                        }
                                    }
                                }
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
                                CommentThreadEntry(
                                    comment = comment,
                                    onUpvote = { onUpvoteComment(comment.id) },
                                    onDownvote = { onDownvoteComment(comment.id) },
                                    onReply = {
                                        highlightedCommentId = comment.id
                                        onReplyToComment(comment.id, comment.authorName, comment.authorUsername)
                                    },
                                    onEdit = { newContent ->
                                        onEditComment(comment.id, newContent)
                                    },
                                    onDelete = {
                                        onDeleteComment(comment.id)
                                    },
                                    isHighlighted = highlightedCommentId == comment.id,
                                    onAuthorClick = onAuthorClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportPostDialog(
            onDismiss = { showReportDialog = false }
        ) { reason ->
            showReportDialog = false
            onReportPost(reason)
        }
    }

    if (showDeleteDialog) {
        DeletePostConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDeletePost()
            }
        )
    }
}

@Composable
private fun PostDetailOptionsMenu(
    expanded: Boolean,
    showReport: Boolean,
    isOwner: Boolean,
    enabled: Boolean,
    onDismissRequest: () -> Unit,
    onReportClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        if (showReport) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.post_detail_action_report)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = null
                    )
                },
                enabled = enabled,
                onClick = onReportClick
            )
        }

        if (isOwner) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.post_detail_action_edit)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null
                    )
                },
                enabled = enabled,
                onClick = onEditClick
            )

            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.post_detail_action_delete)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null
                    )
                },
                enabled = enabled,
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun ReportPostDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val isSubmitEnabled = reason.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.post_detail_report_dialog_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.post_detail_report_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(text = stringResource(R.string.post_detail_report_reason_label)) },
                    placeholder = { Text(text = stringResource(R.string.post_detail_report_reason_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(reason.trim()) },
                enabled = isSubmitEnabled
            ) {
                Text(text = stringResource(R.string.post_detail_report_submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.auth_cancel_action))
            }
        }
    )
}

@Composable
private fun DeletePostConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = stringResource(R.string.post_detail_delete_dialog_title))
        },
        text = {
            Text(
                text = stringResource(R.string.post_detail_delete_dialog_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.post_detail_delete_confirm),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.auth_cancel_action))
            }
        }
    )
}

@Composable
private fun CommentThreadEntry(
    comment: PostCommentUi,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onReply: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onAuthorClick: (String) -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (comment.depth > 0) {
            Spacer(modifier = Modifier.width(CommentThreadIndent * comment.depth))
        }
        PostCommentItem(
            comment = comment,
            onUpvote = onUpvote,
            onDownvote = onDownvote,
            onReply = onReply,
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = Modifier.weight(1f),
            isHighlighted = isHighlighted,
            onAuthorClick = onAuthorClick
        )
    }
}

@Composable
private fun PostCommentItem(
    comment: PostCommentUi,
    onUpvote: () -> Unit,
    onDownvote: () -> Unit,
    onReply: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onAuthorClick: (String) -> Unit = {}
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val depthContainer = when (comment.depth) {
        0 -> MaterialTheme.colorScheme.surfaceContainerLow
        1 -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val targetContainerColor = when {
        isHighlighted -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        comment.isAuthor -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
        else -> depthContainer
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
        tonalElevation = when {
            isHighlighted -> 4.dp
            comment.isAuthor -> 3.dp
            else -> comment.depth.coerceIn(0, 4).dp
        },
        border = if (targetBorderColor != null) {
            BorderStroke(1.dp, borderColor)
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val authorClick = comment.authorUsername?.let { username ->
                    { onAuthorClick(username) }
                }
                ForumAuthorLink(
                    name = comment.authorName,
                    onClick = authorClick,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = comment.relativeTimeText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.post_detail_reply_action),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isHighlighted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier
                            .clickable(enabled = !isHighlighted) { onReply() }
                            .padding(vertical = 4.dp)
                    )
                    
                    if (comment.isCurrentUserComment) {
                        Text(
                            text = "Sửa",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { showEditDialog = true }
                                .padding(vertical = 4.dp)
                        )
                        
                        Text(
                            text = "Xoá",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clickable { showDeleteDialog = true }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                Row(
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
    
    if (showEditDialog) {
        EditCommentDialog(
            initialContent = comment.body,
            onDismiss = { showEditDialog = false },
            onConfirm = { newContent ->
                showEditDialog = false
                onEdit(newContent)
            }
        )
    }
    
    if (showDeleteDialog) {
        DeleteCommentConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            }
        )
    }
}

@Composable
private fun PostSingleImage(
    image: PostImageResource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    PostDetailImageCard(
        image = image,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
private fun PostDetailImageCard(
    image: PostImageResource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    var aspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    var ratioResolved by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        onClick = onClick
    ) {
        AuthenticatedRemoteImage(
            imageRequest = image.request,
            modifier = Modifier.fillMaxSize(),
            contentScale = if (ratioResolved) ContentScale.FillBounds else ContentScale.Crop,
            onSuccess = { success ->
                if (!ratioResolved) {
                    val intrinsicSize = success.painter.intrinsicSize
                    val width = intrinsicSize.width
                    val height = intrinsicSize.height
                    if (width.isFinite() && height.isFinite() && width > 0f && height > 0f) {
                        val resolvedRatio = width / height
                        if (resolvedRatio > 0f && resolvedRatio.isFinite()) {
                            aspectRatio = resolvedRatio
                            ratioResolved = true
                        }
                    }
                }
            },
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PostImageGallery(
    images: List<PostImageResource>,
    modifier: Modifier = Modifier,
    onImageClick: (Int) -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { images.size })
    val coroutineScope = rememberCoroutineScope()
    
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Image Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            PostGalleryImageItem(
                image = images[page],
                onClick = { onImageClick(page) }
            )
        }
        
        // Navigation Arrows
        if (images.size > 1) {
            // Left Arrow
            if (pagerState.currentPage > 0) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .size(32.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        tonalElevation = 1.dp
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous image",
                            modifier = Modifier
                                .padding(6.dp)
                                .size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            // Right Arrow
            if (pagerState.currentPage < images.size - 1) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(32.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        tonalElevation = 1.dp
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next image",
                            modifier = Modifier
                                .padding(6.dp)
                                .size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            
            // Page Indicator (Dots)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(images.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = CircleShape,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        }
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun PostGalleryImageItem(
    image: PostImageResource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    PostDetailImageCard(
        image = image,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
private fun ZoomableImage(
    image: PostImageResource,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    
                    if (scale > 1f) {
                        val maxOffsetX = (size.width * (scale - 1)) / 2
                        val maxOffsetY = (size.height * (scale - 1)) / 2
                        
                        offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AuthenticatedRemoteImage(
            imageRequest = image.request,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FullScreenImageViewer(
    images: List<PostImageResource>,
    initialPage: Int = 0,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { images.size }
        )
        val coroutineScope = rememberCoroutineScope()
        
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Image Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImage(image = images[page])
            }
            
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    tonalElevation = 2.dp
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            
            // Navigation arrows for multiple images
            if (images.size > 1) {
                // Left Arrow
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            tonalElevation = 2.dp
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous image",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                
                // Right Arrow
                if (pagerState.currentPage < images.size - 1) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            tonalElevation = 2.dp
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next image",
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
                
                // Page Indicator (Dots)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(images.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color.White.copy(alpha = 0.5f)
                            }
                        ) {}
                    }
                }
            }
            
            // Image counter text
            if (images.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
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
            authorId = "user-preview",
            authorName = "Jane_Doe",
            authorAvatarUrl = "mock://avatar/jane",
            relativeTimeText = "6 giờ trước",
            title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
            body = "Donec dictum rhoncus eros, eget fermentum dui laoreet a.",
            voteCount = 17,
            voteState = VoteState.UPVOTED,
            commentCount = 5,
            galleryImages = listOf(
                "mock://gallery/image-1",
                "mock://gallery/image-2",
                "mock://gallery/image-3"
            )
        ),
        comments = listOf(
            PostCommentUi(
                id = "comment-1",
                authorName = "crystal",
                relativeTimeText = "2 giờ trước",
                body = "Etiam vitae ex massa. Sed vulputate tellus magna.",
                voteCount = 6,
                voteState = VoteState.NONE,
                isAuthor = false,
                depth = 0,
                hasReplies = true,
                isFirstChild = true,
                isLastChild = false
            ),
            PostCommentUi(
                id = "comment-1-1",
                authorName = "mentorX",
                relativeTimeText = "1 giờ trước",
                body = "Mình nghĩ bạn nên thu âm từng câu rồi đối chiếu IPA.",
                voteCount = 12,
                voteState = VoteState.UPVOTED,
                isAuthor = false,
                depth = 1,
                hasReplies = true,
                isFirstChild = true,
                isLastChild = false
            ),
            PostCommentUi(
                id = "comment-1-1-1",
                authorName = "Jane_Doe",
                relativeTimeText = "45 phút trước",
                body = "Cảm ơn tips, mình sẽ thử áp dụng ngay!",
                voteCount = 2,
                voteState = VoteState.NONE,
                isAuthor = true,
                depth = 2,
                hasReplies = false,
                isFirstChild = true,
                isLastChild = true
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
            onCommentDraftChanged = {},
            onSubmitComment = {},
            onCancelReplyTarget = {},
            onReplyToComment = { _, _, _ -> },
            onOpenAiPracticeClick = {},
            onReportPost = {},
            onEditPostClick = {},
            onDeletePost = {},
            onEditComment = { _, _ -> },
            onDeleteComment = {},
            onUserMessageShown = {},
            onNewCommentHighlightShown = {},
            onPostDeletionHandled = {},
            onPostDeleted = {},
            onRefresh = {}
        )
    }
}

@Composable
private fun EditCommentDialog(
    initialContent: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var editedContent by remember { mutableStateOf(initialContent) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(text = "Chỉnh sửa bình luận")
        },
        text = {
            OutlinedTextField(
                value = editedContent,
                onValueChange = { editedContent = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nhập nội dung bình luận") },
                minLines = 3,
                maxLines = 10
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(editedContent) },
                enabled = editedContent.trim().isNotEmpty()
            ) {
                Text(text = "Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.auth_cancel_action))
            }
        }
    )
}

@Composable
private fun DeleteCommentConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(text = "Xoá bình luận")
        },
        text = {
            Text(
                text = "Bạn có chắc chắn muốn xoá bình luận này? Hành động này không thể hoàn tác.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "Xoá",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.auth_cancel_action))
            }
        }
    )
}
