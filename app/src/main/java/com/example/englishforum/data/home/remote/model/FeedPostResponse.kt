package com.example.englishforum.data.home.remote.model

import com.example.englishforum.data.post.remote.model.AttachmentResponse
import com.squareup.moshi.Json

data class FeedPostResponse(
    @Json(name = "post_id") val postId: Int,
    val title: String = "",
    val content: String = "",
    @Json(name = "vote_count") val voteCount: Int = 0,
    @Json(name = "user_vote") val userVote: Int = 0,
    @Json(name = "comment_count") val commentCount: Int? = null,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "is_modified") val isModified: Boolean? = null,
    val tag: String? = null,
    @Json(name = "author_username") val authorUsername: String? = null,
    @Json(name = "author_display_name") val authorDisplayName: String? = null,
    @Json(name = "author_name") val authorName: String? = null,
    @Json(name = "author_avatar_url") val authorAvatarUrl: String? = null,
    @Json(name = "author_avatar") val authorAvatar: String? = null,
    val attachments: List<AttachmentResponse>? = null
)
