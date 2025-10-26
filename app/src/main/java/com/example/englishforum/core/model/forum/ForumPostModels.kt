package com.example.englishforum.core.model.forum

import com.example.englishforum.core.model.VoteState

enum class PostTag(val serverValue: String) {
    Tutorial("tutorial"),
    AskQuestion("question"),
    Resource("resource"),
    Experience("experience");

    companion object {
        fun fromServerValue(value: String): PostTag? {
            return values().firstOrNull { it.serverValue.equals(value, ignoreCase = true) }
        }
    }
}

data class ForumPostSummary(
    val id: String,
    val authorName: String,
    val authorUsername: String? = null,
    val minutesAgo: Int,
    val title: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val commentCount: Int,
    val tag: PostTag,
    val authorAvatarUrl: String? = null,
    val previewImageUrl: String? = null
)

data class ForumComment(
    val id: String,
    val authorId: String? = null,
    val authorName: String,
    val authorUsername: String? = null,
    val minutesAgo: Int,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val isAuthor: Boolean = false,
    val replies: List<ForumComment> = emptyList()
)

data class ForumPostDetail(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String? = null,
    val minutesAgo: Int,
    val title: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val comments: List<ForumComment>,
    val tag: PostTag,
    val authorAvatarUrl: String? = null,
    val previewImageUrl: String? = null,
    val galleryImages: List<String>? = null
) {
    val commentCount: Int = comments.sumOf { it.totalThreadCount() }
}

private fun ForumComment.totalThreadCount(): Int {
    return 1 + replies.sumOf { it.totalThreadCount() }
}
