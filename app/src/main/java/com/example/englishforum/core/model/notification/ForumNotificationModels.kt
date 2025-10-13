package com.example.englishforum.core.model.notification

data class ForumNotification(
    val id: String,
    val actorName: String,
    val title: String,
    val description: String,
    val minutesAgo: Int,
    val target: ForumNotificationTarget,
    val isRead: Boolean = false
)

sealed interface ForumNotificationTarget {
    data class Post(val postId: String) : ForumNotificationTarget
    data class Comment(val postId: String, val commentId: String) : ForumNotificationTarget
}
