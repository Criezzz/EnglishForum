package com.example.englishforum.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.englishforum.R
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.ui.components.card.ForumContentCard
import com.example.englishforum.core.ui.theme.EnglishForumTheme

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
    onSettingsClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onPostMoreClick: (ProfilePost) -> Unit = {},
    onReplyMoreClick: (ProfileReply) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    ProfileContent(
        modifier = modifier,
        uiState = uiState,
        onSettingsClick = onSettingsClick,
        onEditClick = onEditClick,
        onPostMoreClick = onPostMoreClick,
        onReplyMoreClick = onReplyMoreClick,
        onPostUpvote = viewModel::onPostUpvote,
        onPostDownvote = viewModel::onPostDownvote,
        onReplyUpvote = viewModel::onReplyUpvote,
        onReplyDownvote = viewModel::onReplyDownvote
    )
}

@Composable
private fun ProfileContent(
    modifier: Modifier = Modifier,
    uiState: ProfileUiState,
    onSettingsClick: () -> Unit,
    onEditClick: () -> Unit,
    onPostMoreClick: (ProfilePost) -> Unit,
    onReplyMoreClick: (ProfileReply) -> Unit,
    onPostUpvote: (String) -> Unit,
    onPostDownvote: (String) -> Unit,
    onReplyUpvote: (String) -> Unit,
    onReplyDownvote: (String) -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(ProfileTab.Posts.ordinal) }
    val tabs = ProfileTab.entries.toList()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProfileHeader(
                overview = uiState.overview,
                isLoading = uiState.isLoading,
                onSettingsClick = onSettingsClick,
                onEditClick = onEditClick,
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
                            meta = stringResource(R.string.profile_post_meta, post.minutesAgo),
                            voteCount = post.voteCount,
                            title = post.title,
                            body = post.body,
                            voteState = post.voteState,
                            onUpvoteClick = { onPostUpvote(post.id) },
                            onDownvoteClick = { onPostDownvote(post.id) },
                            onMoreActionsClick = { onPostMoreClick(post) }
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
                        ForumContentCard(
                            modifier = Modifier
                                .padding(horizontal = 16.dp),
                            meta = stringResource(R.string.profile_reply_meta, reply.questionTitle, reply.minutesAgo),
                            voteCount = reply.voteCount,
                            title = reply.questionTitle,
                            body = reply.body,
                            voteState = reply.voteState,
                            onUpvoteClick = { onReplyUpvote(reply.id) },
                            onDownvoteClick = { onReplyDownvote(reply.id) },
                            onMoreActionsClick = { onReplyMoreClick(reply) }
                        )
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
    onSettingsClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        when {
            isLoading && overview == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            overview != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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

                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Text(
                        text = overview.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ProfileStatsRow(stats = overview.stats)
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "-")
                }
            }
        }
    }
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

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    EnglishForumTheme {
        ProfileContent(
            uiState = ProfileUiState(
                overview = ProfileOverview(
                    displayName = "A_Great_Name",
                    stats = ProfileStats(
                        upvotes = 57,
                        posts = 12,
                        answers = 25
                    )
                ),
                posts = listOf(
                    ProfilePost(
                        id = "post_1",
                        title = "Lorem ipsum dolor sit amet",
                        body = "Nullam justo felis, ullamcorper et lectus non, vestibulum feugiat risus.",
                        minutesAgo = 13,
                        voteCount = 17,
                        voteState = VoteState.UPVOTED
                    ),
                    ProfilePost(
                        id = "post_2",
                        title = "In mattis tincidunt mi ac pretium",
                        body = "Nullam euismod urna in arcu mollis, at consectetur ante mattis.",
                        minutesAgo = 28,
                        voteCount = 9,
                        voteState = VoteState.NONE
                    )
                ),
                replies = listOf(
                    ProfileReply(
                        id = "reply_1",
                        questionTitle = "Câu hỏi về ABC",
                        body = "Donec dictum rhoncus eros, eget fermentum dui laoreet a.",
                        minutesAgo = 12,
                        voteCount = 6,
                        voteState = VoteState.UPVOTED
                    )
                ),
                isLoading = false
            ),
            onSettingsClick = {},
            onEditClick = {},
            onPostMoreClick = {},
            onReplyMoreClick = {},
            onPostUpvote = {},
            onPostDownvote = {},
            onReplyUpvote = {},
            onReplyDownvote = {}
        )
    }
}
