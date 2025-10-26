package com.example.englishforum.feature.aipractice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.englishforum.R
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import kotlinx.coroutines.delay

// Danh sách các fact tiếng Anh thú vị
private val englishFacts = listOf(
    "Did you know? The word 'set' has the most meanings in English - over 400 different definitions!",
    "Fun fact: 'I am' is the shortest complete sentence in English.",
    "Interesting: The word 'queue' is pronounced the same way even if you remove the last 4 letters!",
    "Amazing: 'Bookkeeper' and 'bookkeeping' are the only words with 3 consecutive double letters.",
    "Cool fact: The word 'therein' contains 10 words without rearranging any letters: the, there, he, in, rein, her, here, ere, therein, herein.",
    "Fascinating: 'Rhythm' is the longest English word without a vowel.",
    "Did you know? 'Dreamt' is the only English word that ends in 'mt'.",
    "Fun fact: The word 'almost' is the longest word with all letters in alphabetical order.",
    "Interesting: 'Underground' is the only word that begins and ends with 'und'.",
    "Amazing: The word 'strengths' is the longest word with only one vowel.",
    "Cool fact: 'Subdermatoglyphic' is the longest English word that can be written without repeating any letter.",
    "Fascinating: The word 'queue' is pronounced the same way even if you remove the last 4 letters!",
    "Did you know? 'The quick brown fox jumps over the lazy dog' uses every letter of the alphabet.",
    "Fun fact: 'Go' is the shortest English word with 2 syllables.",
    "Interesting: The word 'set' has the most meanings in English - over 400 different definitions!"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPracticeRoute(
    postId: String,
    onBackClick: () -> Unit
) {
    val appContainer = LocalAppContainer.current
    val viewModel: AiPracticeViewModel = viewModel(
        factory = remember(postId, appContainer) {
            AiPracticeViewModelFactory(
                postId = postId,
                repository = appContainer.aiPracticeRepository,
                postRepository = appContainer.postDetailRepository
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorMessageShown()
        }
    }

    AiPracticeScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBackClick = {
            onBackClick()
        },
        onOptionSelected = viewModel::onOptionSelected,
        onAnswerInputChanged = viewModel::onAnswerInputChanged,
        onHintClick = viewModel::onRequestHint,
        onCheckClick = viewModel::onCheckAnswer,
        onNextClick = viewModel::onNextQuestion,
        onCompleteClick = viewModel::onCompletePractice,
        onRetakeClick = viewModel::onRetake,
        onExitClick = onBackClick,
        onRequestCancel = viewModel::cancelGeneration
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPracticeScreen(
    uiState: AiPracticeUiState,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onRequestCancel: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onAnswerInputChanged: (String) -> Unit,
    onHintClick: () -> Unit,
    onCheckClick: () -> Unit,
    onNextClick: () -> Unit,
    onCompleteClick: () -> Unit,
    onRetakeClick: () -> Unit,
    onExitClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCancelConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = stringResource(R.string.ai_practice_title))
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isLoading) {
                            showCancelConfirm = true
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.auth_back_action)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.post_detail_options_content_description)
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.padding(innerPadding))
            }

            uiState.isCompleted && uiState.summary != null -> {
                CompletionContent(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    summary = uiState.summary,
                    onRetakeClick = onRetakeClick,
                    onExitClick = onExitClick
                )
            }

            uiState.question == null -> {
                EmptyState(modifier = Modifier.padding(innerPadding))
            }

            else -> {
                QuestionContent(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    uiState = uiState,
                    onOptionSelected = onOptionSelected,
                    onAnswerInputChanged = onAnswerInputChanged,
                    onHintClick = onHintClick,
                    onCheckClick = onCheckClick,
                    onNextClick = onNextClick,
                    onCompleteClick = onCompleteClick
                )
            }
        }
    }

    // Auto-dismiss confirm dialog if generation completed while dialog is shown
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            showCancelConfirm = false
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text(text = "Huỷ tạo bài tập?") },
            text = { Text(text = "Bạn có chắc muốn huỷ quá trình tạo?") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelConfirm = false
                    onRequestCancel()
                    onBackClick()
                }) {
                    Text(text = "Huỷ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text(text = "Ở lại")
                }
            }
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    var showFact by remember { mutableStateOf(false) }
    var currentFact by remember { mutableStateOf(englishFacts.random()) }
    
    // Thay đổi fact mỗi khi component được tạo lại
    LaunchedEffect(Unit) {
        currentFact = englishFacts.random()
        delay(2000) // 2 giây
        showFact = true
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        
        if (showFact) {
            // Hiển thị fact thú vị sau 2 giây
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.TipsAndUpdates,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "💡 English Fun Fact",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = currentFact,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                    )
                }
            }
        } else {
            // Hiển thị loading message ban đầu trong 2 giây đầu
            Text(
                text = stringResource(R.string.ai_practice_loading_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.ai_practice_empty_state),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuestionContent(
    modifier: Modifier = Modifier,
    uiState: AiPracticeUiState,
    onOptionSelected: (String) -> Unit,
    onAnswerInputChanged: (String) -> Unit,
    onHintClick: () -> Unit,
    onCheckClick: () -> Unit,
    onNextClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    val question = uiState.question ?: return
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (uiState.totalQuestionCount > 0) {
                Text(
                    text = stringResource(
                        R.string.ai_practice_question_counter,
                        uiState.currentQuestionNumber,
                        uiState.totalQuestionCount
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 1.0f)
                )
            }

            Text(
                text = question.prompt,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        when (question) {
            is AiPracticeMultipleChoiceQuestionUi -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    question.options.forEach { option ->
                        AiPracticeOptionItem(
                            option = option,
                            question = question,
                            uiState = uiState,
                            onOptionSelected = onOptionSelected
                        )
                    }
                }
            }

            is AiPracticeFillInBlankQuestionUi -> {
                FillInBlankAnswerSection(
                    uiState = uiState,
                    onAnswerChanged = onAnswerInputChanged
                )
            }
        }

        AnimatedVisibility(visible = uiState.hintVisible && question.hint != null) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.ai_practice_hint_title),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = question.hint.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        if (uiState.stage == AiPracticeStage.Feedback && uiState.isCurrentAnswerCorrect != null) {
            val feedbackText = if (uiState.isCurrentAnswerCorrect) {
                stringResource(R.string.ai_practice_correct_feedback)
            } else {
                stringResource(R.string.ai_practice_incorrect_feedback)
            }
            val feedbackColor = if (uiState.isCurrentAnswerCorrect) {
                MaterialTheme.colorScheme.primary.copy(alpha = 1.0f)
            } else {
                MaterialTheme.colorScheme.error.copy(alpha = 1.0f)
            }

            Text(
                text = feedbackText,
                style = MaterialTheme.typography.bodyLarge,
                color = feedbackColor
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onHintClick,
                enabled = uiState.stage == AiPracticeStage.Answering && !uiState.hintVisible && question.hint != null,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Outlined.TipsAndUpdates,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.ai_practice_hint_button))
            }

            Button(
                onClick = {
                    when {
                        uiState.stage == AiPracticeStage.Answering -> onCheckClick()
                        uiState.stage == AiPracticeStage.Feedback && uiState.isLastQuestion -> onCompleteClick()
                        uiState.stage == AiPracticeStage.Feedback -> onNextClick()
                    }
                },
                enabled = when (uiState.stage) {
                    AiPracticeStage.Answering -> when (question) {
                        is AiPracticeMultipleChoiceQuestionUi -> uiState.selectedOptionId != null
                        is AiPracticeFillInBlankQuestionUi -> uiState.answerInput.isNotBlank()
                    }

                    AiPracticeStage.Feedback -> true
                },
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.stage == AiPracticeStage.Answering) {
                    Text(text = stringResource(R.string.ai_practice_check_button))
                } else if (uiState.isLastQuestion) {
                    Text(text = stringResource(R.string.ai_practice_finish_button))
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.ai_practice_next_button))
                }
            }
        }
    }
}

