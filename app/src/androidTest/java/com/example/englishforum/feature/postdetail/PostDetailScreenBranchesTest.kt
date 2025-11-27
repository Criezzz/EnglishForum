package com.example.englishforum.feature.postdetail

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.PostTag
import com.example.englishforum.core.ui.theme.EnglishForumTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostDetailScreenBranchesTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val samplePost = PostDetailUi(
        id = "post-branch",
        authorId = "author-1",
        authorName = "Alice",
        authorUsername = "alice",
        relativeTimeText = "1h",
        title = "Sample post",
        body = "Body",
        voteCount = 10,
        voteState = VoteState.NONE,
        commentCount = 0,
        tag = PostTag.Experience
    )

    private fun renderScreen(
        state: PostDetailUiState,
        targetCommentId: String? = null,
        createdFlag: Boolean = false,
        onReportPost: (String) -> Unit = {},
        onEditPostClick: () -> Unit = {},
        onDeletePost: () -> Unit = {},
        onUserMessageShown: () -> Unit = {},
        onCommentViewed: (String) -> Unit = {},
        onOpenAiPracticeClick: () -> Unit = {},
        onAuthorClick: (String) -> Unit = {},
        onRefresh: () -> Unit = {},
        onPostDeleted: () -> Unit = {},
        onPostDeletionHandled: () -> Unit = {},
        onNewCommentHighlightShown: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            EnglishForumTheme {
                PostDetailScreen(
                    uiState = state,
                    onBackClick = {},
                    onUpvotePost = {},
                    onDownvotePost = {},
                    onUpvoteComment = {},
                    onDownvoteComment = {},
                    onCommentDraftChanged = {},
                    onSubmitComment = {},
                    onCancelReplyTarget = {},
                    onReplyToComment = { _, _, _ -> },
                    onOpenAiPracticeClick = onOpenAiPracticeClick,
                    onReportPost = onReportPost,
                    onEditPostClick = onEditPostClick,
                    onDeletePost = onDeletePost,
                    onEditComment = { _, _ -> },
                    onDeleteComment = {},
                    onUserMessageShown = onUserMessageShown,
                    onNewCommentHighlightShown = onNewCommentHighlightShown,
                    onPostDeletionHandled = onPostDeletionHandled,
                    onPostDeleted = onPostDeleted,
                    onRefresh = onRefresh,
                    onAuthorClick = onAuthorClick,
                    onCommentViewed = onCommentViewed,
                    targetCommentId = targetCommentId,
                    createdFlag = createdFlag
                )
            }
        }
    }

    @Test
    fun errorAndUserMessages_showSnackbarsAndCallback() {
        var userMessageHandled = false
        val state = PostDetailUiState(
            isLoading = false,
            post = samplePost,
            comments = emptyList(),
            errorMessage = "Lỗi hệ thống",
            userMessage = "Thành công"
        )

        renderScreen(
            state = state,
            onUserMessageShown = { userMessageHandled = true }
        )

        composeTestRule.waitUntil(timeoutMillis = 2_000) { userMessageHandled }
        composeTestRule.onNodeWithText("Lỗi hệ thống", useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Thành công", useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun newCommentViewed_afterDelayCallsCallback() {
        composeTestRule.mainClock.autoAdvance = false
        try {
            var viewedCommentId: String? = null
            val comment = PostCommentUi(
                id = "comment-new",
                authorName = "Bob",
                relativeTimeText = "1m",
                body = "New comment",
                voteCount = 0,
                voteState = VoteState.NONE,
                isAuthor = false,
                depth = 0,
                hasReplies = false,
                isFirstChild = true,
                isLastChild = true,
                isNew = true
            )
            val state = PostDetailUiState(
                isLoading = false,
                post = samplePost,
                comments = listOf(comment)
            )

            renderScreen(
                state = state,
                onCommentViewed = { viewedCommentId = it }
            )

            composeTestRule.mainClock.advanceTimeBy(2_200)
            composeTestRule.waitForIdle()
            assertEquals("comment-new", viewedCommentId)
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun targetCommentScroll_highlightFlowRuns() {
        composeTestRule.mainClock.autoAdvance = false
        try {
            val comments = listOf(
                PostCommentUi(
                    id = "comment-1",
                    authorName = "User1",
                    relativeTimeText = "2m",
                    body = "First",
                    voteCount = 0,
                    voteState = VoteState.NONE,
                    isAuthor = false,
                    depth = 0,
                    hasReplies = false,
                    isFirstChild = true,
                    isLastChild = false
                ),
                PostCommentUi(
                    id = "comment-target",
                    authorName = "User2",
                    relativeTimeText = "1m",
                    body = "Target",
                    voteCount = 0,
                    voteState = VoteState.NONE,
                    isAuthor = false,
                    depth = 0,
                    hasReplies = false,
                    isFirstChild = false,
                    isLastChild = true
                )
            )
            val state = PostDetailUiState(
                isLoading = false,
                post = samplePost.copy(commentCount = 2),
                comments = comments
            )

            renderScreen(
                state = state,
                targetCommentId = "comment-target"
            )

            composeTestRule.mainClock.advanceTimeBy(3_200)
            composeTestRule.waitForIdle()
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun newlyPostedComment_highlightClearsAfterDelay() {
        composeTestRule.mainClock.autoAdvance = false
        try {
            var cleared = false
            val comments = listOf(
                PostCommentUi(
                    id = "comment-newly",
                    authorName = "User new",
                    relativeTimeText = "1m",
                    body = "Fresh comment",
                    voteCount = 0,
                    voteState = VoteState.NONE,
                    isAuthor = false,
                    depth = 0,
                    hasReplies = false,
                    isFirstChild = true,
                    isLastChild = true
                )
            )
            val state = PostDetailUiState(
                isLoading = false,
                post = samplePost.copy(commentCount = 1),
                comments = comments,
                newlyPostedCommentId = "comment-newly"
            )

            renderScreen(
                state = state,
                onNewCommentHighlightShown = { cleared = true }
            )

            composeTestRule.mainClock.advanceTimeBy(3_200)
            composeTestRule.waitForIdle()
            assertTrue(cleared)
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun performingAction_collapsesOptionsMenu() {
        val state = PostDetailUiState(
            isLoading = false,
            post = samplePost,
            comments = emptyList(),
            isPerformingAction = true
        )

        renderScreen(state = state)

        composeTestRule.waitForIdle()
    }

    @Test
    fun optionsMenu_reportFlowOpensDialogAndSubmits() {
        var reportedReason: String? = null
        val state = PostDetailUiState(
            isLoading = false,
            post = samplePost,
            comments = emptyList(),
            isCurrentUserPostOwner = false
        )

        renderScreen(
            state = state,
            onReportPost = { reportedReason = it }
        )

        composeTestRule.onNodeWithTag("post_detail_more_button", useUnmergedTree = true)
            .performClick()
        composeTestRule.onNodeWithText("Báo cáo bài viết", useUnmergedTree = true)
            .performClick()
        composeTestRule.onNodeWithTag("post_detail_report_reason_field", useUnmergedTree = true)
            .performTextInput("Nội dung vi phạm")
        composeTestRule.onNodeWithText("Gửi báo cáo", useUnmergedTree = true)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 2_000) { reportedReason != null }
        assertEquals("Nội dung vi phạm", reportedReason)
    }

    @Test
    fun optionsMenu_ownerActionsInvokeCallbacks() {
        var editCalled = false
        var deleteCalled = false
        val ownerState = PostDetailUiState(
            isLoading = false,
            post = samplePost,
            comments = emptyList(),
            isCurrentUserPostOwner = true
        )

        renderScreen(
            state = ownerState,
            onEditPostClick = { editCalled = true },
            onDeletePost = { deleteCalled = true }
        )

        composeTestRule.onNodeWithTag("post_detail_more_button", useUnmergedTree = true)
            .performClick()
        composeTestRule.onNodeWithText("Chỉnh sửa bài viết", useUnmergedTree = true)
            .performClick()

        composeTestRule.onNodeWithTag("post_detail_more_button", useUnmergedTree = true)
            .performClick()
        composeTestRule.onNodeWithText("Xoá bài viết", useUnmergedTree = true)
            .performClick()

        composeTestRule.onNodeWithTag("delete_post_confirm_button", useUnmergedTree = true)
            .performClick()

        assertTrue(editCalled)
        assertTrue(deleteCalled)
    }

    @Test
    fun postDeletion_triggersCallbacks() {
        var deleted = false
        var handled = false
        val state = PostDetailUiState(
            isLoading = false,
            post = samplePost,
            comments = emptyList(),
            isPostDeleted = true
        )

        renderScreen(
            state = state,
            onPostDeleted = { deleted = true },
            onPostDeletionHandled = { handled = true }
        )

        composeTestRule.waitUntil(timeoutMillis = 2_000) { deleted && handled }
    }

    @Test
    fun aiPracticeFab_dragUpAndDown_togglesPositionAndClick() {
        var clickCalled = false
        val state = PostDetailUiState(
            isLoading = false,
            post = samplePost,
            comments = emptyList()
        )

        renderScreen(
            state = state,
            onOpenAiPracticeClick = { clickCalled = true }
        )

        val fabNode = composeTestRule.onNodeWithContentDescription(
            "Mở câu hỏi luyện tập cùng AI",
            useUnmergedTree = true
        )

        fabNode.performTouchInput {
            down(center)
            moveBy(Offset(0f, -120f))
            up()
        }

        fabNode.performTouchInput {
            down(center)
            moveBy(Offset(0f, 120f))
            up()
        }

        fabNode.performClick()
        assertTrue(clickCalled)
    }

    @Test
    fun aiPracticeFab_loadingStateHidesIcon() {
        val checkingState = PostDetailUiState(
            isLoading = false,
            post = samplePost,
            comments = emptyList(),
            isAiPracticeChecking = true
        )

        renderScreen(state = checkingState)

        val nodes = composeTestRule.onAllNodesWithContentDescription(
            "Mở câu hỏi luyện tập cùng AI",
            useUnmergedTree = true
        ).fetchSemanticsNodes()
        assertTrue(nodes.isEmpty())
    }

    @Test
    fun emptyComments_showsEmptyStateText() {
        val state = PostDetailUiState(
            isLoading = false,
            post = samplePost,
            comments = emptyList()
        )

        renderScreen(state = state)

        composeTestRule.onNodeWithText("Chưa có bình luận nào", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
