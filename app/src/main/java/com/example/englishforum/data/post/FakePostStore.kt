package com.example.englishforum.data.post

import com.example.englishforum.core.model.VoteState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

object FakePostStore {

    private val _posts = MutableStateFlow(createInitialPosts())
    val posts: StateFlow<List<PostDetail>> = _posts.asStateFlow()

    fun observePost(postId: String): Flow<PostDetail?> {
        return posts.map { list -> list.firstOrNull { it.id == postId } }
    }

    fun addPost(post: PostDetail) {
        _posts.update { current -> listOf(post) + current }
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
                    val updatedComments = post.comments.map { comment ->
                        if (comment.id == commentId) {
                            commentFound = true
                            val (nextState, delta) = resolveVoteChange(comment.voteState, target)
                            comment.copy(
                                voteState = nextState,
                                voteCount = comment.voteCount + delta
                            )
                        } else {
                            comment
                        }
                    }
                    post.copy(
                        comments = updatedComments,
                        commentCount = updatedComments.size
                    )
                } else {
                    post
                }
            }
        }
        return postFound to commentFound
    }

    private fun resolveVoteChange(currentState: VoteState, targetState: VoteState): Pair<VoteState, Int> {
        val nextState = if (currentState == targetState) {
            VoteState.NONE
        } else {
            targetState
        }

        val delta = when (currentState to nextState) {
            VoteState.NONE to VoteState.UPVOTED -> 1
            VoteState.NONE to VoteState.DOWNVOTED -> -1
            VoteState.UPVOTED to VoteState.NONE -> -1
            VoteState.DOWNVOTED to VoteState.NONE -> 1
            VoteState.UPVOTED to VoteState.DOWNVOTED -> -2
            VoteState.DOWNVOTED to VoteState.UPVOTED -> 2
            else -> 0
        }
        return nextState to delta
    }

    private fun createInitialPosts(): List<PostDetail> {
        val sampleCommentsPost1 = listOf(
            PostComment(
                id = "comment-1",
                authorName = "crystal",
                minutesAgo = 6 * 60,
                body = "Sed vulputate tellus magna, ac fringilla ipsum ornare in. Mình thường ghi âm lại rồi so sánh với bản chuẩn để thấy lỗi phát âm. Bạn có thể thử đọc đoạn hội thoại, thu âm từng câu, sau đó nhờ bạn bè góp ý. Nếu tự luyện, nên chia nhỏ từng cụm âm khó và lặp lại nhiều lần.",
                voteCount = 6,
                voteState = VoteState.NONE
            ),
            PostComment(
                id = "comment-2",
                authorName = "anotherone",
                minutesAgo = 4 * 60,
                body = "Quisque a lorem vitae ante pretium feugiat id a justo.",
                voteCount = 2,
                voteState = VoteState.NONE
            ),
            PostComment(
                id = "comment-11",
                authorName = "pronunciation_pro",
                minutesAgo = 3 * 60 + 30,
                body = "Mình thường dùng app Speechify để luyện theo, nó có thể chỉ ra lỗi phát âm và cho điểm. Còn cách truyền thống thì nên tập trước gương để quan sát miệng lưỡi.",
                voteCount = 8,
                voteState = VoteState.UPVOTED
            ),
            PostComment(
                id = "comment-12",
                authorName = "english_learner_vn",
                minutesAgo = 2 * 60 + 45,
                body = "Có thể tham khảo YouTube channel của Rachel's English. Cô ấy giải thích rất chi tiết về cách di chuyển lưỡi và môi cho từng âm.",
                voteCount = 12,
                voteState = VoteState.NONE
            ),
            PostComment(
                id = "comment-13",
                authorName = "speaking_master",
                minutesAgo = 2 * 60 + 15,
                body = "Shadow reading technique rất hiệu quả! Nghe podcast hoặc audiobook rồi nói theo ngay lập tức, không cần hiểu hết ý nghĩa lúc đầu.",
                voteCount = 5,
                voteState = VoteState.NONE
            ),
            PostComment(
                id = "comment-14",
                authorName = "ielts_coach",
                minutesAgo = 90,
                body = "Mình khuyên các bạn nên tập phát âm theo từng phoneme trước, rồi mới lên word level. Có thể dùng Cambridge Dictionary để nghe chuẩn âm Anh và Mỹ.",
                voteCount = 15,
                voteState = VoteState.UPVOTED
            ),
            PostComment(
                id = "comment-15",
                authorName = "practice_buddy",
                minutesAgo = 75,
                body = "Tongue twisters cũng là một cách hay đấy! 'She sells seashells by the seashore' - lặp lại mỗi ngày 10-15 phút.",
                voteCount = 7,
                voteState = VoteState.NONE
            ),
            PostComment(
                id = "comment-16",
                authorName = "voice_trainer",
                minutesAgo = 60,
                body = "Đừng quên breathing technique! Thở đúng cách sẽ giúp giọng nói tự tin và rõ ràng hơn. Luyện tập hít thở từ bụng, không phải ngực.",
                voteCount = 9,
                voteState = VoteState.NONE
            ),
            PostComment(
                id = "comment-17",
                authorName = "phonetics_expert",
                minutesAgo = 45,
                body = "International Phonetic Alphabet (IPA) là công cụ rất hữu ích. Học IPA sẽ giúp bạn phát âm chính xác bất kỳ từ nào mà không cần nghe audio.",
                voteCount = 11,
                voteState = VoteState.UPVOTED
            ),
            PostComment(
                id = "comment-18",
                authorName = "accent_reducer",
                minutesAgo = 30,
                body = "Mọi người có thể thử minimal pairs exercise. Ví dụ: ship/sheep, bit/beat. Tập phân biệt và phát âm các cặp từ này để cải thiện độ chính xác.",
                voteCount = 6,
                voteState = VoteState.NONE
            ),
            PostComment(
                id = "comment-19",
                authorName = "daily_english",
                minutesAgo = 15,
                body = "Reading aloud mỗi ngày 20-30 phút với newspaper hoặc novel cũng rất hiệu quả. Quan trọng là consistency và patience!",
                voteCount = 13,
                voteState = VoteState.UPVOTED
            )
        )

        val sampleCommentsPost2 = listOf(
            PostComment(
                id = "comment-3",
                authorName = "mentorX",
                minutesAgo = 32,
                body = "Great compilation! I usually start learners with Cambridge 15.",
                voteCount = 11,
                voteState = VoteState.UPVOTED
            ),
            PostComment(
                id = "comment-4",
                authorName = "selfstudy",
                minutesAgo = 124,
                body = "Thanks for sharing, saved to my drive already.",
                voteCount = 4,
                voteState = VoteState.NONE
            )
        )

        val sampleCommentsPost3 = listOf(
            PostComment(
                id = "comment-5",
                authorName = "ieltslover",
                minutesAgo = 75,
                body = "Học theo chủ đề giúp mình ghi nhớ lâu hơn đó! Ví dụ tuần này mình chọn chủ đề du lịch, sẽ tạo mind map với các nhóm từ vựng nhỏ hơn (địa điểm, hoạt động, cảm xúc). Sau đó ghép từ vào câu chuyện ngắn và luyện nói trước gương. Cuối tuần mình tự kiểm tra lại bằng cách viết journal tóm tắt hành trình du lịch trong tưởng tượng.",
                voteCount = 3,
                voteState = VoteState.NONE
            ),
            PostComment(
                id = "comment-6",
                authorName = "wordsmith",
                minutesAgo = 190,
                body = "Mình dùng Anki nên theo list 3000 từ khá ổn.",
                voteCount = 5,
                voteState = VoteState.UPVOTED
            )
        )

        val sampleCommentsPost4 = listOf(
            PostComment(
                id = "comment-7",
                authorName = "listentalk",
                minutesAgo = 260,
                body = "Mọi người thử kênh BBC Learning English xem nhé.",
                voteCount = 8,
                voteState = VoteState.NONE
            ),
            PostComment(
                id = "comment-8",
                authorName = "podcastfan",
                minutesAgo = 420,
                body = "Podcast dùng được không mọi người?",
                voteCount = 1,
                voteState = VoteState.NONE
            )
        )

        val sampleCommentsPost5 = listOf(
            PostComment(
                id = "comment-9",
                authorName = "speakingtime",
                minutesAgo = 300,
                body = "Cho xin link Zoom với nha!",
                voteCount = 7,
                voteState = VoteState.NONE
            ),
            PostComment(
                id = "comment-10",
                authorName = "friendfinder",
                minutesAgo = 512,
                body = "Mình cũng đang tìm partner, nhắn mình nhé.",
                voteCount = 2,
                voteState = VoteState.NONE
            )
        )

        return listOf(
            PostDetail(
                id = "post-1",
                authorName = "Jane_Doe",
                minutesAgo = 12,
                title = "Cách luyện phát âm tiếng Anh hàng ngày",
                body = "Mọi người có mẹo nào luyện phát âm hiệu quả khi không có người chỉnh lỗi không? Donec dictum rhoncus eros, eget fermentum dui laoreet a.",
                voteCount = 23,
                voteState = VoteState.NONE,
                commentCount = sampleCommentsPost1.size,
                comments = sampleCommentsPost1
            ),
            PostDetail(
                id = "post-2",
                authorName = "Mr.Teacher",
                minutesAgo = 38,
                title = "Chia sẻ tài liệu IELTS Reading band 7+",
                body = "Mình tổng hợp vài tài liệu tự học khá hay, mọi người tải về tham khảo thử nhé.",
                voteCount = 54,
                voteState = VoteState.NONE,
                commentCount = sampleCommentsPost2.size,
                comments = sampleCommentsPost2
            ),
            PostDetail(
                id = "post-3",
                authorName = "lisa.ng",
                minutesAgo = 51,
                title = "Nên học từ vựng theo chủ đề hay danh sách 3000 từ?",
                body = "Đang phân vân không biết nên follow list nào để học cho hiệu quả hơn.",
                voteCount = 11,
                voteState = VoteState.UPVOTED,
                commentCount = sampleCommentsPost3.size,
                comments = sampleCommentsPost3
            ),
            PostDetail(
                id = "post-4",
                authorName = "NguyenVanA",
                minutesAgo = 89,
                title = "Các app luyện nghe miễn phí tốt nhất hiện nay",
                body = "Mình thử qua vài app như Elsa, Cake... có app nào mọi người thấy hay hơn không?",
                voteCount = 31,
                voteState = VoteState.NONE,
                commentCount = sampleCommentsPost4.size,
                comments = sampleCommentsPost4
            ),
            PostDetail(
                id = "post-5",
                authorName = "studybuddy",
                minutesAgo = 143,
                title = "Looking for a speaking partner",
                body = "We can practice on Zoom 2-3h/week, ai quan tâm để lại comment nha!",
                voteCount = 27,
                voteState = VoteState.DOWNVOTED,
                commentCount = sampleCommentsPost5.size,
                comments = sampleCommentsPost5
            )
        )
    }
}
