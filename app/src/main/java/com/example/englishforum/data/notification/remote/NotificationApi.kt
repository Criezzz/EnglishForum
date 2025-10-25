package com.example.englishforum.data.notification.remote

import com.example.englishforum.data.notification.remote.model.NotificationResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {
    @GET("/notifications")
    suspend fun getNotifications(
        @Header("Authorization") bearer: String,
        @Query("cursor") cursorIso: String? = null
    ): List<NotificationResponse>

    @PUT("/notifications/{notification_id}")
    suspend fun markAsRead(
        @Header("Authorization") bearer: String,
        @Path("notification_id") notificationId: Int
    )
}
