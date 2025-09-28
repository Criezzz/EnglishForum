package com.example.englishforum.feature.noti

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.englishforum.R
import com.example.englishforum.core.ui.theme.EnglishForumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotiRoute(
    modifier: Modifier = Modifier,
    viewModel: NotiViewModel = viewModel(),
    onNotificationClick: (postId: String, commentId: String?) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    NotiScreen(
        modifier = modifier,
        uiState = uiState,
        onNotificationClick = onNotificationClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotiScreen(
    uiState: NotificationUiState,
    onNotificationClick: (postId: String, commentId: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.notifications_title)) },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
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
                        NotificationListItem(
                            item = item,
                            onClick = { onNotificationClick(item.postId, item.commentId) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun NotificationListItem(
    item: NotificationItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                tonalElevation = 0.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = item.actorInitial,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f, fill = false),
                        text = item.headline,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item.timestampText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = item.supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
                headline = "Jane_Doe đã bình luận bài viết của bạn",
                supportingText = "\"Thanks for sharing, saved to my drive already.\"",
                timestampText = "12 phút trước",
                postId = "post-1",
                commentId = "comment-9"
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
                        headline = "crystal đã bình luận bài viết của bạn",
                        supportingText = "\"Sed vulputate tellus magna, ac fringilla ipsum ornare in.\"",
                        timestampText = "9 phút trước",
                        postId = "post-1",
                        commentId = "comment-1"
                    ),
                    NotificationItemUi(
                        id = "noti-2",
                        actorInitial = "M",
                        headline = "mentorX đã nhắc bạn trong bình luận",
                        supportingText = "mentorX: Great compilation! I usually start learners with Cambridge 15.",
                        timestampText = "58 phút trước",
                        postId = "post-2",
                        commentId = "comment-3"
                    )
                )
            ),
            onNotificationClick = { _, _ -> }
        )
    }
}
