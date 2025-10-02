package com.example.englishforum.feature.create

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.englishforum.R
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.ui.theme.EnglishForumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRoute(
    modifier: Modifier = Modifier,
    onNavigateToPostDetail: (String) -> Unit
) {
    val appContainer = LocalAppContainer.current
    val viewModel: CreateViewModel = viewModel(
        factory = remember(appContainer) { CreateViewModelFactory(appContainer.createPostRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.successPostId) {
        val postId = uiState.successPostId
        if (postId != null) {
            onNavigateToPostDetail(postId)
            viewModel.onNavigationHandled()
        }
    }

    CreateScreen(
        modifier = modifier,
        uiState = uiState,
        onTitleChange = viewModel::onTitleChange,
        onBodyChange = viewModel::onBodyChange,
        onAddAttachment = viewModel::onAddAttachment,
        onRemoveAttachment = viewModel::onRemoveAttachment,
        onSubmit = viewModel::onSubmit,
        onDeclineReasonDismissed = viewModel::onDeclineReasonDismissed,
        onErrorMessageConsumed = viewModel::onErrorMessageDisplayed
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    uiState: CreateUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onSubmit: () -> Unit,
    onDeclineReasonDismissed: () -> Unit,
    onErrorMessageConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onErrorMessageConsumed()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.create_post_title)) },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TextField(
                value = uiState.title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(id = R.string.create_post_title_label)) },
                placeholder = { Text(text = stringResource(id = R.string.create_post_title_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(onNext = { })
            )

            TextField(
                value = uiState.body,
                onValueChange = onBodyChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),
                label = { Text(text = stringResource(id = R.string.create_post_body_label)) },
                placeholder = { Text(text = stringResource(id = R.string.create_post_body_placeholder)) },
                singleLine = false,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Default,
                    keyboardType = KeyboardType.Text
                )
            )

            AttachmentSection(
                attachments = uiState.attachments,
                onAddAttachment = onAddAttachment,
                onRemoveAttachment = onRemoveAttachment
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                enabled = uiState.canSubmit,
                onClick = onSubmit,
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.create_post_submit_loading))
                } else {
                    Text(text = stringResource(id = R.string.create_post_submit))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (uiState.declineReason != null) {
        DeclineDialog(
            reason = uiState.declineReason,
            onDismiss = onDeclineReasonDismissed
        )
    }
}

@Composable
private fun AttachmentSection(
    attachments: List<CreateAttachmentUi>,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.create_post_attachment_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (attachments.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                attachments.forEach { attachment ->
                    AssistChip(
                        onClick = { onRemoveAttachment(attachment.id) },
                        label = { Text(text = attachment.label) },
                        shape = MaterialTheme.shapes.medium,
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(
                                    id = R.string.create_post_remove_image_content_description,
                                    attachment.label
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        FilledTonalButton(onClick = onAddAttachment) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(id = R.string.create_post_add_image))
        }
    }
}

@Composable
private fun DeclineDialog(
    reason: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = stringResource(id = R.string.create_post_declined_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(id = R.string.create_post_declined_message, reason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                FilledTonalButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.create_post_declined_edit))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateScreenPreview() {
    val previewState = CreateUiState(
        title = "What?",
        body = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse urna est, vulputate eu justo nec, feugiat ullamcorper velit.",
        attachments = listOf(
            CreateAttachmentUi(id = "1", label = "Ảnh 1"),
            CreateAttachmentUi(id = "2", label = "Ảnh 2")
        )
    )

    EnglishForumTheme {
        CreateScreen(
            uiState = previewState,
            onTitleChange = {},
            onBodyChange = {},
            onAddAttachment = {},
            onRemoveAttachment = {},
            onSubmit = {},
            onDeclineReasonDismissed = {},
            onErrorMessageConsumed = {}
        )
    }
}
