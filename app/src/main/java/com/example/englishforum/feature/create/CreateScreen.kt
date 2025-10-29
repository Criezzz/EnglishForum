package com.example.englishforum.feature.create

//import androidx.compose.foundation.layout.navigationBarsPadding
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.englishforum.R
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.core.ui.components.ForumAuthorAvatar
import com.example.englishforum.core.ui.components.ForumAuthorLink
import com.example.englishforum.core.ui.components.ForumTagLabel
import com.example.englishforum.core.ui.components.card.ForumContentCard
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import com.example.englishforum.core.ui.toLabelResId
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostBottomSheet(
    onDismiss: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val appContainer = LocalAppContainer.current
    val viewModel: CreateViewModel = viewModel(
        factory = remember(appContainer) { CreateViewModelFactory(appContainer.createPostRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.successPostId) {
        val postId = uiState.successPostId
        if (postId != null) {
            onDismiss()
            onNavigateToPostDetail(postId)
            viewModel.onNavigationHandled()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        CreateScreen(
            uiState = uiState,
            onTitleChange = viewModel::onTitleChange,
            onBodyChange = viewModel::onBodyChange,
            onAddAttachment = viewModel::onAddAttachment,
            onRemoveAttachment = viewModel::onRemoveAttachment,
            onImageSelected = viewModel::onImageSelected,
            onRemoveImage = viewModel::onRemoveImage,
            onTagSelected = viewModel::onTagSelected,
            onSubmit = viewModel::onSubmit,
            onDeclineReasonDismissed = viewModel::onDeclineReasonDismissed,
            onErrorMessageConsumed = viewModel::onErrorMessageDisplayed,
            onSuccessMessageConsumed = viewModel::onSuccessMessageDisplayed,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    uiState: CreateUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onTagSelected: (PostTag) -> Unit,
    onSubmit: () -> Unit,
    onDeclineReasonDismissed: () -> Unit,
    onErrorMessageConsumed: () -> Unit,
    onSuccessMessageConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes topBarTitleRes: Int = R.string.create_post_title,
    @StringRes submitLabelRes: Int = R.string.create_post_submit,
    @StringRes submitLoadingLabelRes: Int = R.string.create_post_submit_loading,
    onBackClick: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onErrorMessageConsumed()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        val message = uiState.successMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onSuccessMessageConsumed()
        }
    }
    val tfColors = TextFieldDefaults.colors(

        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,

        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,

        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    val boxShape = RoundedCornerShape(12.dp)
    val borderColor = MaterialTheme.colorScheme.outline

    // Use Scaffold only if there's a back button (for edit mode), otherwise just Column for bottom sheet
    if (onBackClick != null) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(id = topBarTitleRes)) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.auth_back_action)
                            )
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0)
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            if (uiState.isInitialLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    CreateFormContent(
                        uiState = uiState,
                        onTitleChange = onTitleChange,
                        onBodyChange = onBodyChange,
                        onImageSelected = onImageSelected,
                        onRemoveImage = onRemoveImage,
                        onTagSelected = onTagSelected,
                        onSubmit = onSubmit,
                        submitLabelRes = submitLabelRes,
                        submitLoadingLabelRes = submitLoadingLabelRes,
                        tfColors = tfColors,
                        boxShape = boxShape,
                        borderColor = borderColor
                    )
                }
            }
        }
    } else {
        // Bottom sheet mode - Material 3 expressive design with multi-step
        var currentStep by rememberSaveable { mutableIntStateOf(0) }
        val totalSteps = 4
        
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = topBarTitleRes),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (currentStep) {
                            0 -> stringResource(id = R.string.create_post_step1_subtitle)
                            1 -> stringResource(id = R.string.create_post_step2_subtitle)
                            2 -> stringResource(id = R.string.create_post_step3_subtitle)
                            3 -> stringResource(id = R.string.create_post_step4_subtitle)
                            else -> stringResource(id = R.string.create_post_subtitle)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (onDismiss != null) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(id = R.string.auth_cancel_action)
                        )
                    }
                }
            }

            // Step indicator
            StepIndicator(
                currentStep = currentStep,
                totalSteps = totalSteps,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (uiState.isInitialLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    when (currentStep) {
                        0 -> Step1TagSelection(
                            availableTags = uiState.availableTags,
                            selectedTag = uiState.selectedTag,
                            onTagSelected = onTagSelected
                        )
                        1 -> Step2ContentInput(
                            title = uiState.title,
                            body = uiState.body,
                            onTitleChange = onTitleChange,
                            onBodyChange = onBodyChange,
                            tfColors = tfColors,
                            boxShape = boxShape,
                            borderColor = borderColor
                        )
                        2 -> Step3Images(
                            uiState = uiState,
                            onImageSelected = onImageSelected,
                            onRemoveImage = onRemoveImage
                        )
                        3 -> Step4Preview(uiState = uiState)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentStep > 0) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = stringResource(R.string.auth_back_action))
                        }
                    }

                    Button(
                        onClick = {
                            if (currentStep < totalSteps - 1) {
                                currentStep++
                            } else {
                                onSubmit()
                            }
                        },
                        enabled = when (currentStep) {
                            0 -> uiState.selectedTag != null
                            1 -> uiState.title.isNotBlank() && uiState.body.isNotBlank()
                            2 -> true
                            3 -> uiState.canSubmit
                            else -> false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        if (currentStep == totalSteps - 1 && uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(id = submitLoadingLabelRes))
                        } else {
                            Text(
                                text = if (currentStep < totalSteps - 1) {
                                    stringResource(R.string.create_post_next)
                                } else {
                                    stringResource(id = submitLabelRes)
                                }
                            )
                            if (currentStep < totalSteps - 1) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Snackbar for bottom sheet mode
            LaunchedEffect(snackbarHostState) {
                // Snackbar host is handled by parent in bottom sheet mode
            }
        }
    }

    if (uiState.declineReason != null) {
        DeclineDialog(
            reason = uiState.declineReason,
            onDismiss = onDeclineReasonDismissed
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateFormContent(
    uiState: CreateUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onTagSelected: (PostTag) -> Unit,
    onSubmit: () -> Unit,
    @StringRes submitLabelRes: Int,
    @StringRes submitLoadingLabelRes: Int,
    tfColors: androidx.compose.material3.TextFieldColors,
    boxShape: RoundedCornerShape,
    borderColor: Color
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    // Tag selector at the top
    if (uiState.availableTags.isNotEmpty()) {
        TagSelectorDropdown(
            tags = uiState.availableTags,
            selectedTag = uiState.selectedTag,
            onTagSelected = onTagSelected
        )
    }

    Box(
        modifier = Modifier
            .shadow(1.dp, boxShape)
            .border(1.dp, borderColor, boxShape)
    ) {
        TextField(
            value = uiState.title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            shape = boxShape,
            colors = tfColors,
            label = { Text(text = stringResource(id = R.string.create_post_title_label)) },
            placeholder = { Text(text = stringResource(id = R.string.create_post_title_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { })
        )
    }

    Box(
        modifier = Modifier
            .shadow(1.dp, boxShape)
            .border(1.dp, borderColor, boxShape)
    ) {
        TextField(
            value = uiState.body,
            onValueChange = onBodyChange,
            modifier = Modifier.fillMaxWidth(),
            shape = boxShape,
            colors = tfColors,
            label = { Text(text = stringResource(id = R.string.create_post_body_label)) },
            placeholder = { Text(text = stringResource(id = R.string.create_post_body_placeholder)) },
            singleLine = false,
            minLines = 5,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default,
                keyboardType = KeyboardType.Text
            )
        )
    }

    ImagePickerSection(
        imageUris = uiState.imageUris,
        onPickImage = { imagePickerLauncher.launch("image/*") },
        onRemoveImage = onRemoveImage
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
            Text(text = stringResource(id = submitLoadingLabelRes))
        } else {
            Text(text = stringResource(id = submitLabelRes))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

// Step indicator
@Composable
private fun StepIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSteps) { index ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp),
                color = when {
                    index < currentStep -> MaterialTheme.colorScheme.tertiary
                    index == currentStep -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                shape = MaterialTheme.shapes.small
            ) {}
        }
    }
}

// Step 1: Tag Selection with Chips
@Composable
private fun Step1TagSelection(
    availableTags: List<PostTag>,
    selectedTag: PostTag?,
    onTagSelected: (PostTag) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.create_post_tag_selection_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = stringResource(id = R.string.create_post_tag_helper),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Chip group for tags - using Cards for better visual weight
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableTags.forEach { tag ->
                val isSelected = selectedTag == tag
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagSelected(tag) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Text(
                            text = stringResource(id = tag.toLabelResId()),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// Step 2: Content Input
@Composable
private fun Step2ContentInput(
    title: String,
    body: String,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    tfColors: androidx.compose.material3.TextFieldColors,
    boxShape: RoundedCornerShape,
    borderColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.create_post_content_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.create_post_title_label)) },
            placeholder = { Text(text = stringResource(id = R.string.create_post_title_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            shape = MaterialTheme.shapes.medium
        )
        
        OutlinedTextField(
            value = body,
            onValueChange = onBodyChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(id = R.string.create_post_body_label)) },
            placeholder = { Text(text = stringResource(id = R.string.create_post_body_placeholder)) },
            singleLine = false,
            minLines = 8,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Default,
                keyboardType = KeyboardType.Text
            ),
            shape = MaterialTheme.shapes.medium
        )
    }
}

