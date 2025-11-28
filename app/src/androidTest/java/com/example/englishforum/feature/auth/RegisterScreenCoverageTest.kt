package com.example.englishforum.feature.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.englishforum.data.auth.FakeAuthRepository
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

class RegisterScreenCoverageTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun registerScreen_happyPath_rendersAndTriggersCallbacks() {
        var cancelCalled = false
        val viewModel = RegisterViewModel(FakeAuthRepository())

        composeRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = { cancelCalled = true }
            )
        }

        composeRule.onNodeWithTag("register_username_field").performTextInput("newuser123")
        composeRule.onNodeWithTag("register_email_field").performTextInput("user@example.com")
        composeRule.onNodeWithTag("register_password_field").performTextInput("StrongPass")
        composeRule.onNodeWithTag("register_confirm_password_field").performTextInput("StrongPass")

        // Toggle password visibility text to hit branch
        composeRule.onAllNodesWithText("Hiện", useUnmergedTree = true)
            .onFirst()
            .performClick()
        assertTrue(
            composeRule.onAllNodesWithText("Ẩn", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        )

        composeRule.onNodeWithTag("register_button").performClick()

        // Wait until loading completes (FakeAuthRepository has delay)
        composeRule.waitUntil(timeoutMillis = 4_000) { !viewModel.uiState.isLoading }
        composeRule.onNodeWithTag("register_error").assertDoesNotExist()

        // RegisterScreen shows a success message when verification is needed
        composeRule.waitUntil(timeoutMillis = 4_000) {
            viewModel.uiState.requiresVerification
        }
        composeRule.onNodeWithTag("register_success").assertExists()

        composeRule.onNodeWithTag("register_login_button").performClick()
        assert(cancelCalled)
    }

    @Test
    fun registerScreen_invalidInput_showsError() {
        val viewModel = RegisterViewModel(FakeAuthRepository())

        composeRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        composeRule.onNodeWithTag("register_username_field").performTextInput("short")
        composeRule.onNodeWithTag("register_email_field").performTextInput("invalid-email")
        composeRule.onNodeWithTag("register_password_field").performTextInput("123")
        composeRule.onNodeWithTag("register_confirm_password_field").performTextInput("123")

        composeRule.onNodeWithTag("register_button").performClick()

        composeRule.waitUntil(timeoutMillis = 2_000) {
            viewModel.uiState.errorMessage != null
        }
        composeRule.onNodeWithTag("register_error").assertIsDisplayed()

        // Button should remain enabled once validation fails
        composeRule.onNodeWithTag("register_button").assertIsEnabled()
    }
}
