package com.example.englishforum.data.profile

import com.example.englishforum.core.common.resolveVoteChange
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumProfilePost
import com.example.englishforum.core.model.forum.ForumProfileReply
import com.example.englishforum.core.model.forum.ForumProfileStats
import com.example.englishforum.core.model.forum.ForumUserProfile
import com.example.englishforum.data.profile.ProfileAvatarImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakeProfileRepository : ProfileRepository {

    private val profileState = MutableStateFlow(createInitialProfile())

    override fun observeProfile(userId: String): Flow<ForumUserProfile> {
        return profileState.asStateFlow()
    }

    override suspend fun updateDisplayName(userId: String, displayName: String): Result<Unit> {
        profileState.update { current -> current.copy(displayName = displayName) }
        return Result.success(Unit)
    }

    override suspend fun updateBio(userId: String, bio: String): Result<Unit> {
        profileState.update { current -> current.copy(bio = bio) }
        return Result.success(Unit)
    }

    override suspend fun updateAvatar(userId: String, avatar: ProfileAvatarImage): Result<Unit> {
        profileState.update { current -> current.copy(avatarUrl = avatar.uri.toString()) }
        return Result.success(Unit)
    }

    override suspend fun setPostVote(userId: String, postId: String, target: VoteState): Result<Unit> {
        var updated = false
        profileState.update { current ->
            val posts = current.posts.map { post ->
                if (post.id == postId) {
                    updated = true
                    val (nextState, delta) = resolveVoteChange(post.voteState, target)
                    post.copy(
                        voteState = nextState,
                        voteCount = post.voteCount + delta
                    )
                } else {
                    post
                }
            }
            current.copy(posts = posts)
        }
        return if (updated) Result.success(Unit) else Result.failure(IllegalArgumentException("Post not found"))
    }

    override suspend fun setReplyVote(userId: String, replyId: String, target: VoteState): Result<Unit> {
        var updated = false
        profileState.update { current ->
            val replies = current.replies.map { reply ->
                if (reply.id == replyId) {
                    updated = true
                    val (nextState, delta) = resolveVoteChange(reply.voteState, target)
                    reply.copy(
                        voteState = nextState,
                        voteCount = reply.voteCount + delta
                    )
                } else {
                    reply
                }
            }
            current.copy(replies = replies)
        }
        return if (updated) Result.success(Unit) else Result.failure(IllegalArgumentException("Reply not found"))
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        // Fake implementation - just return success
        return Result.success(Unit)
    }

    override suspend fun updateEmail(newEmail: String): Result<Unit> {
        // Fake implementation - just return success
        return Result.success(Unit)
    }

    override suspend fun confirmEmailUpdate(otp: String): Result<Unit> {
        // Fake implementation - just return success
        return Result.success(Unit)
    }

    private fun createInitialProfile(): ForumUserProfile {
        return ForumUserProfile(
            userId = "user-1",
            displayName = "A_Great_Name",
            avatarUrl = null,
            bio = "Certified English tutor who loves helping learners build confidence in conversation.",
            stats = ForumProfileStats(
                upvotes = 57,
                posts = 12,
                answers = 25
            ),
            posts = listOf(
                ForumProfilePost(
                    id = "post-1",
                    title = "Lorem ipsum dolor sit amet",
                    body = "Nullam justo felis, ullamcorper et lectus non, vestibulum feugiat risus. Aenean lacinia lacus sed erat molestie.",
                    timestampLabel = "13 phút trước",
                    voteCount = 17,
                    voteState = VoteState.UPVOTED
                ),
                ForumProfilePost(
                    id = "post-2",
                    title = "In mattis tincidunt mi ac pretium",
                    body = "Nullam euismod urna in arcu mollis, at consectetur ante mattis. Maecenas vel vehicula dolor. Mauris non est lobortis.",
                    timestampLabel = "12/05/2024 14:30",
                    voteCount = 9,
                    voteState = VoteState.NONE
                )
            ),
            replies = listOf(
                ForumProfileReply(
                    id = "comment-1",
                    postId = "post-1",
                    questionTitle = "Cách luyện phát âm tiếng Anh hàng ngày",
                    body = "Sed vulputate tellus magna, ac fringilla ipsum ornare in. Mình thường ghi âm lại rồi so sánh với bản chuẩn để thấy lỗi phát âm.",
                    timestampLabel = "12 phút trước",
                    voteCount = 6,
                    voteState = VoteState.UPVOTED
                ),
                ForumProfileReply(
                    id = "comment-14",
                    postId = "post-1",
                    questionTitle = "Cách luyện phát âm tiếng Anh hàng ngày",
                    body = "Mình khuyên các bạn nên tập phát âm theo từng phoneme trước, rồi mới lên word level. Có thể dùng Cambridge Dictionary để nghe chuẩn âm Anh và Mỹ.",
                    timestampLabel = "11/05/2024 08:45",
                    voteCount = 15,
                    voteState = VoteState.UPVOTED
                ),
                ForumProfileReply(
                    id = "comment-19",
                    postId = "post-1",
                    questionTitle = "Cách luyện phát âm tiếng Anh hàng ngày",
                    body = "Reading aloud mỗi ngày 20-30 phút với newspaper hoặc novel cũng rất hiệu quả. Quan trọng là consistency và patience!",
                    timestampLabel = "5 phút trước",
                    voteCount = 13,
                    voteState = VoteState.UPVOTED
                ),
                ForumProfileReply(
                    id = "comment-3",
                    postId = "post-2",
                    questionTitle = "Chia sẻ tài liệu IELTS Reading band 7+",
                    body = "Great compilation! I usually start learners with Cambridge 15.",
                    timestampLabel = "10/05/2024 20:12",
                    voteCount = 11,
                    voteState = VoteState.UPVOTED
                )
            )
        )
    }
}
