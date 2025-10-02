package com.example.englishforum.feature.noti

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.common.formatRelativeTime
import com.example.englishforum.core.model.notification.ForumNotification
import com.example.englishforum.core.model.notification.ForumNotificationTarget
import com.example.englishforum.data.notification.FakeNotificationRepository
import com.example.englishforum.data.notification.NotificationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class NotiViewModel(
    private val repository: NotificationRepository
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

    private fun ForumNotification.toUiModel(): NotificationItemUi {
        val initials = actorName
            .trim()
            .split(' ', '_', '-')
            .firstOrNull { it.isNotBlank() }
            ?.firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: actorName.firstOrNull()?.uppercaseChar()?.toString()
            ?: "?"

        val (postId, commentId) = when (val target = target) {
            is ForumNotificationTarget.Post -> target.postId to null
            is ForumNotificationTarget.Comment -> target.postId to target.commentId
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

class NotiViewModelFactory(
    private val repository: NotificationRepository = FakeNotificationRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotiViewModel::class.java)) {
            return NotiViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
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
