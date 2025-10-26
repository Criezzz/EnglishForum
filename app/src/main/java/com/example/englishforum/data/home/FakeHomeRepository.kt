package com.example.englishforum.data.home

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumPostDetail
import com.example.englishforum.core.model.forum.ForumPostSummary
import com.example.englishforum.data.post.FakePostStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeHomeRepository(
    private val store: FakePostStore = FakePostStore
) : HomeRepository {

    override val postsStream: Flow<List<ForumPostSummary>> = store.posts.map { posts ->
        posts.map { detail -> detail.toSummary() }
    }

    override suspend fun setVoteState(postId: String, target: VoteState): Result<Unit> {
        return if (store.updatePostVote(postId, target)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Post not found"))
        }
    }

    private fun ForumPostDetail.toSummary(): ForumPostSummary {
        return ForumPostSummary(
            id = id,
            authorName = authorName,
            authorUsername = authorUsername,
            minutesAgo = minutesAgo,
            title = title,
            body = body,
            voteCount = voteCount,
            voteState = voteState,
            commentCount = commentCount,
            tag = tag,
            authorAvatarUrl = authorAvatarUrl,
            previewImageUrl = previewImageUrl
        )
    }
}
