package com.example.englishforum.feature.aipractice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.englishforum.data.aipractice.AiPracticeOption
import com.example.englishforum.data.aipractice.AiPracticeQuestion
import com.example.englishforum.data.aipractice.AiPracticeRepository
import com.example.englishforum.data.aipractice.FakeAiPracticeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

data class AiPracticeQuestionUi(
    val id: String,
    val prompt: String,
    val options: List<AiPracticeOptionUi>,
    val correctOptionId: String,
    val hint: String?
)

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
    val hintVisible: Boolean = false,
    val stage: AiPracticeStage = AiPracticeStage.Answering,
    val isCurrentAnswerCorrect: Boolean? = null,
    val errorMessage: String? = null,
    val isLastQuestion: Boolean = false,
    val isCompleted: Boolean = false,
    val summary: AiPracticeSummaryUi? = null
)

class AiPracticeViewModel(
    private val postId: String,
    private val repository: AiPracticeRepository = FakeAiPracticeRepository()
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
            if (state.stage == AiPracticeStage.Answering) {
                state.copy(selectedOptionId = optionId)
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
        val question = state.question ?: return
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
                isCurrentAnswerCorrect = isCorrect
            )
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
                    hintVisible = false,
                    stage = AiPracticeStage.Answering,
                    isCurrentAnswerCorrect = null,
                    isLastQuestion = currentIndex == questions.lastIndex
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
                hintVisible = false,
                stage = AiPracticeStage.Feedback,
                isCurrentAnswerCorrect = null,
                isLastQuestion = false
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
                hintVisible = false,
                stage = AiPracticeStage.Answering,
                isCurrentAnswerCorrect = null,
                errorMessage = null,
                isLastQuestion = questions.size == 1,
                isCompleted = false,
                summary = null
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
            val result = repository.loadQuestions(postId)
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
                            hintVisible = false,
                            stage = AiPracticeStage.Answering,
                            isCurrentAnswerCorrect = null,
                            errorMessage = null,
                            isLastQuestion = list.size == 1,
                            isCompleted = false,
                            summary = null
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
        return AiPracticeQuestionUi(
            id = id,
            prompt = prompt,
            options = options.map { it.toUiModel() },
            correctOptionId = correctOptionId,
            hint = hint
        )
    }

    private fun AiPracticeOption.toUiModel(): AiPracticeOptionUi {
        return AiPracticeOptionUi(id = id, label = label)
    }
}

class AiPracticeViewModelFactory(
    private val postId: String,
    private val repository: AiPracticeRepository = FakeAiPracticeRepository()
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AiPracticeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AiPracticeViewModel(postId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
