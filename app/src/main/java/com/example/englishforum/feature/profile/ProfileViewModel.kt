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
                    id = "post_1",
                    title = "Lorem ipsum dolor sit amet",
                    body = "Nullam justo felis, ullamcorper et lectus non, vestibulum feugiat risus. Aenean lacinia lacus sed erat molestie.",
                    minutesAgo = 13,
                    voteCount = 17,
                    voteState = VoteState.UPVOTED
                ),
                ProfilePost(
                    id = "post_2",
                    title = "In mattis tincidunt mi ac pretium",
                    body = "Nullam euismod urna in arcu mollis, at consectetur ante mattis. Maecenas vel vehicula dolor. Mauris non est lobortis.",
                    minutesAgo = 28,
                    voteCount = 9
                )
            ),
            replies = listOf(
                ProfileReply(
                    id = "reply_1",
                    questionTitle = "Câu hỏi về ABC",
                    body = "Donec dictum rhoncus eros, eget fermentum dui laoreet a. Orci varius natoque penatibus et magnis dis parturient.",
                    minutesAgo = 12,
                    voteCount = 6,
                    voteState = VoteState.UPVOTED
                ),
                ProfileReply(
                    id = "reply_2",
                    questionTitle = "Câu hỏi về XYZ",
                    body = "Morbi ultrices condimentum fermentum. Pellentesque dictum sem eget diam rutrum, non dapibus massa elementum.",
                    minutesAgo = 45,
                    voteCount = 4
                )
            ),
            isLoading = false
        )
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