@Composable
private fun FillInBlankAnswerSection(
    uiState: AiPracticeUiState,
    onAnswerChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFeedback = uiState.stage == AiPracticeStage.Feedback
    val isCorrect = uiState.isCurrentAnswerCorrect == true

    OutlinedTextField(
        value = uiState.answerInput,
        onValueChange = onAnswerChanged,
        modifier = modifier.fillMaxWidth(),
        enabled = !isFeedback,
        singleLine = true,
        label = { Text(text = stringResource(R.string.ai_practice_fill_in_blank_label)) },
        placeholder = { Text(text = stringResource(R.string.ai_practice_fill_in_blank_placeholder)) },
        isError = isFeedback && !isCorrect,
        supportingText = {
            when {
                isFeedback && !isCorrect && uiState.fillInCorrectAnswer != null -> {
                    Text(
                        text = stringResource(
                            R.string.ai_practice_fill_in_blank_correct_answer,
                            uiState.fillInCorrectAnswer
                        )
                    )
                }

                !isFeedback -> {
                    Text(text = stringResource(R.string.ai_practice_fill_in_blank_supporting))
                }
            }
        }
    )
}

@Composable
private fun CompletionContent(
    modifier: Modifier = Modifier,
    summary: AiPracticeSummaryUi,
    onRetakeClick: () -> Unit,
    onExitClick: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.ai_practice_summary_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(
                R.string.ai_practice_summary_score,
                summary.correctAnswers,
                summary.totalQuestions
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryStatRow(
                    label = stringResource(R.string.ai_practice_summary_correct_label),
                    value = summary.correctAnswers,
                    highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 1.0f)
                )
                SummaryStatRow(
                    label = stringResource(R.string.ai_practice_summary_incorrect_label),
                    value = summary.incorrectAnswers,
                    highlightColor = MaterialTheme.colorScheme.error.copy(alpha = 1.0f)
                )
            }
        }

        Button(onClick = onRetakeClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.ai_practice_summary_retry_button))
        }

        FilledTonalButton(onClick = onExitClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(R.string.ai_practice_summary_exit_button))
        }
    }
}

