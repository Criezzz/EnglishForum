package com.example.englishforum.data.post

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

    suspend fun updatePost(
        postId: String,
        title: String,
        body: String,
        tag: PostTag,
        previewImageUrl: String?,
        galleryImageUrls: List<String>
    ): Result<Unit>
}
