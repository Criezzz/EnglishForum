package com.example.englishforum.data.notification

import com.example.englishforum.core.model.notification.ForumNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    val notificationsStream: Flow<List<ForumNotification>>
    val realtimeEvents: Flow<NotificationRealtimeEvent>

    suspend fun refresh(): Result<Unit> = Result.success(Unit)

    suspend fun markNotificationAsRead(notificationId: String)

    suspend fun markAllAsRead()
}

sealed interface NotificationRealtimeEvent {
    data class NewNotifications(val ids: List<String>) : NotificationRealtimeEvent
}
