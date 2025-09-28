package com.example.englishforum.feature.noti

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.common.formatRelativeTime
import com.example.englishforum.data.notification.FakeNotificationRepository
import com.example.englishforum.data.notification.NotificationMessage
import com.example.englishforum.data.notification.NotificationRepository
import com.example.englishforum.data.notification.NotificationTarget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NotiViewModel(
    private val repository: NotificationRepository = FakeNotificationRepository()
) : ViewModel() {

    val uiState: StateFlow<NotificationUiState> = repository.notificationsStream
        .map { notifications ->
            NotificationUiState(
                isLoading = false,
                notifications = notifications.map { it.toUiModel() }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = NotificationUiState()
        )

    private fun NotificationMessage.toUiModel(): NotificationItemUi {
        val initials = actorName
            .trim()
            .split(' ', '_', '-')
            .firstOrNull { it.isNotBlank() }
            ?.firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: actorName.firstOrNull()?.uppercaseChar()?.toString()
            ?: "?"

        val (postId, commentId) = when (target) {
            is NotificationTarget.Post -> target.postId to null
            is NotificationTarget.Comment -> target.postId to target.commentId
        }

        return NotificationItemUi(
            id = id,
            actorInitial = initials,
            headline = title,
            supportingText = description,
            timestampText = formatRelativeTime(minutesAgo),
            postId = postId,
            commentId = commentId
        )
    }
}

data class NotificationUiState(
    val isLoading: Boolean = true,
    val notifications: List<NotificationItemUi> = emptyList()
)

data class NotificationItemUi(
    val id: String,
    val actorInitial: String,
    val headline: String,
    val supportingText: String,
    val timestampText: String,
    val postId: String,
    val commentId: String?
)
