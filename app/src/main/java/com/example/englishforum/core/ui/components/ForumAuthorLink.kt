package com.example.englishforum.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CornerBasedShape

@Composable
fun ForumAuthorLink(
    name: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleSmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    maxLines: Int = 1,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    shape: CornerBasedShape = MaterialTheme.shapes.small
) {
    val displayColor = onClick?.let { MaterialTheme.colorScheme.primary } ?: color
    val clickableModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
    } else {
        modifier
    }

    Text(
        text = name,
        style = style,
        color = displayColor,
        maxLines = maxLines,
        overflow = overflow,
        modifier = clickableModifier
    )
}
