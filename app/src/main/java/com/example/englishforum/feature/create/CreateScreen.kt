package com.example.englishforum.feature.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import com.example.englishforum.R
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import com.example.englishforum.core.ui.toLabelResId

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
        onImageSelected = viewModel::onImageSelected,
        onRemoveImage = viewModel::onRemoveImage,
        onTagSelected = viewModel::onTagSelected,
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
    onImageSelected: (Uri) -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onTagSelected: (PostTag) -> Unit,
    onSubmit: () -> Unit,
    onDeclineReasonDismissed: () -> Unit,
    onErrorMessageConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    @StringRes topBarTitleRes: Int = R.string.create_post_title,
    @StringRes submitLabelRes: Int = R.string.create_post_submit,
    @StringRes submitLoadingLabelRes: Int = R.string.create_post_submit_loading,
    onBackClick: (() -> Unit)? = null
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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = topBarTitleRes)) },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.auth_back_action)
                            )
                        }
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
            // Tag selector at the top
            if (uiState.availableTags.isNotEmpty()) {
                TagSelectorDropdown(
                    tags = uiState.availableTags,
                    selectedTag = uiState.selectedTag,
                    onTagSelected = onTagSelected
                )
            }
            
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
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            TextField(
                value = selectedTag?.let { stringResource(id = it.toLabelResId()) } ?: "",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                trailingIcon = { 
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
                },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                singleLine = true
            )
            
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
    modifier: Modifier = Modifier
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
            onErrorMessageConsumed = {}
        )
    }
}
