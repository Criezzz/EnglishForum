package com.example.englishforum.core.ui.components.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.ui.components.VoteIconButton

enum class CommentPillPlacement {
    BesideVotes,
    End
}

@Composable
fun ForumContentCard(
    meta: String,
    voteCount: Int,
    modifier: Modifier = Modifier,
    title: String? = null,
    body: String? = null,
    voteState: VoteState = VoteState.NONE,
    commentCount: Int? = null,
    onCommentClick: (() -> Unit)? = null,
    onUpvoteClick: () -> Unit = {},
    onDownvoteClick: () -> Unit = {},
    onMoreActionsClick: () -> Unit = {},
    showMoreActions: Boolean = true,
    commentPillPlacement: CommentPillPlacement = CommentPillPlacement.BesideVotes,
    onCardClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable ColumnScope.() -> Unit)? = null,
    bodyMaxLines: Int = Int.MAX_VALUE,
    bodyOverflow: TextOverflow = TextOverflow.Clip,
    headerContent: (@Composable ColumnScope.() -> Unit)? = null,
    bodyContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        onClick = onCardClick ?: {},
        enabled = onCardClick != null,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            var hasContentBeforeActions = false

            if (leadingContent != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leadingContent()
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (headerContent != null) {
                            headerContent()
                        } else {
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!title.isNullOrBlank()) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                hasContentBeforeActions = true
            } else {
                val defaultHeader: @Composable ColumnScope.() -> Unit = {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                val header = headerContent ?: defaultHeader
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), content = header)
                hasContentBeforeActions = true
            }

            when {
                bodyContent != null -> {
                    if (hasContentBeforeActions) {
                        Spacer(Modifier.height(2.dp))
                    }
                    bodyContent()
                    hasContentBeforeActions = true
                }
                !body.isNullOrBlank() -> {
                    if (hasContentBeforeActions) {
                        Spacer(Modifier.height(2.dp))
                    }
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = bodyMaxLines,
                        overflow = bodyOverflow
                    )
                    hasContentBeforeActions = true
                }
            }

            supportingContent?.let { content ->
                if (hasContentBeforeActions) {
                    Spacer(Modifier.height(12.dp))
                }
                content()
                hasContentBeforeActions = true
            }

            if (hasContentBeforeActions) {
                Spacer(Modifier.height(12.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                VoteIconButton(
                    icon = Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    selected = voteState == VoteState.UPVOTED,
                    onClick = onUpvoteClick
                )
                Spacer(Modifier.width(8.dp))

                Surface(
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = MaterialTheme.shapes.small,
                    color = Color.Transparent
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        text = voteCount.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.width(8.dp))
                VoteIconButton(
                    icon = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    selected = voteState == VoteState.DOWNVOTED,
                    onClick = onDownvoteClick
                )

                val showCommentPill = commentCount != null

                if (showCommentPill && commentPillPlacement == CommentPillPlacement.BesideVotes) {
                    Spacer(Modifier.width(12.dp))
                    CommentCountPill(
                        commentCount = commentCount!!,
                        onClick = onCommentClick
                    )
                }

                Spacer(Modifier.weight(1f))

                if (showCommentPill && commentPillPlacement == CommentPillPlacement.End) {
                    CommentCountPill(
                        commentCount = commentCount!!,
                        onClick = onCommentClick
                    )
                }

                if (showMoreActions) {
                    if (showCommentPill && commentPillPlacement == CommentPillPlacement.End) {
                        Spacer(Modifier.width(12.dp))
                    }
                    IconButton(onClick = onMoreActionsClick) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCountPill(
    commentCount: Int,
    onClick: (() -> Unit)?
) {
    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = commentCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ForumContentCardPlaceholder(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp),
                shape = MaterialTheme.shapes.extraSmall,
                content = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                shape = MaterialTheme.shapes.extraSmall,
                content = {}
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                shape = MaterialTheme.shapes.extraSmall,
                content = {}
            )
        }
    }
}