@Composable
private fun SummaryStatRow(
    label: String,
    value: Int,
    highlightColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = highlightColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiPracticeOptionItem(
    option: AiPracticeOptionUi,
    question: AiPracticeMultipleChoiceQuestionUi,
    uiState: AiPracticeUiState,
    onOptionSelected: (String) -> Unit
) {
    val isSelected = uiState.selectedOptionId == option.id
    val isCorrectOption = uiState.stage == AiPracticeStage.Feedback && option.id == question.correctOptionId
    val isIncorrectSelection = uiState.stage == AiPracticeStage.Feedback && isSelected && !isCorrectOption

    val backgroundColor = when {
        isCorrectOption -> MaterialTheme.colorScheme.secondaryContainer
        isIncorrectSelection -> MaterialTheme.colorScheme.errorContainer
        uiState.stage == AiPracticeStage.Answering && isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when {
        isCorrectOption -> MaterialTheme.colorScheme.onSecondaryContainer
        isIncorrectSelection -> MaterialTheme.colorScheme.onErrorContainer
        uiState.stage == AiPracticeStage.Answering && isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = backgroundColor,
        contentColor = contentColor,
        tonalElevation = 2.dp,
        onClick = { onOptionSelected(option.id) },
        enabled = uiState.stage == AiPracticeStage.Answering
    ) {
        Text(
            text = option.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AiPracticeScreenPreview() {
    val sampleState = AiPracticeUiState(
        isLoading = false,
        question = AiPracticeMultipleChoiceQuestionUi(
            id = "practice-1",
            prompt = "Câu nào dưới đây dùng \"dodge the bullet\" đúng nhất?",
            options = listOf(
                AiPracticeOptionUi("a", "I studied hard and passed the exam. I dodged the bullet."),
                AiPracticeOptionUi("b", "The train was late, but I dodged the bullet by leaving home early."),
                AiPracticeOptionUi("c", "I decided to dodge the bullet and face my fear."),
                AiPracticeOptionUi("d", "He dodged the bullet by buying a new phone he liked.")
            ),
            correctOptionId = "b",
            hint = "Hãy tìm đáp án nói về việc tránh khỏi một điều xấu xảy ra."
        ),
        currentQuestionNumber = 1,
        totalQuestionCount = 2,
        selectedOptionId = "c",
        hintVisible = true,
        stage = AiPracticeStage.Feedback,
        isCurrentAnswerCorrect = false,
        fillInCorrectAnswer = null
    )

    EnglishForumTheme {
        AiPracticeScreen(
            uiState = sampleState,
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onRequestCancel = {},
            onOptionSelected = {},
            onAnswerInputChanged = {},
            onHintClick = {},
            onCheckClick = {},
            onNextClick = {},
            onCompleteClick = {},
            onRetakeClick = {},
            onExitClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AiPracticeFillInBlankPreview() {
    val sampleState = AiPracticeUiState(
        isLoading = false,
        question = AiPracticeFillInBlankQuestionUi(
            id = "practice-2",
            prompt = "Điền vào chỗ trống: She finally decided to ___ the bullet and tell her boss the truth.",
            hint = "Thành ngữ \"bite the bullet\" nghĩa là đối mặt với điều khó khăn."
        ),
        currentQuestionNumber = 2,
        totalQuestionCount = 3,
        answerInput = "byte",
        hintVisible = true,
        stage = AiPracticeStage.Feedback,
        isCurrentAnswerCorrect = false,
        fillInCorrectAnswer = "bite"
    )

    EnglishForumTheme {
        AiPracticeScreen(
            uiState = sampleState,
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onRequestCancel = {},
            onOptionSelected = {},
            onAnswerInputChanged = {},
            onHintClick = {},
            onCheckClick = {},
            onNextClick = {},
            onCompleteClick = {},
            onRetakeClick = {},
            onExitClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AiPracticeSummaryPreview() {
    val summaryState = AiPracticeUiState(
        isLoading = false,
        isCompleted = true,
        summary = AiPracticeSummaryUi(totalQuestions = 4, correctAnswers = 3)
    )

    EnglishForumTheme {
        AiPracticeScreen(
            uiState = summaryState,
            snackbarHostState = SnackbarHostState(),
            onBackClick = {},
            onRequestCancel = {},
            onOptionSelected = {},
            onAnswerInputChanged = {},
            onHintClick = {},
            onCheckClick = {},
            onNextClick = {},
            onCompleteClick = {},
            onRetakeClick = {},
            onExitClick = {}
        )
    }
}
