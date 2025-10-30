package com.example.englishforum.feature.noti

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.englishforum.R
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.ui.components.image.AuthenticatedRemoteImage
import androidx.compose.ui.layout.ContentScale
import com.example.englishforum.core.ui.theme.EnglishForumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotiRoute(
    modifier: Modifier = Modifier,
    onNotificationClick: (postId: String?, commentId: String?) -> Unit = { _, _ -> }
) {
    val appContainer = LocalAppContainer.current
    val viewModel: NotiViewModel = viewModel(
        factory = remember(appContainer) { NotiViewModelFactory(appContainer.notificationRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    NotiScreen(
        modifier = modifier,
        uiState = uiState,
        onNotificationClick = onNotificationClick,
        onMarkNotificationAsRead = viewModel::markNotificationAsRead,
        onMarkAllAsRead = viewModel::markAllNotificationsAsRead,
        onRefresh = viewModel::onRefresh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotiScreen(
    uiState: NotificationUiState,
    onNotificationClick: (postId: String?, commentId: String?) -> Unit,
    onMarkNotificationAsRead: (notificationId: String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullToRefreshState()
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.notifications_title)) },
                actions = {
                    IconButton(
                        onClick = onMarkAllAsRead,
                        enabled = uiState.unreadCount > 0
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DoneAll,
                            contentDescription = stringResource(id = R.string.notifications_mark_all_read)
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
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
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.notifications.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.notifications_empty_state),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.notifications,
                            key = { it.id }
                        ) { item ->
                            SwipeableNotificationItem(
                                item = item,
                                onClick = {
                                    if (!item.isRead) {
                                        onMarkNotificationAsRead(item.id)
                                    }
                                    if (item.postId != null) {
                                        onNotificationClick(item.postId, item.commentId)
                                    }
                                },
                                onSwipeToMarkAsRead = {
                                    if (!item.isRead) {
                                        onMarkNotificationAsRead(item.id)
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationItem(
    item: NotificationItemUi,
    onClick: () -> Unit,
    onSwipeToMarkAsRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swiped from right to left - mark as read
                    if (!item.isRead) {
                        onSwipeToMarkAsRead()
                    }
                    true // Allow the swipe animation to complete
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swiped from left to right - also mark as read
                    if (!item.isRead) {
                        onSwipeToMarkAsRead()
                    }
                    true // Allow the swipe animation to complete
                }
                else -> false
            }
        }
    )

    // Reset the state after marking as read
    LaunchedEffect(item.isRead) {
        if (item.isRead && dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            // Background shown when swiping
            val backgroundColor by animateColorAsState(
                targetValue = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> {
                        if (item.isRead) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    }
                    SwipeToDismissBoxValue.StartToEnd -> {
                        if (item.isRead) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    }
                    else -> MaterialTheme.colorScheme.surface
                },
                label = "swipeBackgroundColor"
            )

            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor, MaterialTheme.shapes.large)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                if (!item.isRead) {
                    Icon(
                        imageVector = Icons.Filled.Done,
                        contentDescription = stringResource(id = R.string.notifications_mark_as_read),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        enableDismissFromStartToEnd = !item.isRead, // Swipe right for unread
        enableDismissFromEndToStart = !item.isRead  // Swipe left for unread
    ) {
        NotificationListItem(
            item = item,
            onClick = onClick
        )
    }
}

@Composable
private fun NotificationListItem(
    item: NotificationItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (item.isRead) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        label = "notificationContainerColor"
    )
    val headlineStyle = MaterialTheme.typography.titleSmall.copy(
        fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.SemiBold
    )
    val timestampColor = if (item.isRead) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }
    val avatarContainerColor = if (item.isRead) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val avatarContentColor = if (item.isRead) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (item.isRead) 0.dp else 2.dp,
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (item.actorAvatarUrl != null) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 0.dp
                ) {
                    AuthenticatedRemoteImage(
                        url = item.actorAvatarUrl,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = avatarContainerColor,
                    tonalElevation = 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.actorInitial,
                            style = MaterialTheme.typography.titleMedium,
                            color = avatarContentColor
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = item.headline,
                        style = headlineStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    NotificationMetadata(
                        timestampText = item.timestampText,
                        timestampColor = timestampColor
                    )

                    item.supportingText?.let { supporting ->
                        Text(
                            text = supporting,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (!item.isRead) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        tonalElevation = 0.dp,
                        content = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationMetadata(
    timestampText: String,
    timestampColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = timestampText,
            style = MaterialTheme.typography.labelSmall,
            color = timestampColor
        )
    }
}

@Preview
@Composable
private fun NotificationItemPreview() {
    EnglishForumTheme {
        NotificationListItem(
            item = NotificationItemUi(
                id = "noti-1",
                actorInitial = "J",
                actorAvatarUrl = null,
                headline = "Jane_Doe đã bình luận bài viết của bạn",
                supportingText = "\"Thanks for sharing, saved to my drive already.\"",
                timestampText = "12 phút trước",
                postId = "post-1",
                commentId = "comment-9",
                isRead = false
            ),
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun NotificationItemReadPreview() {
    EnglishForumTheme {
        NotificationListItem(
            item = NotificationItemUi(
                id = "noti-2",
                actorInitial = "M",
                actorAvatarUrl = null,
                headline = "mentorX đã nhắc bạn trong bình luận",
                supportingText = "mentorX: Great compilation! I usually start learners with Cambridge 15.",
                timestampText = "58 phút trước",
                postId = "post-2",
                commentId = "comment-3",
                isRead = true
            ),
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun NotiScreenPreview() {
    EnglishForumTheme {
        NotiScreen(
            uiState = NotificationUiState(
                isLoading = false,
                notifications = listOf(
                    NotificationItemUi(
                        id = "noti-1",
                        actorInitial = "C",
                        actorAvatarUrl = null,
                        headline = "crystal đã bình luận bài viết của bạn",
                        supportingText = "\"Sed vulputate tellus magna, ac fringilla ipsum ornare in.\"",
                        timestampText = "9 phút trước",
                        postId = "post-1",
                        commentId = "comment-1",
                        isRead = false
                    ),
                    NotificationItemUi(
                        id = "noti-2",
                        actorInitial = "M",
                        actorAvatarUrl = null,
                        headline = "mentorX đã nhắc bạn trong bình luận",
                        supportingText = "mentorX: Great compilation! I usually start learners with Cambridge 15.",
                        timestampText = "58 phút trước",
                        postId = "post-2",
                        commentId = "comment-3",
                        isRead = true
                    )
                ),
                unreadCount = 1
            ),
            onNotificationClick = { _, _ -> },
            onMarkNotificationAsRead = {},
            onMarkAllAsRead = {},
            onRefresh = {}
        )
    }
}
