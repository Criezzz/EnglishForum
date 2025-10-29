package com.example.englishforum.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.englishforum.R
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.ui.components.VoteIconButton
import com.example.englishforum.core.ui.components.card.ForumContentCard
import com.example.englishforum.core.ui.components.image.AuthenticatedRemoteImage
import com.example.englishforum.core.ui.components.image.ForumPostPreviewImage
import com.example.englishforum.core.ui.theme.EnglishForumTheme

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    userId: String?,
    onSettingsClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onPostClick: (String) -> Unit = {},
    onReplyClick: (String, String?) -> Unit = { _, _ -> },
    onPostMoreClick: (ProfilePost) -> Unit = {},
    onReplyMoreClick: (ProfileReply) -> Unit = {},
    isOwnProfile: Boolean = true,
    onBackClick: () -> Unit = {}
) {
    val appContainer = LocalAppContainer.current
    val resolvedUserId = userId ?: "user-1"
    val viewModel: ProfileViewModel = viewModel(
        factory = remember(appContainer, resolvedUserId) {
            ProfileViewModelFactory(
                repository = appContainer.profileRepository,
                userId = resolvedUserId
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val avatarState by viewModel.avatarState.collectAsState()
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(viewModel::onAvatarSelected)
    }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(editState) {
        if (editState is ProfileEditState.Success) {
            showEditDialog = false
            viewModel.resetEditState()
        }
    }

    LaunchedEffect(showEditDialog) {
        if (!showEditDialog) {
            viewModel.resetAvatarState()
        }
    }

    val canEditProfile = isOwnProfile

    val handleSettingsClick: () -> Unit = {
        if (canEditProfile) {
            onSettingsClick()
        }
    }
    val handleEditClick: () -> Unit = editClick@{
        if (!canEditProfile) return@editClick
        onEditClick()
        viewModel.resetEditState()
        showEditDialog = true
    }
    ProfileContent(
        modifier = modifier,
        uiState = uiState,
        showAccountActions = canEditProfile,
        showBackButton = !canEditProfile,
        onSettingsClick = handleSettingsClick,
        onEditClick = handleEditClick,
        onBackClick = onBackClick,
        onPostClick = onPostClick,
        onReplyClick = onReplyClick,
        onPostMoreClick = onPostMoreClick,
        onReplyMoreClick = onReplyMoreClick,
        onPostUpvote = viewModel::onPostUpvote,
        onPostDownvote = viewModel::onPostDownvote,
        onReplyUpvote = viewModel::onReplyUpvote,
        onReplyDownvote = viewModel::onReplyDownvote,
        onRefresh = viewModel::onRefresh
    )

    val overview = uiState.overview
    if (canEditProfile && showEditDialog && overview != null) {
        ProfileEditDialog(
            currentName = overview.displayName,
            currentBio = overview.bio.orEmpty(),
            avatarUrl = overview.avatarUrl,
            avatarState = avatarState,
            isSaving = editState is ProfileEditState.InProgress,
            errorMessage = (editState as? ProfileEditState.Error)?.message,
            onDismiss = {
                showEditDialog = false
                viewModel.resetEditState()
                viewModel.resetAvatarState()
            },
            onSave = { name, bio ->
                viewModel.updateProfile(name, bio)
            },
            onChangePhoto = {
                if (canEditProfile) {
                    avatarPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileContent(
    modifier: Modifier = Modifier,
    uiState: ProfileUiState,
    showAccountActions: Boolean,
    showBackButton: Boolean,
    onSettingsClick: () -> Unit,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit,
    onPostClick: (String) -> Unit,
    onReplyClick: (String, String?) -> Unit,
    onPostMoreClick: (ProfilePost) -> Unit,
    onReplyMoreClick: (ProfileReply) -> Unit,
    onPostUpvote: (String) -> Unit,
    onPostDownvote: (String) -> Unit,
    onReplyUpvote: (String) -> Unit,
    onReplyDownvote: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(ProfileTab.Posts.ordinal) }
    val tabs = ProfileTab.entries.toList()
    val pullRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        state = pullRefreshState,
        isRefreshing = uiState.isRefreshing,
        onRefresh = {
            if (!uiState.isLoading) {
                onRefresh()
            }
        },
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullRefreshState,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            ProfileHeader(
                overview = uiState.overview,
                isLoading = uiState.isLoading,
                showAccountActions = showAccountActions,
                showBackButton = showBackButton,
                onSettingsClick = onSettingsClick,
                onEditClick = onEditClick,
                onBackClick = onBackClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        item {
            ProfileTabs(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        when (tabs[selectedTabIndex]) {
            ProfileTab.Posts -> {
                if (uiState.posts.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyState(text = stringResource(R.string.profile_empty_posts))
                    }
                } else {
                    items(uiState.posts, key = { it.id }) { post ->
                        ForumContentCard(
                            modifier = Modifier
                                .padding(horizontal = 16.dp),
                            meta = stringResource(R.string.profile_post_meta, post.timeLabel),
                            voteCount = post.voteCount,
                            title = post.title,
                            body = post.body,
                            voteState = post.voteState,
                            onCardClick = { onPostClick(post.id) },
                            onUpvoteClick = { onPostUpvote(post.id) },
                            onDownvoteClick = { onPostDownvote(post.id) },
                            onMoreActionsClick = { onPostMoreClick(post) },
                            supportingContent = {
                                post.previewImageUrl?.let { previewUrl ->
                                    ForumPostPreviewImage(imageUrl = previewUrl)
                                }
                            }
                        )
                    }
                }
            }

            ProfileTab.Replies -> {
                if (uiState.replies.isEmpty() && !uiState.isLoading) {
                    item {
                        EmptyState(text = stringResource(R.string.profile_empty_replies))
                    }
                } else {
                    items(uiState.replies, key = { it.id }) { reply ->
                        ProfileReplyCard(
                            modifier = Modifier
                                .padding(horizontal = 16.dp),
                            reply = reply,
                            onClick = { onReplyClick(reply.postId, reply.id) },
                            onUpvoteClick = { onReplyUpvote(reply.id) },
                            onDownvoteClick = { onReplyDownvote(reply.id) }
                        )
                    }
                }
            }
        }
    }
    }
}

private enum class ProfileTab(val labelRes: Int) {
    Posts(R.string.profile_tab_posts),
    Replies(R.string.profile_tab_replies)
}

@Composable
private fun ProfileTabs(
    tabs: List<ProfileTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = Color.Transparent,
        indicator = { tabPositions ->
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex])
            )
        }
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedTabIndex
            Tab(
                selected = selected,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = stringResource(tab.labelRes),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    overview: ProfileOverview?,
    isLoading: Boolean,
    showAccountActions: Boolean,
    showBackButton: Boolean,
    onSettingsClick: () -> Unit,
    onEditClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                showAccountActions -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.profile_settings_content_description)
                            )
                        }
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.profile_edit_content_description)
                            )
                        }
                    }
                }

                showBackButton -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back)
                            )
                        }
                    }
                }
            }

            when {
                isLoading && overview == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                overview != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileAvatar(
                            avatarUrl = overview.avatarUrl,
                            previewUri = null,
                            isUploading = false,
                            size = 96.dp
                        )

                        Text(
                            text = overview.displayName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        ProfileBioSection(bio = overview.bio)

                        ProfileStatsRow(stats = overview.stats)
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "-")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileBioSection(bio: String?) {
    val trimmed = bio?.trim().orEmpty()
    val hasBio = trimmed.isNotEmpty()
    val displayText = if (hasBio) trimmed else stringResource(R.string.profile_bio_empty)
    val textColor = if (hasBio) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        text = displayText,
        style = MaterialTheme.typography.bodyMedium,
        color = textColor,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ProfileStatsRow(stats: ProfileStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ProfileStatItem(value = stats.upvotes, label = stringResource(R.string.profile_stat_upvotes))
        ProfileStatItem(value = stats.posts, label = stringResource(R.string.profile_stat_posts))
        ProfileStatItem(value = stats.answers, label = stringResource(R.string.profile_stat_answers))
    }
}

@Composable
private fun ProfileStatItem(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfileReplyCard(
    reply: ProfileReply,
    onClick: () -> Unit,
    onUpvoteClick: () -> Unit,
    onDownvoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.profile_reply_meta, reply.questionTitle, reply.timeLabel),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = reply.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))

                VoteIconButton(
                    icon = Icons.Filled.KeyboardArrowUp,
                    contentDescription = null,
                    selected = reply.voteState == VoteState.UPVOTED,
                    onClick = onUpvoteClick
                )
                Spacer(Modifier.width(4.dp))

                Text(
                    text = reply.voteCount.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.width(4.dp))
                VoteIconButton(
                    icon = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    selected = reply.voteState == VoteState.DOWNVOTED,
                    onClick = onDownvoteClick
                )
            }
        }
    }
}

