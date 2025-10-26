package com.example.englishforum.data.notification.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotificationResponse(
    @Json(name = "notification_id")
    val notificationId: Int,
    @Json(name = "actor_username")
    val actorUsername: String?,
    @Json(name = "actor_avatar")
    val actorAvatar: String?,
    @Json(name = "action_type")
    val actionType: String?,
    @Json(name = "action_id")
    val actionId: Int?,
    @Json(name = "target_type")
    val targetType: String?,
    @Json(name = "target_id")
    val targetId: Int?,
    @Json(name = "is_read")
    val isRead: Boolean?,
    @Json(name = "created_at")
    val createdAt: String? = null
)
