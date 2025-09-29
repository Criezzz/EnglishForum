package com.example.englishforum.feature.profile

import androidx.lifecycle.ViewModel
import com.example.englishforum.core.model.VoteState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        _uiState.value = ProfileUiState(
            overview = ProfileOverview(
                displayName = "A_Great_Name",
                stats = ProfileStats(
                    upvotes = 57,
                    posts = 12,
                    answers = 25
                )
            ),
            posts = listOf(
                ProfilePost(
                    id = "post-1",
                    title = "Lorem ipsum dolor sit amet",
                    body = "Nullam justo felis, ullamcorper et lectus non, vestibulum feugiat risus. Aenean lacinia lacus sed erat molestie.",
                    minutesAgo = 13,
                    voteCount = 17,
                    voteState = VoteState.UPVOTED
                ),
                ProfilePost(
                    id = "post-2",
                    title = "In mattis tincidunt mi ac pretium",
                    body = "Nullam euismod urna in arcu mollis, at consectetur ante mattis. Maecenas vel vehicula dolor. Mauris non est lobortis.",
                    minutesAgo = 28,
                    voteCount = 9
                )
            ),
            replies = listOf(
                ProfileReply(
                    id = "comment-1",
                    postId = "post-1",
                    questionTitle = "Cách luyện phát âm tiếng Anh hàng ngày",
                    body = "Sed vulputate tellus magna, ac fringilla ipsum ornare in. Mình thường ghi âm lại rồi so sánh với bản chuẩn để thấy lỗi phát âm.",
                    minutesAgo = 12,
                    voteCount = 6,
                    voteState = VoteState.UPVOTED
                ),
                ProfileReply(
                    id = "comment-14",
                    postId = "post-1",
                    questionTitle = "Cách luyện phát âm tiếng Anh hàng ngày",
                    body = "Mình khuyên các bạn nên tập phát âm theo từng phoneme trước, rồi mới lên word level. Có thể dùng Cambridge Dictionary để nghe chuẩn âm Anh và Mỹ.",
                    minutesAgo = 30,
                    voteCount = 15,
                    voteState = VoteState.UPVOTED
                ),
                ProfileReply(
                    id = "comment-19",
                    postId = "post-1",
                    questionTitle = "Cách luyện phát âm tiếng Anh hàng ngày",
                    body = "Reading aloud mỗi ngày 20-30 phút với newspaper hoặc novel cũng rất hiệu quả. Quan trọng là consistency và patience!",
                    minutesAgo = 5,
                    voteCount = 13,
                    voteState = VoteState.UPVOTED
                ),
                ProfileReply(
                    id = "comment-3",
                    postId = "post-2",
                    questionTitle = "Chia sẻ tài liệu IELTS Reading band 7+",
                    body = "Great compilation! I usually start learners with Cambridge 15.",
                    minutesAgo = 45,
                    voteCount = 11,
                    voteState = VoteState.UPVOTED
                )
            ),
            isLoading = false
        )
    }

    fun updateDisplayName(newName: String) {
        _uiState.update { current ->
            val overview = current.overview
            if (overview == null) {
                current
            } else {
                current.copy(
                    overview = overview.copy(displayName = newName)
                )
            }
        }
    }

    fun onPostUpvote(postId: String) {
        updatePostVote(postId, VoteState.UPVOTED)
    }

    fun onPostDownvote(postId: String) {
        updatePostVote(postId, VoteState.DOWNVOTED)
    }

    fun onReplyUpvote(replyId: String) {
        updateReplyVote(replyId, VoteState.UPVOTED)
    }

    fun onReplyDownvote(replyId: String) {
        updateReplyVote(replyId, VoteState.DOWNVOTED)
    }

    private fun updatePostVote(postId: String, targetState: VoteState) {
        _uiState.update { current ->
            val updatedPosts = current.posts.map { post ->
                if (post.id == postId) {
                    val (nextState, delta) = resolveVoteChange(post.voteState, targetState)
                    post.copy(
                        voteState = nextState,
                        voteCount = post.voteCount + delta
                    )
                } else {
                    post
                }
            }
            current.copy(posts = updatedPosts)
        }
    }

    private fun updateReplyVote(replyId: String, targetState: VoteState) {
        _uiState.update { current ->
            val updatedReplies = current.replies.map { reply ->
                if (reply.id == replyId) {
                    val (nextState, delta) = resolveVoteChange(reply.voteState, targetState)
                    reply.copy(
                        voteState = nextState,
                        voteCount = reply.voteCount + delta
                    )
                } else {
                    reply
                }
            }
            current.copy(replies = updatedReplies)
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
}
