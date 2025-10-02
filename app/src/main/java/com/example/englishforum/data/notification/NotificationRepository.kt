package com.example.englishforum.data.notification

import com.example.englishforum.core.model.notification.ForumNotification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    val notificationsStream: Flow<List<ForumNotification>>
}
