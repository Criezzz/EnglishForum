package com.example.englishforum.feature.postdetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.SavedStateHandle
import com.example.englishforum.R
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.ui.components.VoteIconButton
import com.example.englishforum.core.ui.components.card.CommentPillPlacement
import com.example.englishforum.core.ui.components.card.ForumContentCard
import com.example.englishforum.core.ui.theme.EnglishForumTheme

private val CommentThreadIndent = 20.dp

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
    onPostDeleted: () -> Unit = onBackClick
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
        onOpenAiPracticeClick = {
            viewModel.onAiPracticeClick(onNavigateToAiPractice)
        },
        onReportPost = viewModel::onReportPost,
        onEditPostClick = { onEditPostClick(postId) },
        onDeletePost = {
            viewModel.onDeletePost()
        },
        onUserMessageShown = viewModel::onUserMessageShown,
        onPostDeletionHandled = viewModel::onPostDeletionHandled,
        onPostDeleted = onPostDeleted,
        onRefresh = viewModel::onRefresh
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
    onReportPost: (String) -> Unit,
    onEditPostClick: () -> Unit,
    onDeletePost: () -> Unit,
    onUserMessageShown: () -> Unit,
    onPostDeletionHandled: () -> Unit,
    onPostDeleted: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    targetCommentId: String? = null
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
                            bottom = 120.dp
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
                                commentPillPlacement = CommentPillPlacement.End,
                                supportingContent = {
                                    val galleryImages = uiState.post.galleryImages
                                    val previewImageUrl = uiState.post.previewImageUrl
                                    var showFullScreenViewer by remember { mutableStateOf(false) }
                                    var selectedImageIndex by remember { mutableIntStateOf(0) }

                                    when {
                                        !galleryImages.isNullOrEmpty() -> {
                                            PostImageGallery(
                                                images = galleryImages,
                                                onImageClick = { index ->
                                                    selectedImageIndex = index
                                                    showFullScreenViewer = true
                                                }
                                            )

                                            if (showFullScreenViewer) {
                                                FullScreenImageViewer(
                                                    images = galleryImages,
                                                    initialPage = selectedImageIndex,
                                                    onDismiss = { showFullScreenViewer = false }
                                                )
                                            }
                                        }

                                        previewImageUrl != null -> {
                                            PostSingleImage(
                                                onClick = {
                                                    selectedImageIndex = 0
                                                    showFullScreenViewer = true
                                                }
                                            )

                                            if (showFullScreenViewer) {
                                                FullScreenImageViewer(
                                                    images = listOf(previewImageUrl),
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
                                    isHighlighted = highlightedCommentId == comment.id
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
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
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
            modifier = Modifier.weight(1f),
            isHighlighted = isHighlighted
        )
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.post_detail_reply_action),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

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
}

@Composable
private fun PostSingleImage(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        onClick = onClick
    ) {
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
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PostImageGallery(
    images: List<String>,
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
                imageUrl = images[page],
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
    imageUrl: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = imageUrl.substringAfterLast("/"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(
    imageUrl: String,
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
        // Full-size image placeholder - maintains aspect ratio
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = imageUrl.substringAfterLast("/"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FullScreenImageViewer(
    images: List<String>,
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
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Image Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ZoomableImage(imageUrl = images[page])
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
            onOpenAiPracticeClick = {},
            onReportPost = {},
            onEditPostClick = {},
            onDeletePost = {},
            onUserMessageShown = {},
            onPostDeletionHandled = {},
            onPostDeleted = {},
            onRefresh = {}
        )
    }
}
