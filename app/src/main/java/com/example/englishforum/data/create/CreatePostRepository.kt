package com.example.englishforum.data.create

import com.example.englishforum.core.model.forum.PostTag

sealed class CreatePostResult {
    data class Success(val postId: String) : CreatePostResult()
    data class Declined(val reason: String) : CreatePostResult()
}

data class CreatePostAttachment(
    val id: String,
    val name: String
)

interface CreatePostRepository {
    suspend fun submitPost(
        title: String,
        body: String,
        attachments: List<CreatePostAttachment>,
        tag: PostTag
    ): Result<CreatePostResult>
}