@Composable
private fun EmptyState(text: String) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 32.dp),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    previewUri: Uri?,
    isUploading: Boolean,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.size(size),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                previewUri != null -> {
                    Image(
                        painter = rememberAsyncImagePainter(previewUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                avatarUrl != null -> {
                    AuthenticatedRemoteImage(
                        url = avatarUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = CircleShape
                        )
                )
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    EnglishForumTheme {
        ProfileContent(
            uiState = ProfileUiState(
                overview = ProfileOverview(
                    displayName = "A_Great_Name",
                    bio = "Certified English tutor who loves helping learners build confidence in conversation.",
                    stats = ProfileStats(
                        upvotes = 57,
                        posts = 12,
                        answers = 25
                    )
                ),
                posts = listOf(
                    ProfilePost(
                        id = "post-1",
                        title = "Lorem ipsum dolor sit amet",
                        body = "Nullam justo felis, ullamcorper et lectus non, vestibulum feugiat risus.",
                        timeLabel = "13 phút trước",
                        voteCount = 17,
                        voteState = VoteState.UPVOTED
                    ),
                    ProfilePost(
                        id = "post-2",
                        title = "In mattis tincidunt mi ac pretium",
                        body = "Nullam euismod urna in arcu mollis, at consectetur ante mattis.",
                        timeLabel = "12/05/2024 14:30",
                        voteCount = 9,
                        voteState = VoteState.NONE
                    )
                ),
                replies = listOf(
                    ProfileReply(
                        id = "comment-1",
                        postId = "post-1",
                        questionTitle = "Câu hỏi về ABC",
                        body = "Donec dictum rhoncus eros, eget fermentum dui laoreet a.",
                        timeLabel = "55 phút trước",
                        voteCount = 6,
                        voteState = VoteState.UPVOTED
                    )
            ),
            isLoading = false
        ),
        showAccountActions = true,
        showBackButton = false,
        onSettingsClick = {},
        onEditClick = {},
        onBackClick = {},
        onPostClick = {},
        onReplyClick = { _, _ -> },
        onPostMoreClick = {},
        onReplyMoreClick = {},
            onPostUpvote = {},
            onPostDownvote = {},
            onReplyUpvote = {},
            onReplyDownvote = {},
            onRefresh = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileEditDialog(
    currentName: String,
    currentBio: String,
    avatarUrl: String?,
    avatarState: ProfileAvatarUiState,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
    onChangePhoto: () -> Unit
) {
    var editedName by rememberSaveable(currentName) { mutableStateOf(currentName) }
    var editedBio by rememberSaveable(currentBio) { mutableStateOf(currentBio.take(MAX_PROFILE_BIO_LENGTH)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chỉnh sửa hồ sơ",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cập nhật thông tin cá nhân",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.profile_edit_close_content_description)
                    )
                }
            }

            // Avatar section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProfileAvatar(
                    avatarUrl = avatarUrl,
                    previewUri = avatarState.previewUri,
                    isUploading = avatarState.isUploading,
                    size = 96.dp
                )

                FilledTonalButton(
                    onClick = onChangePhoto,
                    enabled = !avatarState.isUploading,
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_profile_upload),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(R.string.profile_edit_change_photo))
                }

                avatarState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Form fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_edit_display_name_label)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (editedName.isNotBlank()) {
                            IconButton(
                                onClick = { editedName = "" },
                                colors = IconButtonDefaults.iconButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.profile_edit_clear_name)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = editedBio,
                    onValueChange = { newValue ->
                        editedBio = if (newValue.length <= MAX_PROFILE_BIO_LENGTH) {
                            newValue
                        } else {
                            newValue.take(MAX_PROFILE_BIO_LENGTH)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.profile_edit_bio_label)) },
                    placeholder = { Text(stringResource(R.string.profile_edit_bio_placeholder)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.profile_edit_bio_supporting, MAX_PROFILE_BIO_LENGTH),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${editedBio.length}/$MAX_PROFILE_BIO_LENGTH",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Action buttons
            val trimmedName = editedName.trim()
            val trimmedBio = editedBio.trim()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(text = stringResource(R.string.auth_cancel_action))
                }

                Button(
                    onClick = { onSave(trimmedName, trimmedBio) },
                    enabled = trimmedName.isNotBlank() && !isSaving,
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(text = stringResource(R.string.profile_edit_saving))
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = stringResource(R.string.profile_edit_save))
                    }
                }
            }
        }
    }
}
