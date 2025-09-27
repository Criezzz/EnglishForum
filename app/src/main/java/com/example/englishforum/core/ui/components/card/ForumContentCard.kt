package com.example.englishforum.core.ui.components.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.englishforum.core.model.VoteState

@Composable
fun ForumContentCard(
    meta: String,
    voteCount: Int,
    modifier: Modifier = Modifier,
    title: String? = null,
    body: String? = null,
    voteState: VoteState = VoteState.NONE,
    onUpvoteClick: () -> Unit = {},
    onDownvoteClick: () -> Unit = {},
    onMoreActionsClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                if (!body.isNullOrBlank()) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

                Spacer(Modifier.weight(1f))
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

@Composable
private fun VoteIconButton(
    icon: ImageVector,
    contentDescription: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val defaultTint = MaterialTheme.colorScheme.onSurfaceVariant

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (selected) selectedColor.copy(alpha = 0.12f) else Color.Transparent,
            contentColor = if (selected) selectedColor else defaultTint
        )
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
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
