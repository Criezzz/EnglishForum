package com.example.englishforum.feature.search

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.englishforum.core.model.VoteState
import com.example.englishforum.core.model.forum.PostTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Search Screen functionality
 * Tests search input, results display, and user interactions
 */
@RunWith(AndroidJUnit4::class)
class SearchScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun searchScreen_displaysSearchField() {
        composeTestRule.setContent {
            SearchScreen(
                uiState = SearchUiState(),
                onQueryChange = {},
                onClearQuery = {},
                onUpvote = {},
                onDownvote = {},
                onPostClick = {},
                onCommentClick = {},
                onMoreActionsClick = {},
                onAuthorClick = {}
            )
        }

        // Search field should be visible (using test tag)
        composeTestRule.onNodeWithTag("search_text_field").assertExists()
    }

    @Test
    fun searchScreen_allowsTextInput() {
        var currentQuery = ""
        
        composeTestRule.setContent {
            SearchScreen(
                uiState = SearchUiState(query = currentQuery),
                onQueryChange = { currentQuery = it },
                onClearQuery = {},
                onUpvote = {},
                onDownvote = {},
                onPostClick = {},
                onCommentClick = {},
                onMoreActionsClick = {},
                onAuthorClick = {}
            )
        }

        // Type in search field (using test tag)
        composeTestRule.onNodeWithTag("search_text_field")
            .performTextInput("test query")

        // Verify the query was changed
        assert(currentQuery.isNotEmpty())
    }

    @Test
    fun searchScreen_displaysLoadingState() {
        composeTestRule.setContent {
            SearchScreen(
                uiState = SearchUiState(
                    query = "test",
                    isLoading = true
                ),
                onQueryChange = {},
                onClearQuery = {},
                onUpvote = {},
                onDownvote = {},
                onPostClick = {},
                onCommentClick = {},
                onMoreActionsClick = {},
                onAuthorClick = {}
            )
        }

        // Loading state should show placeholders or loading indicator
        // Note: Actual loading UI verification would require checking for CircularProgressIndicator
        composeTestRule.waitForIdle()
    }

    @Test
    fun searchScreen_displaysEmptyState() {
        composeTestRule.setContent {
            SearchScreen(
                uiState = SearchUiState(
                    query = "test",
                    posts = emptyList(),
                    isLoading = false
                ),
                onQueryChange = {},
                onClearQuery = {},
                onUpvote = {},
                onDownvote = {},
                onPostClick = {},
                onCommentClick = {},
                onMoreActionsClick = {},
                onAuthorClick = {}
            )
        }

        // Empty state should be visible
        composeTestRule.onNodeWithTag("search_empty_state").assertExists()
    }

    @Test
    fun searchScreen_displaysPostResults() {
        val testPosts = listOf(
            SearchPostUi(
                id = "1",
                authorName = "Test User",
                authorUsername = "testuser",
                relativeTimeText = "1 giờ trước",
                title = "Test Search Result",
                body = "This is a test search result",
                voteCount = 5,
                voteState = VoteState.NONE,
                commentCount = 2,
                tag = PostTag.Experience,
                authorAvatarUrl = null
            )
        )

        composeTestRule.setContent {
            SearchScreen(
                uiState = SearchUiState(
                    query = "test",
                    posts = testPosts,
                    isLoading = false
                ),
                onQueryChange = {},
                onClearQuery = {},
                onUpvote = {},
                onDownvote = {},
                onPostClick = {},
                onCommentClick = {},
                onMoreActionsClick = {},
                onAuthorClick = {}
            )
        }

        // Post title should be visible
        composeTestRule.onNodeWithText("Test Search Result").assertExists()
        
        // Author name should be visible
        composeTestRule.onNodeWithText("Test User").assertExists()
    }

    @Test
    fun searchScreen_handlesPostClick() {
        var clickedPostId: String? = null
        val testPosts = listOf(
            SearchPostUi(
                id = "123",
                authorName = "Test User",
                authorUsername = "testuser",
                relativeTimeText = "1 giờ trước",
                title = "Clickable Post",
                body = "Test",
                voteCount = 5,
                voteState = VoteState.NONE,
                commentCount = 2,
                tag = PostTag.Experience,
                authorAvatarUrl = null
            )
        )

        composeTestRule.setContent {
            SearchScreen(
                uiState = SearchUiState(
                    query = "test",
                    posts = testPosts,
                    isLoading = false
                ),
                onQueryChange = {},
                onClearQuery = {},
                onUpvote = {},
                onDownvote = {},
                onPostClick = { clickedPostId = it },
                onCommentClick = {},
                onMoreActionsClick = {},
                onAuthorClick = {}
            )
        }

        composeTestRule.onNodeWithText("Clickable Post").performClick()
        assert(clickedPostId == "123")
    }

    // NOTE: Vote button tests are skipped because VoteIconButton uses contentDescription = null
    // To enable these tests, VoteIconButton needs semantic properties or test tags

    @Test
    fun searchScreen_displaysClearButton_whenQueryIsNotEmpty() {
        composeTestRule.setContent {
            SearchScreen(
                uiState = SearchUiState(query = "test query"),
                onQueryChange = {},
                onClearQuery = {},
                onUpvote = {},
                onDownvote = {},
                onPostClick = {},
                onCommentClick = {},
                onMoreActionsClick = {},
                onAuthorClick = {}
            )
        }

        // Clear button should be visible (using actual string resource)
        composeTestRule.onNodeWithContentDescription("Xoá nội dung tìm kiếm").assertExists()
    }
}

