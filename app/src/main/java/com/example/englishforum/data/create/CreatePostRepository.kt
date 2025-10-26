package com.example.englishforum.data.create

import android.net.Uri
import com.example.englishforum.core.model.forum.PostTag

sealed class CreatePostResult {
    data class Success(
        val postId: String?,
        val message: String
    ) : CreatePostResult()

    data class Declined(val reason: String) : CreatePostResult()
}

data class CreatePostAttachment(
    val id: String,
    val name: String
)

data class CreatePostImage(
    val uri: Uri,
    val displayName: String? = null
)

interface CreatePostRepository {
    suspend fun submitPost(
        title: String,
        body: String,
        attachments: List<CreatePostAttachment>,
        images: List<CreatePostImage>,
        tag: PostTag
    ): Result<CreatePostResult>
}
