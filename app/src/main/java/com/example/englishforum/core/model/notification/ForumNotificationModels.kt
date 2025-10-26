package com.example.englishforum.core.model.notification

data class ForumNotification(
    val id: String,
    val actorName: String,
    val actorAvatarUrl: String? = null,
    val title: String,
    val description: String? = null,
    val minutesAgo: Int,
    val target: ForumNotificationTarget,
    val isRead: Boolean = false
)

sealed interface ForumNotificationTarget {
    data class Post(val postId: String) : ForumNotificationTarget
    data class Comment(val commentId: String, val postId: String? = null) : ForumNotificationTarget
    data class Unknown(
        val rawTargetType: String?,
        val targetId: String?,
        val actionId: String?
    ) : ForumNotificationTarget
}
