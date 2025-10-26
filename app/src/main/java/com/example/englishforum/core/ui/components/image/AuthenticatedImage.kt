package com.example.englishforum.core.ui.components.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.englishforum.core.di.LocalAppContainer

@Composable
fun rememberAuthenticatedImageRequest(url: String): ImageRequest {
    val appContainer = LocalAppContainer.current
    val session by appContainer.userSessionRepository.sessionFlow.collectAsState(initial = null)
    val context = LocalContext.current

    return remember(url, session?.accessToken, session?.tokenType, context) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .apply {
                session?.let {
                    val tokenType = it.tokenType.ifBlank { "Bearer" }
                    addHeader("Authorization", "$tokenType ${it.accessToken}")
                }
            }
            .build()
    }
}

@Composable
fun AuthenticatedRemoteImage(
    imageRequest: ImageRequest,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    loading: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null
) {
    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
        loading = {
            if (loading != null) {
                loading()
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {}
            }
        },
        error = {
            if (error != null) {
                error()
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {}
            }
        },
        success = { success ->
            onSuccess?.invoke(success)
            SubcomposeAsyncImageContent()
        }
    )
}

@Composable
fun AuthenticatedRemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    loading: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null,
    onSuccess: ((AsyncImagePainter.State.Success) -> Unit)? = null
) {
    val imageRequest = rememberAuthenticatedImageRequest(url)
    AuthenticatedRemoteImage(
        imageRequest = imageRequest,
        modifier = modifier,
        contentDescription = contentDescription,
        contentScale = contentScale,
        alignment = alignment,
        loading = loading,
        error = error,
        onSuccess = onSuccess
    )
}
