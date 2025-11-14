package com.example.englishforum.core.ui.components.card

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.ui.components.VoteIconButton
import kotlinx.coroutines.launch

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
    bodyContent: (@Composable ColumnScope.() -> Unit)? = null,
    voteCountAnimationKey: Int = 0,
    commentCountAnimationKey: Int = 0,
    testId: String? = null
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
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ForumVoteActionGroup(
                    voteCount = voteCount,
                    voteState = voteState,
                    onUpvoteClick = onUpvoteClick,
                    onDownvoteClick = onDownvoteClick,
                    animationKey = voteCountAnimationKey,
                    testId = testId
                )

                val showCommentAction = commentCount != null

                if (showCommentAction && commentPillPlacement == CommentPillPlacement.BesideVotes) {
                    Spacer(Modifier.width(12.dp))
                    ForumCommentActionButton(
                        commentCount = commentCount!!,
                        onClick = onCommentClick,
                        modifier = Modifier.padding(start = 8.dp),
                        animationKey = commentCountAnimationKey
                    )
                }

                Spacer(Modifier.weight(1f))

                if (showCommentAction && commentPillPlacement == CommentPillPlacement.End) {
                    ForumCommentActionButton(
                        commentCount = commentCount!!,
                        onClick = onCommentClick,
                        modifier = Modifier.padding(start = 8.dp),
                        animationKey = commentCountAnimationKey
                    )
                }

                if (showMoreActions) {
                    if (showCommentAction && commentPillPlacement == CommentPillPlacement.End) {
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
private fun ForumVoteActionGroup(
    voteCount: Int,
    voteState: VoteState,
    onUpvoteClick: () -> Unit,
    onDownvoteClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationKey: Int = 0,
    testId: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    val containerColor: Color
    val borderColor: Color?
    val contentColor: Color

    when (voteState) {
        VoteState.UPVOTED -> {
            containerColor = colorScheme.primaryContainer
            borderColor = null
            contentColor = colorScheme.onPrimaryContainer
        }
        VoteState.DOWNVOTED -> {
            containerColor = colorScheme.errorContainer
            borderColor = null
            contentColor = colorScheme.onErrorContainer
        }
        VoteState.NONE -> {
            containerColor = Color.Transparent
            borderColor = colorScheme.outlineVariant
            contentColor = colorScheme.onSurface
        }
    }
    
    // Enhanced animation state
    val scale = remember { Animatable(1f) }
    val highlightProgress = remember { Animatable(0f) }
    
    // Highlight color (subtle primary tint)
    val highlightColor = colorScheme.primary.copy(alpha = 0.15f)
    val animatedContainerColor = if (highlightProgress.value > 0f) {
        lerp(containerColor, highlightColor, highlightProgress.value)
    } else {
        containerColor
    }
    
    LaunchedEffect(animationKey) {
        if (animationKey > 0) {
            // Launch both animations in parallel
            launch {
                // Bouncy scale animation
                scale.animateTo(
                    targetValue = 1.2f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                // Color pulse animation
                highlightProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200)
                )
                highlightProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 400)
                )
            }
        }
    }

    ForumActionContainer(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        containerColor = animatedContainerColor,
        contentColor = contentColor,
        borderColor = borderColor
    ) {
        VoteIconButton(
            icon = Icons.Filled.KeyboardArrowUp,
            contentDescription = null,
            selected = voteState == VoteState.UPVOTED,
            onClick = onUpvoteClick,
            buttonSize = 32.dp,
            enforceMinimumTouchTarget = false,
            selectedColorOverride = colorScheme.primary,
            modifier = if (testId != null) Modifier.testTag("upvote_button_$testId") else Modifier
        )
        Text(
            text = voteCount.toString(),
            style = MaterialTheme.typography.labelLarge,
            modifier = if (testId != null) Modifier.testTag("vote_count_$testId") else Modifier
        )
        VoteIconButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            selected = voteState == VoteState.DOWNVOTED,
            onClick = onDownvoteClick,
            buttonSize = 32.dp,
            enforceMinimumTouchTarget = false,
            selectedColorOverride = colorScheme.error,
            modifier = if (testId != null) Modifier.testTag("downvote_button_$testId") else Modifier
        )
    }
}

@Composable
private fun ForumCommentActionButton(
    commentCount: Int,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    animationKey: Int = 0
) {
    val colorScheme = MaterialTheme.colorScheme
    
    // Enhanced animation state
    val scale = remember { Animatable(1f) }
    val highlightProgress = remember { Animatable(0f) }
    
    // Highlight color (subtle primary tint)
    val highlightColor = colorScheme.primary.copy(alpha = 0.15f)
    val baseColor = Color.Transparent
    val animatedContainerColor = if (highlightProgress.value > 0f) {
        lerp(baseColor, highlightColor, highlightProgress.value)
    } else {
        baseColor
    }
    
    LaunchedEffect(animationKey) {
        if (animationKey > 0) {
            // Launch both animations in parallel
            launch {
                // Bouncy scale animation
                scale.animateTo(
                    targetValue = 1.2f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                // Color pulse animation
                highlightProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 200)
                )
                highlightProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 400)
                )
            }
        }
    }
    
    ForumActionContainer(
        modifier = modifier.graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        },
        onClick = onClick,
        containerColor = animatedContainerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null
        )
        Text(
            text = commentCount.toString(),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun ForumActionContainer(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color?,
    content: @Composable RowScope.() -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    val border = borderColor?.let { BorderStroke(1.dp, it) }

    if (onClick == null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            border = border
        ) {
            Row(
                modifier = Modifier
                    .requiredHeight(40.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier,
            onClick = onClick,
            enabled = true,
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            tonalElevation = 0.dp,
            border = border
        ) {
            Row(
                modifier = Modifier
                    .requiredHeight(40.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = content
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
