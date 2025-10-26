package com.example.englishforum.core.ui.components.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun ForumPostPreviewImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        AuthenticatedRemoteImage(
            url = imageUrl,
            modifier = Modifier.fillMaxSize(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            loading = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            error = {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}
