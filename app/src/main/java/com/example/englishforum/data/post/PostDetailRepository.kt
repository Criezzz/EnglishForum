package com.example.englishforum.data.post

import android.net.Uri
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostDetail
import com.example.englishforum.core.model.forum.PostTag
import kotlinx.coroutines.flow.Flow

interface PostDetailRepository {
    fun observePost(postId: String): Flow<ForumPostDetail?>

    suspend fun refreshPost(postId: String): Result<Unit>

    suspend fun setPostVote(postId: String, target: VoteState): Result<Unit>

    suspend fun setCommentVote(postId: String, commentId: String, target: VoteState): Result<Unit>

    suspend fun reportPost(postId: String, reason: String): Result<Unit>

    suspend fun deletePost(postId: String): Result<Unit>

    suspend fun addComment(
        postId: String,
        content: String,
        replyToCommentId: String? = null
    ): Result<Unit>

    suspend fun updatePost(
        postId: String,
        title: String,
        body: String,
        tag: PostTag,
        attachmentEdits: PostAttachmentEdit?
    ): Result<Unit>

    suspend fun updateComment(
        postId: String,
        commentId: String,
        content: String
    ): Result<Unit>

    suspend fun deleteComment(
        postId: String,
        commentId: String
    ): Result<Unit>
}

data class PostAttachmentEdit(
    val instructions: String,
    val newAttachments: List<PostAttachmentUpload>,
    val finalState: List<PostAttachmentState>
)

data class PostAttachmentUpload(
    val uri: Uri,
    val displayName: String? = null
)

data class PostAttachmentState(
    val id: String?,
    val uri: Uri,
    val isRemote: Boolean,
    val mediaType: String? = null
)
