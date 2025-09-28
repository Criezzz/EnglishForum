package com.example.englishforum.data.home

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.data.post.FakePostStore
import com.example.englishforum.data.post.PostDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeHomeRepository(
    private val store: FakePostStore = FakePostStore
) : HomeRepository {

    override val postsStream: Flow<List<HomePost>> = store.posts.map { posts ->
        posts.map { detail -> detail.toHomePost() }
    }

    override suspend fun setVoteState(postId: String, target: VoteState): Result<Unit> {
        return if (store.updatePostVote(postId, target)) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Post not found"))
        }
    }

    private fun PostDetail.toHomePost(): HomePost {
        return HomePost(
            id = id,
            authorName = authorName,
            minutesAgo = minutesAgo,
            title = title,
            body = body,
            voteCount = voteCount,
            voteState = voteState,
            commentCount = commentCount
        )
    }
}
