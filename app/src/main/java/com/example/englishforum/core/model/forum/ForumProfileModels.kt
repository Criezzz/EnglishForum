package com.example.englishforum.core.model.forum

import com.example.englishforum.core.model.VoteState

data class ForumProfileStats(
    val upvotes: Int,
    val posts: Int,
    val answers: Int
)

data class ForumProfilePost(
    val id: String,
    val title: String,
    val body: String,
    val minutesAgo: Int,
    val voteCount: Int,
    val voteState: VoteState
)

data class ForumProfileReply(
    val id: String,
    val postId: String,
    val questionTitle: String,
    val body: String,
    val minutesAgo: Int,
    val voteCount: Int,
    val voteState: VoteState
)

data class ForumUserProfile(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val stats: ForumProfileStats,
    val posts: List<ForumProfilePost>,
    val replies: List<ForumProfileReply>
)
