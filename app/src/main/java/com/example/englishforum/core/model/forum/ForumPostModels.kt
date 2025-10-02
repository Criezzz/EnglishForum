package com.example.englishforum.core.model.forum

import com.example.englishforum.core.model.VoteState

data class ForumPostSummary(
    val id: String,
    val authorName: String,
    val minutesAgo: Int,
    val title: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val commentCount: Int
)

data class ForumComment(
    val id: String,
    val authorName: String,
    val minutesAgo: Int,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val isAuthor: Boolean = false
)

data class ForumPostDetail(
    val id: String,
    val authorName: String,
    val minutesAgo: Int,
    val title: String,
    val body: String,
    val voteCount: Int,
    val voteState: VoteState,
    val comments: List<ForumComment>
) {
    val commentCount: Int = comments.size
}
