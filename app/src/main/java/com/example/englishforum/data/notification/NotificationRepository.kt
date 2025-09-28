package com.example.englishforum.data.notification

import kotlinx.coroutines.flow.Flow

data class NotificationMessage(
    val id: String,
    val actorName: String,
    val title: String,
    val description: String,
    val minutesAgo: Int,
    val target: NotificationTarget
)

sealed interface NotificationTarget {
    data class Post(val postId: String) : NotificationTarget
    data class Comment(val postId: String, val commentId: String) : NotificationTarget
}

interface NotificationRepository {
    val notificationsStream: Flow<List<NotificationMessage>>
}
