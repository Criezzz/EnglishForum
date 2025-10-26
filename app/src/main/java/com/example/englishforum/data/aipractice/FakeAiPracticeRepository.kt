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


    override suspend fun loadQuestions(postContent: String): Result<List<AiPracticeQuestion>> {
        // For backward compatibility, use generateQuestions with random type and 1 question (50-50 chance)
        val randomType = if (kotlin.random.Random.nextBoolean()) "mcq" else "fill"
        return generateQuestions(postContent, randomType, 1)
    }

    override suspend fun generateQuestions(postContent: String, type: String, numItems: Int): Result<List<AiPracticeQuestion>> {
        // For fake implementation, simulate feasibility based on content length
        // Posts with more content are more likely to be askable
        val isAskable = postContent.length > 50 && kotlin.random.Random.nextFloat() < 0.8f
        
        if (!isAskable) {
            return Result.failure(IllegalStateException("AI practice is not available for this post"))
        }
        
        // Generate some fake questions based on type and content
        val generatedQuestions = generateFakeQuestions(postContent, type, numItems)
        return Result.success(generatedQuestions)
    }

    private fun generateFakeQuestions(postContent: String, type: String, numItems: Int): List<AiPracticeQuestion> {
        return when (type.lowercase()) {
            "mcq" -> {
                (1..numItems).map { index ->
                    AiPracticeMultipleChoiceQuestion(
                        id = "generated-mc-${postContent.hashCode()}-$index",
                        prompt = "Generated multiple choice question $index based on content: ${postContent.take(50)}...?",
                        options = listOf(
                            AiPracticeOption("a", "Option A"),
                            AiPracticeOption("b", "Option B"),
                            AiPracticeOption("c", "Option C"),
                            AiPracticeOption("d", "Option D")
                        ),
                        correctOptionId = "b",
                        hint = "This is a generated hint for question $index"
                    )
                }
            }
            "fill" -> {
                (1..numItems).map { index ->
                    AiPracticeFillInBlankQuestion(
                        id = "generated-fib-${postContent.hashCode()}-$index",
                        prompt = "Generated fill-in-blank question $index based on content: ${postContent.take(50)}...: The answer is ___.",
                        correctAnswer = "correct",
          
                        hint = "This is a generated hint for question $index"
                    )
                }
            }
            else -> {
                // Mixed type - return both types
                val mcCount = (numItems + 1) / 2
                val fibCount = numItems - mcCount
                
                val mcQuestions = (1..mcCount).map { index ->
                    AiPracticeMultipleChoiceQuestion(
                        id = "generated-mc-${postContent.hashCode()}-$index",
                        prompt = "Generated multiple choice question $index based on content: ${postContent.take(50)}...?",
                        options = listOf(
                            AiPracticeOption("a", "Option A"),
                            AiPracticeOption("b", "Option B"),
                            AiPracticeOption("c", "Option C"),
                            AiPracticeOption("d", "Option D")
                        ),
                        correctOptionId = "b",
                        hint = "This is a generated hint for question $index"
                    )
                }
                
                val fibQuestions = (1..fibCount).map { index ->
                    AiPracticeFillInBlankQuestion(
                        id = "generated-fib-${postContent.hashCode()}-$index",
                        prompt = "Generated fill-in-blank question $index based on content: ${postContent.take(50)}...: The answer is ___.",
                        correctAnswer = "correct",
                        hint = "This is a generated hint for question $index"
                    )
                }
                
                mcQuestions + fibQuestions
            }
        }
    }
    
    override suspend fun getCachedQuestions(postContent: String, type: String, numItems: Int): List<AiPracticeQuestion>? {
        // Fake implementation doesn't use cache
        return null
    }
    
    override suspend fun cacheQuestions(postContent: String, type: String, numItems: Int, questions: List<AiPracticeQuestion>) {
        // Fake implementation doesn't use cache
    }
    
    override suspend fun clearCache() {
        // Fake implementation doesn't use cache
    }
}
