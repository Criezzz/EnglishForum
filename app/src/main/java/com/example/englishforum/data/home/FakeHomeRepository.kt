package com.example.englishforum.data.home

import com.example.englishforum.core.model.VoteState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeHomeRepository : HomeRepository {

    private val _posts = MutableStateFlow(createInitialPosts())
    override val postsStream: StateFlow<List<HomePost>> = _posts.asStateFlow()

    override suspend fun setVoteState(postId: String, target: VoteState): Result<Unit> {
        var foundPost = false
        _posts.update { posts ->
            posts.map { post ->
                if (post.id == postId) {
                    foundPost = true
                    val (nextState, delta) = resolveVoteChange(post.voteState, target)
                    post.copy(
                        voteState = nextState,
                        voteCount = post.voteCount + delta
                    )
                } else {
                    post
                }
            }
        }

        return if (foundPost) {
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Post not found"))
        }
    }

    private fun resolveVoteChange(currentState: VoteState, targetState: VoteState): Pair<VoteState, Int> {
        val nextState = if (currentState == targetState) {
            VoteState.NONE
        } else {
            targetState
        }

        val delta = when (currentState to nextState) {
            VoteState.NONE to VoteState.UPVOTED -> 1
            VoteState.NONE to VoteState.DOWNVOTED -> -1
            VoteState.UPVOTED to VoteState.NONE -> -1
            VoteState.DOWNVOTED to VoteState.NONE -> 1
            VoteState.UPVOTED to VoteState.DOWNVOTED -> -2
            VoteState.DOWNVOTED to VoteState.UPVOTED -> 2
            else -> 0
        }

        return nextState to delta
    }

    private fun createInitialPosts(): List<HomePost> = listOf(
        HomePost(
            id = "post-1",
            authorName = "Jane_Doe",
            minutesAgo = 12,
            title = "Cách luyện phát âm tiếng Anh hàng ngày",
            body = "Mọi người có mẹo nào luyện phát âm hiệu quả khi không có người chỉnh lỗi không?",
            voteCount = 23,
            voteState = VoteState.NONE,
            commentCount = 8
        ),
        HomePost(
            id = "post-2",
            authorName = "Mr.Teacher",
            minutesAgo = 38,
            title = "Chia sẻ tài liệu IELTS Reading band 7+",
            body = "Mình tổng hợp vài tài liệu tự học khá hay, mọi người tải về tham khảo thử nhé.",
            voteCount = 54,
            voteState = VoteState.NONE,
            commentCount = 15
        ),
        HomePost(
            id = "post-3",
            authorName = "lisa.ng",
            minutesAgo = 51,
            title = "Nên học từ vựng theo chủ đề hay danh sách 3000 từ?",
            body = "Đang phân vân không biết nên follow list nào để học cho hiệu quả hơn.",
            voteCount = 11,
            voteState = VoteState.UPVOTED,
            commentCount = 4
        ),
        HomePost(
            id = "post-4",
            authorName = "NguyenVanA",
            minutesAgo = 89,
            title = "Các app luyện nghe miễn phí tốt nhất hiện nay",
            body = "Mình thử qua vài app như Elsa, Cake... có app nào mọi người thấy hay hơn không?",
            voteCount = 31,
            voteState = VoteState.NONE,
            commentCount = 12
        ),
        HomePost(
            id = "post-5",
            authorName = "studybuddy",
            minutesAgo = 143,
            title = "Looking for a speaking partner",
            body = "We can practice on Zoom 2-3h/week, ai quan tâm để lại comment nha!",
            voteCount = 27,
            voteState = VoteState.DOWNVOTED,
            commentCount = 19
        )
    )
}
