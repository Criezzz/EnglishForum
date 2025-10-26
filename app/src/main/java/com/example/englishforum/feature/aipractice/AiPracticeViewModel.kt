package com.example.englishforum.feature.aipractice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.data.aipractice.AiPracticeFillInBlankQuestion
import com.example.englishforum.data.aipractice.AiPracticeMultipleChoiceQuestion
import com.example.englishforum.data.aipractice.AiPracticeOption
import com.example.englishforum.data.aipractice.AiPracticeQuestion
import com.example.englishforum.data.aipractice.AiPracticeRepository
import com.example.englishforum.data.aipractice.FakeAiPracticeRepository
import com.example.englishforum.data.post.PostDetailRepository
import com.example.englishforum.data.post.FakePostDetailRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AiPracticeStage {
    Answering,
    Feedback
}

data class AiPracticeOptionUi(
    val id: String,
    val label: String
)

sealed interface AiPracticeQuestionUi {
    val id: String
    val prompt: String
    val hint: String?
}

data class AiPracticeMultipleChoiceQuestionUi(
    override val id: String,
    override val prompt: String,
    val options: List<AiPracticeOptionUi>,
    val correctOptionId: String,
    override val hint: String?
) : AiPracticeQuestionUi

data class AiPracticeFillInBlankQuestionUi(
    override val id: String,
    override val prompt: String,
    override val hint: String?
) : AiPracticeQuestionUi

data class AiPracticeSummaryUi(
    val totalQuestions: Int,
    val correctAnswers: Int
) {
    val incorrectAnswers: Int = totalQuestions - correctAnswers
}

data class AiPracticeUiState(
    val isLoading: Boolean = true,
    val question: AiPracticeQuestionUi? = null,
    val currentQuestionNumber: Int = 0,
    val totalQuestionCount: Int = 0,
    val selectedOptionId: String? = null,
    val answerInput: String = "",
    val hintVisible: Boolean = false,
    val stage: AiPracticeStage = AiPracticeStage.Answering,
    val isCurrentAnswerCorrect: Boolean? = null,
    val errorMessage: String? = null,
    val isLastQuestion: Boolean = false,
    val isCompleted: Boolean = false,
    val summary: AiPracticeSummaryUi? = null,
    val fillInCorrectAnswer: String? = null
)

