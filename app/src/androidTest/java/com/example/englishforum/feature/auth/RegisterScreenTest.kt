package com.example.englishforum.feature.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.englishforum.data.auth.FakeAuthRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Register Screen
 * Based on functional test specifications for Register feature
 */
@RunWith(AndroidJUnit4::class)
class RegisterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun registerScreen_TC01_validInputs_registrationSuccess() {
        // TC-REGISTER-01: Đăng ký với thông tin hợp lệ
        // Input: username = "newuser123", email = "newuser@example.com",
        //        password = "Strong@123", confirmPassword = "Strong@123"
        // Expected: Đăng ký thành công, chuyển sang xác thực email

        var verificationRequired = false
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = RegisterViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = { verificationRequired = true },
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        // Enter valid registration data (username >= 8 chars, valid email, password >= 8 chars)
        composeTestRule.onNodeWithTag("register_username_field").performTextInput("newuser123")
        composeTestRule.onNodeWithTag("register_email_field").performTextInput("newuser@example.com")
        composeTestRule.onNodeWithTag("register_password_field").performTextInput("Strong@123")
        composeTestRule.onNodeWithTag("register_confirm_password_field").performTextInput("Strong@123")

        // Click register button
        composeTestRule.onNodeWithTag("register_button").performClick()

        // Wait for async operation (FakeAuthRepository has 1000ms delay)wwwww
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            verificationRequired
        }

        // Verify verification required
        assert(verificationRequired)
    }

    @Test
    fun registerScreen_TC02_emptyUsername_showsError() {
        // TC-REGISTER-02: Đăng ký với tên đăng nhập trống
        // Input: username = "", email = "test@example.com",
        //        password = "Test@123", confirmPassword = "Test@123"
        // Expected: Hiển thị lỗi "Tên đăng nhập không được để trống"

        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = RegisterViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        // Leave username empty
        composeTestRule.onNodeWithTag("register_email_field").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("register_password_field").performTextInput("Test@123")
        composeTestRule.onNodeWithTag("register_confirm_password_field").performTextInput("Test@123")

        // Click register button
        composeTestRule.onNodeWithTag("register_button").performClick()

        // Wait for async operation
        composeTestRule.waitForIdle()

        // Verify error message (ViewModel should validate before API call)
        composeTestRule.onNodeWithTag("register_error").assertExists()
    }

    @Test
    fun registerScreen_TC03_shortUsername_showsError() {
        // TC-REGISTER-03: Đăng ký với tên đăng nhập quá ngắn
        // Input: username = "user", email = "test@example.com",
        //        password = "Test@123", confirmPassword = "Test@123"
        // Expected: Hiển thị lỗi "Tên đăng nhập phải có ít nhất 8 ký tự"

        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = RegisterViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        // Enter username < 8 chars
        composeTestRule.onNodeWithTag("register_username_field").performTextInput("user")
        composeTestRule.onNodeWithTag("register_email_field").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("register_password_field").performTextInput("Test@123")
        composeTestRule.onNodeWithTag("register_confirm_password_field").performTextInput("Test@123")

        // Click register button
        composeTestRule.onNodeWithTag("register_button").performClick()

        // Wait for async operation
        composeTestRule.waitForIdle()
        Thread.sleep(1200)
        composeTestRule.waitForIdle()

        // Verify error message from FakeAuthRepository
        composeTestRule.onNodeWithTag("register_error").assertExists()
    }

    @Test
    fun registerScreen_TC04_invalidEmail_showsError() {
        // TC-REGISTER-04: Đăng ký với email không hợp lệ
        // Input: username = "testuser123", email = "invalidemail",
        //        password = "Test@123", confirmPassword = "Test@123"
        // Expected: Hiển thị lỗi "Email không hợp lệ"

        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = RegisterViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        // Enter invalid email
        composeTestRule.onNodeWithTag("register_username_field").performTextInput("testuser123")
        composeTestRule.onNodeWithTag("register_email_field").performTextInput("invalidemail")
        composeTestRule.onNodeWithTag("register_password_field").performTextInput("Test@123")
        composeTestRule.onNodeWithTag("register_confirm_password_field").performTextInput("Test@123")

        // Click register button
        composeTestRule.onNodeWithTag("register_button").performClick()

        // Wait for async operation
        composeTestRule.waitForIdle()
        Thread.sleep(1200)
        composeTestRule.waitForIdle()

        // Verify error message
        composeTestRule.onNodeWithTag("register_error").assertExists()
    }

    @Test
    fun registerScreen_TC05_weakPassword_showsError() {
        // TC-REGISTER-05: Đăng ký với mật khẩu yếu
        // Input: username = "testuser123", email = "test@example.com",
        //        password = "123", confirmPassword = "123"
        // Expected: Hiển thị lỗi "Mật khẩu phải có ít nhất 8 ký tự"

        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = RegisterViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        // Enter weak password
        composeTestRule.onNodeWithTag("register_username_field").performTextInput("testuser123")
        composeTestRule.onNodeWithTag("register_email_field").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("register_password_field").performTextInput("123")
        composeTestRule.onNodeWithTag("register_confirm_password_field").performTextInput("123")

        // Click register button
        composeTestRule.onNodeWithTag("register_button").performClick()

        // Wait for async operation
        composeTestRule.waitForIdle()
        Thread.sleep(1200)
        composeTestRule.waitForIdle()

        // Verify error message
        composeTestRule.onNodeWithTag("register_error").assertExists()
    }

    @Test
    fun registerScreen_TC06_passwordMismatch_showsError() {
        // TC-REGISTER-06: Đăng ký với mật khẩu xác nhận không khớp
        // Input: username = "testuser123", email = "test@example.com",
        //        password = "Test@123", confirmPassword = "Test@456"
        // Expected: Hiển thị lỗi "Mật khẩu xác nhận không khớp"

        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = RegisterViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        // Enter mismatched passwords
        composeTestRule.onNodeWithTag("register_username_field").performTextInput("testuser123")
        composeTestRule.onNodeWithTag("register_email_field").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("register_password_field").performTextInput("Test@123")
        composeTestRule.onNodeWithTag("register_confirm_password_field").performTextInput("Test@456")

        // Click register button
        composeTestRule.onNodeWithTag("register_button").performClick()

        // Wait for async operation
        composeTestRule.waitForIdle()

        // Verify error message (should be validated by ViewModel)
        composeTestRule.onNodeWithTag("register_error").assertExists()
    }

    @Test
    fun registerScreen_TC07_emptyEmail_showsError() {
        // TC-REGISTER-07: Đăng ký với email trống
        // Input: username = "testuser123", email = "",
        //        password = "Test@123", confirmPassword = "Test@123"
        // Expected: Hiển thị lỗi "Email không được để trống"

        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = RegisterViewModel(fakeAuthRepo)

        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        // Leave email empty
        composeTestRule.onNodeWithTag("register_username_field").performTextInput("testuser123")
        composeTestRule.onNodeWithTag("register_password_field").performTextInput("Test@123")
        composeTestRule.onNodeWithTag("register_confirm_password_field").performTextInput("Test@123")

        // Click register button
        composeTestRule.onNodeWithTag("register_button").performClick()

        // Wait for async operation
        composeTestRule.waitForIdle()
        Thread.sleep(1200)
        composeTestRule.waitForIdle()

        // Verify error message
        composeTestRule.onNodeWithTag("register_error").assertExists()
    }

    @Test
    fun registerScreen_displaysAllUIElements() {
        // Additional test: Verify all UI elements are present

        val viewModel = RegisterViewModel(FakeAuthRepository())

        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        // Verify UI elements exist
        composeTestRule.onNodeWithTag("register_username_field").assertExists()
        composeTestRule.onNodeWithTag("register_email_field").assertExists()
        composeTestRule.onNodeWithTag("register_password_field").assertExists()
        composeTestRule.onNodeWithTag("register_confirm_password_field").assertExists()
        composeTestRule.onNodeWithTag("register_button").assertExists()
    }

    @Test
    fun registerScreen_togglePasswordVisibility() {
        // Additional test: Verify password visibility toggle works

        val viewModel = RegisterViewModel(FakeAuthRepository())

        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        // Enter password
        composeTestRule.onNodeWithTag("register_password_field").performTextInput("TestPassword")

        // Show password button should exist
        composeTestRule.onAllNodesWithText("Hiện")[0].assertExists()
    }

    @Test
    fun registerScreen_navigateToLogin() {
        // Additional test: Verify login navigation works

        var cancelClicked = false
        val viewModel = RegisterViewModel(FakeAuthRepository())

        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = { cancelClicked = true }
            )
        }

        // Click login link
        composeTestRule.onNodeWithTag("register_login_button").performClick()
        
        assert(cancelClicked)
    }

    @Test
    fun registerScreen_clearError_removesError() {
        // Test: Clear error message
        
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = RegisterViewModel(fakeAuthRepo)
        
        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        // Trigger error by entering invalid data
        composeTestRule.onNodeWithTag("register_username_field").performTextInput("user")
        composeTestRule.onNodeWithTag("register_button").performClick()
        
        composeTestRule.waitForIdle()
        Thread.sleep(1200)
        composeTestRule.waitForIdle()
        
        // Verify error exists
        assert(viewModel.uiState.errorMessage != null)
        
        // Clear error
        viewModel.clearError()
        
        // Verify error is cleared
        assert(viewModel.uiState.errorMessage == null)
    }

    @Test
    fun registerScreen_registerWithoutVerification_success() {
        // Test: Register without email verification requirement
        
        var registrationComplete = false
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = RegisterViewModel(fakeAuthRepo)
        
        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = { registrationComplete = true },
                onCancel = {}
            )
        }

        // Enter valid registration data
        composeTestRule.onNodeWithTag("register_username_field").performTextInput("newuser123")
        composeTestRule.onNodeWithTag("register_email_field").performTextInput("newuser@example.com")
        composeTestRule.onNodeWithTag("register_password_field").performTextInput("Strong@123")
        composeTestRule.onNodeWithTag("register_confirm_password_field").performTextInput("Strong@123")
        
        // Click register button
        composeTestRule.onNodeWithTag("register_button").performClick()
        
        // Wait for async operation
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            viewModel.uiState.isRegistrationComplete || registrationComplete
        }
        
        // Note: FakeAuthRepository may always require verification
        // This test verifies the path exists
    }

    @Test
    fun registerScreen_validation_emptyConfirmPassword_showsError() {
        // Test: Validation for empty confirm password
        
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = RegisterViewModel(fakeAuthRepo)
        
        composeTestRule.setContent {
            RegisterScreen(
                viewModel = viewModel,
                onVerificationRequired = {},
                onRegisterSuccess = {},
                onCancel = {}
            )
        }

        // Enter valid data but leave confirm password empty
        composeTestRule.onNodeWithTag("register_username_field").performTextInput("newuser123")
        composeTestRule.onNodeWithTag("register_email_field").performTextInput("newuser@example.com")
        composeTestRule.onNodeWithTag("register_password_field").performTextInput("Strong@123")
        // Leave confirm password empty
        
        // Click register button
        composeTestRule.onNodeWithTag("register_button").performClick()
        
        // Wait for validation
        composeTestRule.waitForIdle()
        
        // Verify error message (ViewModel validates before API call)
        assert(viewModel.uiState.errorMessage != null)
    }
}

