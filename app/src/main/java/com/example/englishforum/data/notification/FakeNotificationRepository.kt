package com.example.englishforum.data.notification

import com.example.englishforum.core.model.forum.ForumPostDetail
import com.example.englishforum.core.model.notification.ForumNotification
import com.example.englishforum.core.model.notification.ForumNotificationTarget
import com.example.englishforum.data.post.FakePostStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class FakeNotificationRepository(
    private val store: FakePostStore = FakePostStore
) : NotificationRepository {

    private val readNotificationIds = MutableStateFlow<Set<String>>(emptySet())

    override val notificationsStream: Flow<List<ForumNotification>> = combine(
        store.posts,
        readNotificationIds
    ) { posts, readIds ->
        val notifications = createNotifications(posts)
        val activeReadIds = sanitizeReadIds(readIds, notifications)

        notifications.map { notification ->
            val targetReadState = notification.id in activeReadIds
            if (notification.isRead == targetReadState) {
                notification
            } else {
                notification.copy(isRead = targetReadState)
            }
        }
    }

    override suspend fun markNotificationAsRead(notificationId: String) {
        readNotificationIds.update { current -> current + notificationId }
    }

    override suspend fun markAllAsRead() {
        val allIds = createNotifications(store.posts.value).map { it.id }.toSet()
        if (allIds.isEmpty()) return
        readNotificationIds.update { current -> current + allIds }
    }

    private fun createNotifications(posts: List<ForumPostDetail>): List<ForumNotification> {
        val list = mutableListOf<ForumNotification>()

        val postOne = posts.firstOrNull { it.id == "post-1" }
        val commentOne = postOne?.comments?.firstOrNull { it.id == "comment-1" }
        val commentBottom = postOne?.comments?.firstOrNull { it.id == "comment-19" }

        if (postOne != null && commentOne != null) {
            list += ForumNotification(
                id = "noti-1",
                actorName = commentOne.authorName,
                actorAvatarUrl = null,
                title = "${commentOne.authorName} đã bình luận bài viết của bạn",
                description = "\"${commentOne.body.take(140)}\"",
                minutesAgo = 9,
                target = ForumNotificationTarget.Comment(
                    commentId = commentOne.id,
                    postId = postOne.id
                )
            )
        }

        val commentMiddle = postOne?.comments?.firstOrNull { it.id == "comment-14" }
        if (postOne != null && commentMiddle != null) {
            list += ForumNotification(
                id = "noti-middle",
                actorName = commentMiddle.authorName,
                actorAvatarUrl = null,
                title = "${commentMiddle.authorName} đã nhắc bạn trong bình luận",
                description = "\"${commentMiddle.body.take(120)}\"",
                minutesAgo = 7,
                target = ForumNotificationTarget.Comment(
                    commentId = commentMiddle.id,
                    postId = postOne.id
                )
            )
        }

        if (postOne != null && commentBottom != null) {
            list += ForumNotification(
                id = "noti-bottom",
                actorName = commentBottom.authorName,
                actorAvatarUrl = null,
                title = "${commentBottom.authorName} đã trả lời bình luận của bạn",
                description = "\"${commentBottom.body.take(140)}\"",
                minutesAgo = 3,
                target = ForumNotificationTarget.Comment(
                    commentId = commentBottom.id,
                    postId = postOne.id
                )
            )
        }

        val postTwo = posts.firstOrNull { it.id == "post-2" }
        val commentThree = postTwo?.comments?.firstOrNull { it.id == "comment-3" }
        if (postTwo != null && commentThree != null) {
            list += ForumNotification(
                id = "noti-2",
                actorName = commentThree.authorName,
                actorAvatarUrl = null,
                title = "${commentThree.authorName} đã nhắc bạn trong bình luận",
                description = "${commentThree.authorName}: ${commentThree.body.take(120)}",
                minutesAgo = 58,
                target = ForumNotificationTarget.Comment(
                    commentId = commentThree.id,
                    postId = postTwo.id
                )
            )
        }

        val postThree = posts.firstOrNull { it.id == "post-3" }
        if (postThree != null) {
            list += ForumNotification(
                id = "noti-3",
                actorName = postThree.authorName,
                actorAvatarUrl = null,
                title = "${postThree.authorName} vừa đăng bài mới",
                description = postThree.title,
                minutesAgo = 120,
                target = ForumNotificationTarget.Post(postThree.id)
            )
        }

        return if (list.isNotEmpty()) {
            list
        } else {
            listOf(
                ForumNotification(
                    id = "noti-fallback",
                    actorName = "English Forum",
                    actorAvatarUrl = null,
                    title = "Không có thông báo nào",
                    description = "Bạn sẽ thấy cập nhật mới nhất ở đây",
                    minutesAgo = 0,
                    target = ForumNotificationTarget.Post(postId = "post-1")
                )
            )
        }
    }

    private fun sanitizeReadIds(
        readIds: Set<String>,
        notifications: List<ForumNotification>
    ): Set<String> {
        if (readIds.isEmpty()) return emptySet()
        val existingIds = notifications.mapTo(mutableSetOf()) { it.id }
        return readIds.filterTo(mutableSetOf()) { it in existingIds }
    }
}
