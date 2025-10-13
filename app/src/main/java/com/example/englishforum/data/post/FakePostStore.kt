package com.example.englishforum.data.post

import com.example.englishforum.core.common.resolveVoteChange
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.ForumComment
import com.example.englishforum.core.model.forum.ForumPostDetail
import com.example.englishforum.core.model.forum.PostTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

object FakePostStore {

    private val _posts = MutableStateFlow(createInitialPosts())
    val posts: StateFlow<List<ForumPostDetail>> = _posts.asStateFlow()

    fun observePost(postId: String): Flow<ForumPostDetail?> {
        return posts.map { list -> list.firstOrNull { it.id == postId } }
    }

    fun addPost(post: ForumPostDetail) {
        _posts.update { current -> listOf(post) + current }
    }

    fun reportPost(postId: String, reason: String): Boolean {
        return _posts.value.any { it.id == postId }
    }

    fun deletePost(postId: String): Boolean {
        var removed = false
        _posts.update { current ->
            val filtered = current.filter { post ->
                val shouldKeep = post.id != postId
                if (!shouldKeep) {
                    removed = true
                }
                shouldKeep
            }
            filtered
        }
        return removed
    }

    fun updatePostContent(
        postId: String,
        title: String,
        body: String,
        tag: PostTag,
        previewImageUrl: String?,
        galleryImageUrls: List<String>
    ): Boolean {
        var updated = false
        val sanitizedGallery = galleryImageUrls.distinct().takeIf { it.isNotEmpty() }
        _posts.update { current ->
            current.map { post ->
                if (post.id == postId) {
                    updated = true
                    post.copy(
                        title = title,
                        body = body,
                        tag = tag,
                        previewImageUrl = previewImageUrl,
                        galleryImages = sanitizedGallery
                    )
                } else {
                    post
                }
            }
        }
        return updated
    }

    fun updatePostVote(postId: String, target: VoteState): Boolean {
        var found = false
        _posts.update { current ->
            current.map { post ->
                if (post.id == postId) {
                    found = true
                    val (nextState, delta) = resolveVoteChange(post.voteState, target)
                    post.copy(
                        voteState = nextState,
                        voteCount = post.voteCount + delta
                    )
                } else {
                    post
                }
            }
        }
        return found
    }

    fun updateCommentVote(postId: String, commentId: String, target: VoteState): Pair<Boolean, Boolean> {
        var postFound = false
        var commentFound = false
        _posts.update { current ->
            current.map { post ->
                if (post.id == postId) {
                    postFound = true
                    val (updatedComments, found) = updateCommentThread(post.comments, commentId, target)
                    if (found) {
                        commentFound = true
                    }
                    post.copy(comments = updatedComments)
                } else {
                    post
                }
            }
        }
        return postFound to commentFound
    }

    private fun updateCommentThread(
        comments: List<ForumComment>,
        commentId: String,
        target: VoteState
    ): Pair<List<ForumComment>, Boolean> {
        var found = false
        val updated = comments.map { comment ->
            when {
                comment.id == commentId -> {
                    found = true
                    val (nextState, delta) = resolveVoteChange(comment.voteState, target)
                    comment.copy(
                        voteState = nextState,
                        voteCount = comment.voteCount + delta
                    )
                }

                comment.replies.isNotEmpty() -> {
                    val (updatedReplies, replyFound) = updateCommentThread(comment.replies, commentId, target)
                    if (replyFound) {
                        found = true
                        comment.copy(replies = updatedReplies)
                    } else {
                        comment
                    }
                }

                else -> comment
            }
        }
        return updated to found
    }

