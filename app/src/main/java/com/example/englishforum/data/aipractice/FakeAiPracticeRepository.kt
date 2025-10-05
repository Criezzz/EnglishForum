package com.example.englishforum.data.aipractice

class FakeAiPracticeRepository : AiPracticeRepository {

    private val practiceSets: Map<String, List<AiPracticeQuestion>> = mapOf(
        "post-1" to listOf(
            AiPracticeMultipleChoiceQuestion(
                id = "practice-1",
                prompt = "Câu nào dưới đây dùng \"dodge the bullet\" đúng nhất?",
                options = listOf(
                    AiPracticeOption("a", "I studied hard and passed the exam. I dodged the bullet."),
                    AiPracticeOption("b", "The train was late, but I dodged the bullet by leaving home early."),
                    AiPracticeOption("c", "I decided to dodge the bullet and face my fear."),
                    AiPracticeOption("d", "He dodged the bullet by buying a new phone he liked.")
                ),
                correctOptionId = "b",
                hint = "Hãy tìm đáp án nói về việc tránh khỏi một điều xấu xảy ra."
            ),
            AiPracticeMultipleChoiceQuestion(
                id = "practice-2",
                prompt = "Cụm từ \"speak of the devil\" phù hợp với tình huống nào?",
                options = listOf(
                    AiPracticeOption("a", "We were talking about Anna and she just walked in."),
                    AiPracticeOption("b", "The movie was so good it gave me goosebumps."),
                    AiPracticeOption("c", "I need to speak of the devil before the presentation."),
                    AiPracticeOption("d", "He speaks like the devil when he is angry.")
                ),
                correctOptionId = "a",
                hint = "Cụm này thường dùng khi người vừa được nhắc tới bất ngờ xuất hiện."
            ),
            AiPracticeFillInBlankQuestion(
                id = "practice-3",
                prompt = "Điền vào chỗ trống: She finally decided to ___ the bullet and tell her boss the truth.",
                correctAnswer = "bite",
                hint = "Thành ngữ \"bite the bullet\" nghĩa là đối mặt với điều khó khăn."
            )
        ),
        "post-3" to listOf(
            AiPracticeMultipleChoiceQuestion(
                id = "practice-4",
                prompt = "Đâu là cách dùng phù hợp của từ \"meticulous\"?",
                options = listOf(
                    AiPracticeOption("a", "She is meticulous about checking her essay for mistakes."),
                    AiPracticeOption("b", "He ran meticulous because he was late."),
                    AiPracticeOption("c", "They danced meticulous at the festival."),
                    AiPracticeOption("d", "Meticulous is my favorite color this season.")
                ),
                correctOptionId = "a",
                hint = "Meticulous mô tả sự cẩn thận, chú ý đến từng chi tiết."
            )
        )
    )

    override suspend fun checkFeasibility(postId: String): Result<Boolean> {
        return Result.success(practiceSets.containsKey(postId))
    }

    override suspend fun loadQuestions(postId: String): Result<List<AiPracticeQuestion>> {
        val questions = practiceSets[postId]
        return if (questions != null && questions.isNotEmpty()) {
            Result.success(questions)
        } else {
            Result.failure(IllegalStateException("Không tìm thấy câu hỏi phù hợp"))
        }
    }
}
