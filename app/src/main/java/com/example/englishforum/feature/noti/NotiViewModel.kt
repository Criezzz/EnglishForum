package com.example.englishforum.feature.noti

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.core.common.formatRelativeTime
import com.example.englishforum.core.model.notification.ForumNotification
import com.example.englishforum.core.model.notification.ForumNotificationTarget
import com.example.englishforum.data.notification.FakeNotificationRepository
import com.example.englishforum.data.notification.NotificationRealtimeEvent
import com.example.englishforum.data.notification.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotiViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val realtimePrompt = MutableStateFlow<NotificationRealtimePromptUi?>(null)

    init {
        viewModelScope.launch {
            repository.realtimeEvents.collect { event ->
                when (event) {
                    is NotificationRealtimeEvent.NewNotifications -> onRealtimeNotifications(event.ids.size)
                }
            }
        }
    }

    val uiState: StateFlow<NotificationUiState> = combine(
        repository.notificationsStream,
        refreshing,
        realtimePrompt
    ) { notifications, isRefreshing, prompt ->
        val items = notifications.map { it.toUiModel() }
        NotificationUiState(
            isLoading = false,
            isRefreshing = isRefreshing,
            notifications = items,
            unreadCount = notifications.count { !it.isRead },
            realtimePrompt = prompt
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = NotificationUiState()
        )

    fun markNotificationAsRead(notificationId: String) {
        val alreadyRead = uiState.value.notifications.firstOrNull { it.id == notificationId }?.isRead == true
        if (alreadyRead) return
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    fun markAllNotificationsAsRead() {
        if (uiState.value.unreadCount == 0) return
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun onRefresh() {
        if (refreshing.value) return
        viewModelScope.launch {
            refreshing.value = true
            try {
                repository.refresh()
            } finally {
                refreshing.value = false
            }
        }
    }

    fun onRealtimePromptDismissed(version: Int) {
        val current = realtimePrompt.value
        if (current?.version == version) {
            realtimePrompt.value = null
        }
    }

    fun onRealtimePromptReveal(version: Int) {
        val current = realtimePrompt.value
        if (current?.version == version) {
            realtimePrompt.value = null
        }
    }

    private fun onRealtimeNotifications(newCount: Int) {
        if (newCount <= 0) return
        val current = realtimePrompt.value
        val updatedCount = (current?.count ?: 0) + newCount
        val nextVersion = (current?.version ?: 0) + 1
        realtimePrompt.value = NotificationRealtimePromptUi(
            count = updatedCount,
            version = nextVersion
        )
    }

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

        val postId: String?
        val commentId: String?
        when (val target = target) {
            is ForumNotificationTarget.Post -> {
                postId = target.postId
                commentId = null
            }

            is ForumNotificationTarget.Comment -> {
                postId = target.postId
                commentId = target.commentId
            }

            is ForumNotificationTarget.Unknown -> {
                postId = null
                commentId = null
            }
        }

        return NotificationItemUi(
            id = id,
            actorInitial = initials,
            actorAvatarUrl = actorAvatarUrl,
            headline = title,
            supportingText = description?.takeIf { it.isNotBlank() },
            timestampText = formatRelativeTime(minutesAgo),
            postId = postId,
            commentId = commentId,
            isRead = isRead
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
    val isRefreshing: Boolean = false,
    val notifications: List<NotificationItemUi> = emptyList(),
    val unreadCount: Int = 0,
    val realtimePrompt: NotificationRealtimePromptUi? = null
)

data class NotificationItemUi(
    val id: String,
    val actorInitial: String,
    val actorAvatarUrl: String? = null,
    val headline: String,
    val supportingText: String? = null,
    val timestampText: String,
    val postId: String?,
    val commentId: String?,
    val isRead: Boolean
)

data class NotificationRealtimePromptUi(
    val count: Int,
    val version: Int
)
