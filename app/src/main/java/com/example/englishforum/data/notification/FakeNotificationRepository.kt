package com.example.englishforum.data.notification

import com.example.englishforum.data.post.FakePostStore
import com.example.englishforum.data.post.PostDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FakeNotificationRepository(
    private val store: FakePostStore = FakePostStore
) : NotificationRepository {

    override val notificationsStream: Flow<List<NotificationMessage>> = store.posts.map { posts ->
        createNotifications(posts)
    }

    private fun createNotifications(posts: List<PostDetail>): List<NotificationMessage> {
        val list = mutableListOf<NotificationMessage>()

        val postOne = posts.firstOrNull { it.id == "post-1" }
        val commentOne = postOne?.comments?.firstOrNull { it.id == "comment-1" }
        val commentBottom = postOne?.comments?.firstOrNull { it.id == "comment-19" }
        
        // Add notification for the first comment
        if (postOne != null && commentOne != null) {
            list += NotificationMessage(
                id = "noti-1",
                actorName = commentOne.authorName,
                title = "${commentOne.authorName} đã bình luận bài viết của bạn",
                description = "\"${commentOne.body.take(140)}\"",
                minutesAgo = 9,
                target = NotificationTarget.Comment(postOne.id, commentOne.id)
            )
        }
        
        // Add notification for a middle comment to test scrolling
        val commentMiddle = postOne?.comments?.firstOrNull { it.id == "comment-14" }
        if (postOne != null && commentMiddle != null) {
            list += NotificationMessage(
                id = "noti-middle",
                actorName = commentMiddle.authorName,
                title = "${commentMiddle.authorName} đã nhắc bạn trong bình luận",
                description = "\"${commentMiddle.body.take(120)}\"",
                minutesAgo = 7,
                target = NotificationTarget.Comment(postOne.id, commentMiddle.id)
            )
        }
        
        // Add notification for a bottom comment to test scrolling
        if (postOne != null && commentBottom != null) {
            list += NotificationMessage(
                id = "noti-bottom",
                actorName = commentBottom.authorName,
                title = "${commentBottom.authorName} đã trả lời bình luận của bạn",
                description = "\"${commentBottom.body.take(140)}\"",
                minutesAgo = 3,
                target = NotificationTarget.Comment(postOne.id, commentBottom.id)
            )
        }

        val postTwo = posts.firstOrNull { it.id == "post-2" }
        val commentThree = postTwo?.comments?.firstOrNull { it.id == "comment-3" }
        if (postTwo != null && commentThree != null) {
            list += NotificationMessage(
                id = "noti-2",
                actorName = commentThree.authorName,
                title = "${commentThree.authorName} đã nhắc bạn trong bình luận",
                description = "${commentThree.authorName}: ${commentThree.body.take(120)}",
                minutesAgo = 58,
                target = NotificationTarget.Comment(postTwo.id, commentThree.id)
            )
        }

        val postThree = posts.firstOrNull { it.id == "post-3" }
        if (postThree != null) {
            list += NotificationMessage(
                id = "noti-3",
                actorName = postThree.authorName,
                title = "${postThree.authorName} vừa đăng bài mới",
                description = postThree.title,
                minutesAgo = 120,
                target = NotificationTarget.Post(postThree.id)
            )
        }

        return if (list.isNotEmpty()) {
            list
        } else {
            listOf(
                NotificationMessage(
                    id = "noti-fallback",
                    actorName = "English Forum",
                    title = "Không có thông báo nào",
                    description = "Bạn sẽ thấy cập nhật mới nhất ở đây",
                    minutesAgo = 0,
                    target = NotificationTarget.Post(postId = "post-1")
                )
            )
        }
    }
}