class AiPracticeViewModel(
    private val postId: String,
    private val repository: AiPracticeRepository,
    private val postRepository: PostDetailRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiPracticeUiState())
    val uiState: StateFlow<AiPracticeUiState> = _uiState.asStateFlow()

    private var questions: List<AiPracticeQuestion> = emptyList()
    private var currentIndex: Int = 0
    private val answerRecord = mutableMapOf<String, Boolean>()

    init {
        loadQuestions()
    }

    fun onOptionSelected(optionId: String) {
        _uiState.update { state ->
            if (state.stage == AiPracticeStage.Answering && state.question is AiPracticeMultipleChoiceQuestionUi) {
                state.copy(selectedOptionId = optionId, errorMessage = null)
            } else {
                state
            }
        }
    }

    fun onAnswerInputChanged(answer: String) {
        _uiState.update { state ->
            if (state.stage == AiPracticeStage.Answering && state.question is AiPracticeFillInBlankQuestionUi) {
                state.copy(answerInput = answer, errorMessage = null)
            } else {
                state
            }
        }
    }

    fun onRequestHint() {
        _uiState.update { state ->
            if (state.stage == AiPracticeStage.Answering && state.question?.hint != null) {
                state.copy(hintVisible = true)
            } else {
                state
            }
        }
    }

    fun onCheckAnswer() {
        val state = _uiState.value
        val question = questions.getOrNull(currentIndex) ?: return

        when (question) {
            is AiPracticeMultipleChoiceQuestion -> {
                val selected = state.selectedOptionId
                if (selected == null) {
                    _uiState.update {
                        it.copy(errorMessage = "Vui lòng chọn đáp án trước khi kiểm tra.")
                    }
                    return
                }

                val isCorrect = selected == question.correctOptionId
                answerRecord[question.id] = isCorrect
                _uiState.update {
                    it.copy(
                        stage = AiPracticeStage.Feedback,
                        isCurrentAnswerCorrect = isCorrect,
                        fillInCorrectAnswer = null
                    )
                }
            }

            is AiPracticeFillInBlankQuestion -> {
                val answer = state.answerInput.trim()
                if (answer.isEmpty()) {
                    _uiState.update {
                        it.copy(errorMessage = "Vui lòng nhập đáp án trước khi kiểm tra.")
                    }
                    return
                }

                val normalizedAnswer = answer.lowercase()
                val acceptableAnswers = listOf(question.correctAnswer) + question.acceptedAnswers
                val isCorrect = acceptableAnswers.any { candidate ->
                    normalizedAnswer == candidate.trim().lowercase()
                }

                answerRecord[question.id] = isCorrect
                _uiState.update {
                    it.copy(
                        stage = AiPracticeStage.Feedback,
                        isCurrentAnswerCorrect = isCorrect,
                        fillInCorrectAnswer = if (isCorrect) null else question.correctAnswer
                    )
                }
            }
        }
    }

    fun onNextQuestion() {
        if (_uiState.value.stage != AiPracticeStage.Feedback) return
        if (currentIndex < questions.lastIndex) {
            currentIndex += 1
            val nextQuestion = questions[currentIndex]
            _uiState.update {
                it.copy(
                    question = nextQuestion.toUiModel(),
                    currentQuestionNumber = currentIndex + 1,
                    totalQuestionCount = questions.size,
                    selectedOptionId = null,
                    answerInput = "",
                    hintVisible = false,
                    stage = AiPracticeStage.Answering,
                    isCurrentAnswerCorrect = null,
                    isLastQuestion = currentIndex == questions.lastIndex,
                    fillInCorrectAnswer = null
                )
            }
        } else {
            onCompletePractice()
        }
    }

    fun onErrorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun onCompletePractice() {
        val state = _uiState.value
        if (state.stage != AiPracticeStage.Feedback || state.summary != null) return
        val total = questions.size
        val correct = answerRecord.count { it.value }

        _uiState.update {
            it.copy(
                isCompleted = true,
                summary = AiPracticeSummaryUi(
                    totalQuestions = total,
                    correctAnswers = correct
                ),
                question = null,
                currentQuestionNumber = total,
                selectedOptionId = null,
                answerInput = "",
                hintVisible = false,
                stage = AiPracticeStage.Feedback,
                isCurrentAnswerCorrect = null,
                isLastQuestion = false,
                fillInCorrectAnswer = null
            )
        }
    }

    fun onRetake() {
        if (questions.isEmpty()) {
            loadQuestions()
            return
        }

        answerRecord.clear()
        currentIndex = 0
        val initialQuestion = questions[currentIndex]
        _uiState.update {
            it.copy(
                isLoading = false,
                question = initialQuestion.toUiModel(),
                currentQuestionNumber = 1,
                totalQuestionCount = questions.size,
                selectedOptionId = null,
                answerInput = "",
                hintVisible = false,
                stage = AiPracticeStage.Answering,
                isCurrentAnswerCorrect = null,
                errorMessage = null,
                isLastQuestion = questions.size == 1,
                isCompleted = false,
                summary = null,
                fillInCorrectAnswer = null
            )
        }
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isCompleted = false,
                    summary = null
                )
            }

            answerRecord.clear()
            
            // Get post content first
            val post = postRepository.observePost(postId).firstOrNull()
            if (post == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        question = null,
                        currentQuestionNumber = 0,
                        totalQuestionCount = 0,
                        errorMessage = "Không tìm thấy bài viết.",
                        summary = null
                    )
                }
                return@launch
            }
            
            // Combine title and body for AI processing
            val postContent = "${post.title}\n\n${post.body}"
            
            // Check cache first for instant loading
            val cachedQuestions = repository.getCachedQuestions(postContent, "mcq", 3)
            
            if (cachedQuestions != null) {
                // Use cached questions immediately
                questions = cachedQuestions
                if (cachedQuestions.isNotEmpty()) {
                    currentIndex = 0
                    val initialQuestion = cachedQuestions[currentIndex]
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            question = initialQuestion.toUiModel(),
                            currentQuestionNumber = 1,
                            totalQuestionCount = cachedQuestions.size,
                            selectedOptionId = null,
                            answerInput = "",
                            hintVisible = false,
                            stage = AiPracticeStage.Answering,
                            isCurrentAnswerCorrect = null,
                            errorMessage = null,
                            isLastQuestion = cachedQuestions.size == 1,
                            isCompleted = false,
                            summary = null,
                            fillInCorrectAnswer = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            question = null,
                            currentQuestionNumber = 0,
                            totalQuestionCount = 0,
                            errorMessage = "Không có câu hỏi luyện tập cho bài viết này.",
                            summary = null
                        )
                    }
                }
                return@launch
            }
            
            // No cache found, generate new questions
            val result = repository.generateQuestions(postContent, "mcq", 3)
            result.onSuccess { list ->
                questions = list
                if (list.isNotEmpty()) {
                    currentIndex = 0
                    val initialQuestion = list[currentIndex]
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            question = initialQuestion.toUiModel(),
                            currentQuestionNumber = 1,
                            totalQuestionCount = list.size,
                            selectedOptionId = null,
                            answerInput = "",
                            hintVisible = false,
                            stage = AiPracticeStage.Answering,
                            isCurrentAnswerCorrect = null,
                            errorMessage = null,
                            isLastQuestion = list.size == 1,
                            isCompleted = false,
                            summary = null,
                            fillInCorrectAnswer = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            question = null,
                            currentQuestionNumber = 0,
                            totalQuestionCount = 0,
                            errorMessage = "Không có câu hỏi luyện tập cho bài viết này.",
                            summary = null
                        )
                    }
                }
            }
            result.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        question = null,
                        currentQuestionNumber = 0,
                        totalQuestionCount = 0,
                        errorMessage = throwable.message ?: "Không thể tải câu hỏi luyện tập.",
                        summary = null
                    )
                }
            }
        }
    }

    private fun AiPracticeQuestion.toUiModel(): AiPracticeQuestionUi {
        return when (this) {
            is AiPracticeMultipleChoiceQuestion -> AiPracticeMultipleChoiceQuestionUi(
                id = id,
                prompt = prompt,
                options = options.map { it.toUiModel() },
                correctOptionId = correctOptionId,
                hint = hint
            )

            is AiPracticeFillInBlankQuestion -> AiPracticeFillInBlankQuestionUi(
                id = id,
                prompt = prompt,
                hint = hint
            )
        }
    }

    private fun AiPracticeOption.toUiModel(): AiPracticeOptionUi {
        return AiPracticeOptionUi(id = id, label = label)
    }
}

class AiPracticeViewModelFactory(
    private val postId: String,
    private val repository: AiPracticeRepository = FakeAiPracticeRepository(),
    private val postRepository: PostDetailRepository = FakePostDetailRepository()
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiPracticeViewModel::class.java)) {
            return AiPracticeViewModel(postId, repository, postRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