// Step 3: Images
@Composable
private fun Step3Images(
    uiState: CreateUiState,
    onImageSelected: (Uri) -> Unit,
    onRemoveImage: (Uri) -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.create_post_images_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = stringResource(id = R.string.create_post_images_helper),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ImagePickerSection(
            imageUris = uiState.imageUris,
            onPickImage = { imagePickerLauncher.launch("image/*") },
            onRemoveImage = onRemoveImage,
            showHeader = false
        )
    }
}

// Step 4: Preview
@Composable
private fun Step4Preview(
    uiState: CreateUiState,
    modifier: Modifier = Modifier
) {
    val tagLabel = uiState.selectedTag?.let { stringResource(id = it.toLabelResId()) }
    val displayTitle = uiState.title.ifBlank { stringResource(id = R.string.create_post_preview_empty_title) }
    val displayBody = uiState.body.ifBlank { stringResource(id = R.string.create_post_preview_empty_body) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.create_post_review_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ForumAuthorAvatar(
                        name = stringResource(id = R.string.create_post_preview_author),
                        avatarUrl = null,
                        modifier = Modifier.size(48.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        ForumAuthorLink(
                            name = stringResource(id = R.string.create_post_preview_author),
                            onClick = null
                        )
                        Text(
                            text = stringResource(id = R.string.create_post_preview_meta_now),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val safeTag = tagLabel ?: stringResource(id = R.string.create_post_preview_tag_placeholder)
                    ForumTagLabel(label = safeTag)
                }

                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = displayBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (uiState.imageUris.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = uiState.imageUris.first()),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSelectorDropdown(
    tags: List<PostTag>,
    selectedTag: PostTag?,
    onTagSelected: (PostTag) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(id = R.string.create_post_tag_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        val tfColors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            Box(
                modifier = Modifier
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
            ) {
                TextField(
                    value = selectedTag?.let { stringResource(id = it.toLabelResId()) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    colors = tfColors,
                    singleLine = true
                )
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                tags.forEach { tag ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = tag.toLabelResId())) },
                        onClick = {
                            onTagSelected(tag)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePickerSection(
    imageUris: List<Uri>,
    onPickImage: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val maxImages = 5
    val canAddMore = imageUris.size < maxImages
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Scroll to end when a new image is added
    LaunchedEffect(imageUris.size) {
        if (imageUris.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(imageUris.size)
            }
        }
    }
    
    // Check if scrollable
    val canScrollLeft = listState.canScrollBackward
    val canScrollRight = listState.canScrollForward
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showHeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.create_post_attachment_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (imageUris.isNotEmpty()) {
                    Text(
                        text = "${imageUris.size}/$maxImages",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(imageUris, key = { it.toString() }) { uri ->
                    ImagePreviewCard(
                        imageUri = uri,
                        onRemove = { onRemoveImage(uri) }
                    )
                }
                
                // Add image button inline
                if (canAddMore) {
                    item {
                        AddImageCard(onClick = onPickImage)
                    }
                }
            }
            
            // Left scroll indicator
            if (canScrollLeft) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Scroll left",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(24.dp)
                    )
                }
            }
            
            // Right scroll indicator
            if (canScrollRight) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Scroll right",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(24.dp)
                    )
                }
            }
        }
        
        // Show message when limit reached
        if (!canAddMore) {
            Text(
                text = stringResource(id = R.string.create_post_max_images_reached),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImagePreviewCard(
    imageUri: Uri,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(120.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(id = R.string.create_post_remove_image_content_description, ""),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(4.dp).size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddImageCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.size(120.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = stringResource(id = R.string.create_post_add_image_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        imageUris = emptyList(),
        availableTags = listOf(
            PostTag.AskQuestion,
            PostTag.Tutorial,
            PostTag.Resource
        ),
        selectedTag = PostTag.AskQuestion
    )

    EnglishForumTheme {
        CreateScreen(
            uiState = previewState,
            onTitleChange = {},
            onBodyChange = {},
            onAddAttachment = {},
            onRemoveAttachment = {},
            onImageSelected = {},
            onRemoveImage = {},
            onTagSelected = {},
            onSubmit = {},
            onDeclineReasonDismissed = {},
            onErrorMessageConsumed = {},
            onSuccessMessageConsumed = {}
        )
    }
}
