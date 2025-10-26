package com.example.englishforum.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.englishforum.core.ui.components.image.AuthenticatedRemoteImage
import kotlin.math.abs

@Composable
fun ForumAuthorAvatar(
    name: String,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val palette = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
        MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    )
    val hash = name.hashCode()
    val safeHash = if (hash == Int.MIN_VALUE) 0 else abs(hash)
    val index = safeHash % palette.size
    val (containerColor, contentColor) = palette[index]
    val initial = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 0.dp
    ) {
        if (avatarUrl.isNullOrBlank()) {
            AvatarInitial(initial)
        } else {
            AuthenticatedRemoteImage(
                url = avatarUrl,
                modifier = Modifier.fillMaxSize(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                loading = {
                    AvatarInitial(initial)
                },
                error = {
                    AvatarInitial(initial)
                }
            )
        }
    }
}

@Composable
private fun AvatarInitial(initial: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}
