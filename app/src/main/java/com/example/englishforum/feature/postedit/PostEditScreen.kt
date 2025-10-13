package com.example.englishforum.feature.postedit

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.englishforum.R
import com.example.englishforum.core.di.LocalAppContainer
import com.example.englishforum.feature.create.CreateScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostEditRoute(
    postId: String,
    onBackClick: () -> Unit,
    onPostUpdated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appContainer = LocalAppContainer.current
    val viewModel: PostEditViewModel = viewModel(
        factory = remember(postId, appContainer) {
            PostEditViewModelFactory(postId, appContainer.postDetailRepository)
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.successPostId) {
        if (uiState.successPostId != null) {
            onPostUpdated()
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
        onDeclineReasonDismissed = viewModel::onDeclineDialogDismissed,
        onErrorMessageConsumed = viewModel::onErrorMessageConsumed,
        topBarTitleRes = R.string.post_edit_title,
        submitLabelRes = R.string.post_edit_save,
        submitLoadingLabelRes = R.string.post_edit_saving,
        onBackClick = onBackClick
    )
}