    private fun createInitialPosts(): List<ForumPostDetail> {
        val sampleCommentsPost1 = listOf(
            ForumComment(
                id = "comment-1",
                authorName = "crystal",
                minutesAgo = 6 * 60,
                body = "Sed vulputate tellus magna, ac fringilla ipsum ornare in. Mình thường ghi âm lại rồi so sánh với bản chuẩn để thấy lỗi phát âm. Bạn có thể thử đọc đoạn hội thoại, thu âm từng câu, sau đó nhờ bạn bè góp ý. Nếu tự luyện, nên chia nhỏ từng cụm âm khó và lặp lại nhiều lần.",
                voteCount = 6,
                voteState = VoteState.NONE,
                replies = listOf(
                    ForumComment(
                        id = "comment-1-1",
                        authorName = "reply_bot",
                        minutesAgo = 5 * 60 + 20,
                        body = "Đồng ý! Mình còn dùng thêm app Elsa Speak để đo độ chính xác sau mỗi lần thu.",
                        voteCount = 3,
                        voteState = VoteState.UPVOTED,
                        replies = listOf(
                            ForumComment(
                                id = "comment-1-1-1",
                                authorName = "crystal",
                                minutesAgo = 5 * 60,
                                body = "Elsa Speak chuẩn đó, nhưng nhớ luyện phát âm từng âm trước khi vào bài dài nha!",
                                voteCount = 2,
                                voteState = VoteState.NONE,
                                isAuthor = true
                            )
                        )
                    ),
                    ForumComment(
                        id = "comment-1-2",
                        authorName = "practice_buddy",
                        minutesAgo = 5 * 60,
                        body = "Nếu luyện theo nhóm thì mọi người có tips gì để nhận xét cho nhau không?",
                        voteCount = 1,
                        voteState = VoteState.NONE
                    )
                )
            ),
            ForumComment(
                id = "comment-2",
                authorName = "anotherone",
                minutesAgo = 4 * 60,
                body = "Quisque a lorem vitae ante pretium feugiat id a justo.",
                voteCount = 2,
                voteState = VoteState.NONE
            ),
            ForumComment(
                id = "comment-11",
                authorName = "pronunciation_pro",
                minutesAgo = 3 * 60 + 30,
                body = "Mình thường dùng app Speechify để luyện theo, nó có thể chỉ ra lỗi phát âm và cho điểm. Còn cách truyền thống thì nên tập trước gương để quan sát miệng lưỡi.",
                voteCount = 8,
                voteState = VoteState.UPVOTED
            ),
            ForumComment(
                id = "comment-12",
                authorName = "english_learner_vn",
                minutesAgo = 2 * 60 + 45,
                body = "Có thể tham khảo YouTube channel của Rachel's English. Cô ấy giải thích rất chi tiết về cách di chuyển lưỡi và môi cho từng âm.",
                voteCount = 12,
                voteState = VoteState.NONE,
                replies = listOf(
                    ForumComment(
                        id = "comment-12-1",
                        authorName = "accent_reducer",
                        minutesAgo = 2 * 60 + 20,
                        body = "Chuẩn nè! Bạn cũng nên note lại khẩu hình miệng từng âm để luyện offline.",
                        voteCount = 4,
                        voteState = VoteState.NONE,
                        replies = listOf(
                            ForumComment(
                                id = "comment-12-1-1",
                                authorName = "english_learner_vn",
                                minutesAgo = 2 * 60,
                                body = "Mình đang làm flashcard khẩu hình bằng Notion, ai cần thì hú mình.",
                                voteCount = 2,
                                voteState = VoteState.UPVOTED
                            ),
                            ForumComment(
                                id = "comment-12-1-2",
                                authorName = "coach_mia",
                                minutesAgo = 2 * 60 - 10,
                                body = "Flashcard thì nên in ra và dán trong góc học tập, dễ nhớ hơn á.",
                                voteCount = 1,
                                voteState = VoteState.NONE
                            )
                        )
                    )
                )
            ),
            ForumComment(
                id = "comment-13",
                authorName = "speaking_master",
                minutesAgo = 2 * 60 + 15,
                body = "Shadow reading technique rất hiệu quả! Nghe podcast hoặc audiobook rồi nói theo ngay lập tức, không cần hiểu hết ý nghĩa lúc đầu.",
                voteCount = 5,
                voteState = VoteState.NONE
            ),
            ForumComment(
                id = "comment-14",
                authorName = "ielts_coach",
                minutesAgo = 90,
                body = "Mình khuyên các bạn nên tập phát âm theo từng phoneme trước, rồi mới lên word level. Có thể dùng Cambridge Dictionary để nghe chuẩn âm Anh và Mỹ.",
                voteCount = 15,
                voteState = VoteState.UPVOTED
            ),
            ForumComment(
                id = "comment-15",
                authorName = "practice_buddy",
                minutesAgo = 75,
                body = "Tongue twisters cũng là một cách hay đấy! 'She sells seashells by the seashore' - lặp lại mỗi ngày 10-15 phút.",
                voteCount = 7,
                voteState = VoteState.NONE
            ),
            ForumComment(
                id = "comment-16",
                authorName = "voice_trainer",
                minutesAgo = 60,
                body = "Đừng quên breathing technique! Thở đúng cách sẽ giúp giọng nói tự tin và rõ ràng hơn. Luyện tập hít thở từ bụng, không phải ngực.",
                voteCount = 9,
                voteState = VoteState.NONE
            ),
            ForumComment(
                id = "comment-17",
                authorName = "phonetics_expert",
                minutesAgo = 45,
                body = "International Phonetic Alphabet (IPA) là công cụ rất hữu ích. Học IPA sẽ giúp bạn phát âm chính xác bất kỳ từ nào mà không cần nghe audio.",
                voteCount = 11,
                voteState = VoteState.UPVOTED
            ),
            ForumComment(
                id = "comment-18",
                authorName = "accent_reducer",
                minutesAgo = 30,
                body = "Mọi người có thể thử minimal pairs exercise. Ví dụ: ship/sheep, bit/beat. Tập phân biệt và phát âm các cặp từ này để cải thiện độ chính xác.",
                voteCount = 6,
                voteState = VoteState.NONE
            ),
            ForumComment(
                id = "comment-19",
                authorName = "daily_english",
                minutesAgo = 15,
                body = "Reading aloud mỗi ngày 20-30 phút với newspaper hoặc novel cũng rất hiệu quả. Quan trọng là consistency và patience!",
                voteCount = 13,
                voteState = VoteState.UPVOTED
            )
        )

        val sampleCommentsPost2 = listOf(
            ForumComment(
                id = "comment-3",
                authorName = "mentorX",
                minutesAgo = 32,
                body = "Great compilation! I usually start learners with Cambridge 15.",
                voteCount = 11,
                voteState = VoteState.UPVOTED,
                replies = listOf(
                    ForumComment(
                        id = "comment-3-1",
                        authorName = "reading_addict",
                        minutesAgo = 30,
                        body = "Bạn có gợi ý nào cho band 6-7 không?",
                        voteCount = 2,
                        voteState = VoteState.NONE,
                        replies = listOf(
                            ForumComment(
                                id = "comment-3-1-1",
                                authorName = "mentorX",
                                minutesAgo = 28,
                                body = "Tập trung vào Cambridge 10-13 trước nha, độ khó vừa đủ.",
                                voteCount = 2,
                                voteState = VoteState.UPVOTED,
                                isAuthor = true
                            )
                        )
                    )
                )
            ),
            ForumComment(
                id = "comment-4",
                authorName = "selfstudy",
                minutesAgo = 124,
                body = "Thanks for sharing, saved to my drive already.",
                voteCount = 4,
                voteState = VoteState.NONE
            ),
            ForumComment(
                id = "comment-5",
                authorName = "reader91",
                minutesAgo = 240,
                body = "Do you also have speaking materials?",
                voteCount = 3,
                voteState = VoteState.NONE
            ),
            ForumComment(
                id = "comment-6",
                authorName = "ielts_lover",
                minutesAgo = 18,
                body = "This list is gold!",
                voteCount = 5,
                voteState = VoteState.UPVOTED
            )
        )

        val sampleCommentsPost3 = listOf(
            ForumComment(
                id = "comment-7",
                authorName = "grammar_guru",
                minutesAgo = 55,
                body = "I also struggle with this, tagging along!",
                voteCount = 2,
                voteState = VoteState.NONE
            ),
            ForumComment(
                id = "comment-8",
                authorName = "teacherAnna",
                minutesAgo = 47,
                body = "Start with identifying the main clause, then break down subordinate clauses.",
                voteCount = 7,
                voteState = VoteState.UPVOTED
            ),
            ForumComment(
                id = "comment-9",
                authorName = "reading_addict",
                minutesAgo = 12,
                body = "News articles often have complex sentences, try The Economist.",
                voteCount = 1,
                voteState = VoteState.NONE
            )
        )

        val sampleCommentsPost4 = listOf(
            ForumComment(
                id = "comment-10",
                authorName = "ielts_fighter",
                minutesAgo = 5,
                body = "Band 7 writing is not easy, thanks for sharing!",
                voteCount = 4,
                voteState = VoteState.UPVOTED
            )
        )

        val sampleCommentsPost5 = listOf(
            ForumComment(
                id = "comment-20",
                authorName = "newbie",
                minutesAgo = 80,
                body = "Cảm ơn, mình cũng đang cần tài liệu Speaking.",
                voteCount = 2,
                voteState = VoteState.NONE
            )
        )

        return listOf(
            ForumPostDetail(
                id = "post-1",
                authorId = "demo-user",
                authorName = "linhtran",
                minutesAgo = 45,
                title = "Cách luyện phát âm tiếng Anh hàng ngày",
                body = "Mọi người có kinh nghiệm nào luyện phát âm khi không có người chỉnh lỗi không?",
                voteCount = 128,
                voteState = VoteState.UPVOTED,
                comments = sampleCommentsPost1,
                tag = PostTag.Tutorial,
                authorAvatarUrl = "mock://avatar/linhtran",
                previewImageUrl = "mock://daily-pronunciation",
                galleryImages = listOf(
                    "mock://gallery/pronunciation-1",
                    "mock://gallery/pronunciation-2",
                    "mock://gallery/pronunciation-3"
                )
            ),
            ForumPostDetail(
                id = "post-2",
                authorId = "user-hoangnguyen",
                authorName = "hoangnguyen",
                minutesAgo = 95,
                title = "Chia sẻ tài liệu IELTS Reading band 7+",
                body = "Mình đã tổng hợp vài bộ đề mình thấy hay trong Drive, mời mọi người download.",
                voteCount = 84,
                voteState = VoteState.NONE,
                comments = sampleCommentsPost2,
                tag = PostTag.Resource,
                authorAvatarUrl = "mock://avatar/hoangnguyen",
                previewImageUrl = "mock://ielts-reading-kit"
            ),
            ForumPostDetail(
                id = "post-3",
                authorId = "user-minhchau",
                authorName = "minhchau",
                minutesAgo = 60 * 5,
                title = "Luyện đọc hiểu câu dài như thế nào?",
                body = "Những câu dài trong bài đọc IELTS thực sự khiến mình đau đầu, mọi người có tips nào không?",
                voteCount = 45,
                voteState = VoteState.NONE,
                comments = sampleCommentsPost3,
                tag = PostTag.AskQuestion,
                authorAvatarUrl = "mock://avatar/minhchau"
            ),
            ForumPostDetail(
                id = "post-4",
                authorId = "user-thuyle",
                authorName = "thuyle",
                minutesAgo = 60 * 8,
                title = "Viết Writing Task 2 mở bài ra sao cho hấp dẫn?",
                body = "Mình luôn mất nhiều thời gian cho phần mở bài, làm sao để mở bài nhanh và thu hút?",
                voteCount = 51,
                voteState = VoteState.DOWNVOTED,
                comments = sampleCommentsPost4,
                tag = PostTag.Experience,
                authorAvatarUrl = "mock://avatar/thuyle"
            ),
            ForumPostDetail(
                id = "post-5",
                authorId = "user-anhvu",
                authorName = "anhvu",
                minutesAgo = 60 * 12,
                title = "Nguồn luyện Listening accent Anh-Anh",
                body = "Bạn nào có nguồn podcast hoặc video luyện accent Anh chuẩn không?",
                voteCount = 67,
                voteState = VoteState.NONE,
                comments = sampleCommentsPost5,
                tag = PostTag.Resource,
                authorAvatarUrl = "mock://avatar/anhvu"
            )
        )
    }
}
