package com.example.englishforum.data.create

import com.example.englishforum.core.model.VoteState
import com.example.englishforum.data.post.FakePostStore
import com.example.englishforum.data.post.PostDetail
import java.util.UUID
import kotlin.random.Random

class FakeCreatePostRepository(
    private val postStore: FakePostStore = FakePostStore
) : CreatePostRepository {

    private val random = Random(System.currentTimeMillis())
    private val declineReasons = listOf(
        "Nội dung bài viết chứa thông tin chưa phù hợp.",
        "Bài viết cần rõ ràng hơn trước khi đăng.",
        "Bài viết có dấu hiệu vi phạm quy định cộng đồng."
    )

    override suspend fun submitPost(
        title: String,
        body: String,
        attachments: List<CreatePostAttachment>
    ): Result<CreatePostResult> {
        val trimmedTitle = title.trim()
        val trimmedBody = body.trim()

        if (trimmedTitle.isEmpty() || trimmedBody.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tiêu đề và nội dung không được để trống"))
        }

        val shouldDecline = random.nextFloat() < 0.35f
        if (shouldDecline) {
            val reason = declineReasons[random.nextInt(declineReasons.size)]
            return Result.success(CreatePostResult.Declined(reason))
        }

        val newPostId = generatePostId()
        val newPost = PostDetail(
            id = newPostId,
            authorName = "bạn",
            minutesAgo = 0,
            title = trimmedTitle,
            body = trimmedBody,
            voteCount = 0,
            voteState = VoteState.NONE,
            commentCount = 0,
            comments = emptyList()
        )
        postStore.addPost(newPost)

        return Result.success(CreatePostResult.Success(newPostId))
    }

    private fun generatePostId(): String {
        return "post-${UUID.randomUUID()}"
    }
}
