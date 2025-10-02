package com.example.englishforum.data.notification

import com.example.englishforum.core.model.notification.ForumNotification
import com.example.englishforum.core.model.notification.ForumNotificationTarget
import com.example.englishforum.core.model.forum.ForumPostDetail
import com.example.englishforum.data.post.FakePostStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeNotificationRepository(
    private val store: FakePostStore = FakePostStore
) : NotificationRepository {

    override val notificationsStream: Flow<List<ForumNotification>> = store.posts.map { posts ->
        createNotifications(posts)
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
                title = "${commentOne.authorName} đã bình luận bài viết của bạn",
                description = "\"${commentOne.body.take(140)}\"",
                minutesAgo = 9,
                target = ForumNotificationTarget.Comment(postOne.id, commentOne.id)
            )
        }

        val commentMiddle = postOne?.comments?.firstOrNull { it.id == "comment-14" }
        if (postOne != null && commentMiddle != null) {
            list += ForumNotification(
                id = "noti-middle",
                actorName = commentMiddle.authorName,
                title = "${commentMiddle.authorName} đã nhắc bạn trong bình luận",
                description = "\"${commentMiddle.body.take(120)}\"",
                minutesAgo = 7,
                target = ForumNotificationTarget.Comment(postOne.id, commentMiddle.id)
            )
        }

        if (postOne != null && commentBottom != null) {
            list += ForumNotification(
                id = "noti-bottom",
                actorName = commentBottom.authorName,
                title = "${commentBottom.authorName} đã trả lời bình luận của bạn",
                description = "\"${commentBottom.body.take(140)}\"",
                minutesAgo = 3,
                target = ForumNotificationTarget.Comment(postOne.id, commentBottom.id)
            )
        }

        val postTwo = posts.firstOrNull { it.id == "post-2" }
        val commentThree = postTwo?.comments?.firstOrNull { it.id == "comment-3" }
        if (postTwo != null && commentThree != null) {
            list += ForumNotification(
                id = "noti-2",
                actorName = commentThree.authorName,
                title = "${commentThree.authorName} đã nhắc bạn trong bình luận",
                description = "${commentThree.authorName}: ${commentThree.body.take(120)}",
                minutesAgo = 58,
                target = ForumNotificationTarget.Comment(postTwo.id, commentThree.id)
            )
        }

        val postThree = posts.firstOrNull { it.id == "post-3" }
        if (postThree != null) {
            list += ForumNotification(
                id = "noti-3",
                actorName = postThree.authorName,
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
                    title = "Không có thông báo nào",
                    description = "Bạn sẽ thấy cập nhật mới nhất ở đây",
                    minutesAgo = 0,
                    target = ForumNotificationTarget.Post(postId = "post-1")
                )
            )
        }
    }
}
