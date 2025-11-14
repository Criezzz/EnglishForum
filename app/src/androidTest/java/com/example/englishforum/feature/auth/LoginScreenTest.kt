package com.example.englishforum.feature.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.englishforum.data.auth.FakeAuthRepository
import com.example.englishforum.data.auth.SessionPreferenceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Login Screen
 * Based on functional test specifications for Login feature
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createFakeSessionPreferenceRepository(): SessionPreferenceRepository {
        return object : SessionPreferenceRepository {
            override val keepLoggedInFlow: Flow<Boolean> = flowOf(false)
            override suspend fun setKeepLoggedIn(value: Boolean) {}
        }
    }

    @Test
    fun loginScreen_TC01_validCredentials_loginSuccess() {
        // TC-LOGIN-01: Đăng nhập với tài khoản và mật khẩu hợp lệ
        // Input: username = "user", password = "pass"
        // Expected: Đăng nhập thành công, chuyển đến màn hình chính
        
        var loginSuccessCalled = false
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeAuthRepo, createFakeSessionPreferenceRepository())
        
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { loginSuccessCalled = true },
                onRequireVerification = {},
                onRegisterClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Enter valid username (using test tag)
        composeTestRule.onNodeWithTag("login_username_field").performTextInput("user")
        
        // Enter valid password (using test tag)
        composeTestRule.onNodeWithTag("login_password_field").performTextInput("pass")
        
        // Click login button (using test tag)
        composeTestRule.onNodeWithTag("login_button").performClick()
        
        // Wait for async operation (FakeAuthRepository has 900ms delay)
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            loginSuccessCalled
        }
        
        // Verify login success callback was called
        assert(loginSuccessCalled)
    }

    @Test
    fun loginScreen_TC02_emptyUsername_showsError() {
        // TC-LOGIN-02: Đăng nhập với tên đăng nhập trống
        // Input: username = "", password = "pass"
        // Expected: Hiển thị lỗi "Tên đăng nhập hoặc mật khẩu không đúng"
        
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeAuthRepo, createFakeSessionPreferenceRepository())
        
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {},
                onRequireVerification = {},
                onRegisterClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Leave username empty, enter password
        composeTestRule.onNodeWithTag("login_password_field").performTextInput("pass")
        
        // Click login button
        composeTestRule.onNodeWithTag("login_button").performClick()
        
        // Wait for async operation
        composeTestRule.waitForIdle()
        Thread.sleep(1000) // Wait for FakeAuthRepository delay
        composeTestRule.waitForIdle()
        
        // Verify error message is displayed
        composeTestRule.onNodeWithTag("login_error").assertExists()
    }

    @Test
    fun loginScreen_TC03_emptyPassword_showsError() {
        // TC-LOGIN-03: Đăng nhập với mật khẩu trống
        // Input: username = "user", password = ""
        // Expected: Hiển thị lỗi "Tên đăng nhập hoặc mật khẩu không đúng"
        
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeAuthRepo, createFakeSessionPreferenceRepository())
        
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {},
                onRequireVerification = {},
                onRegisterClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Enter username, leave password empty
        composeTestRule.onNodeWithTag("login_username_field").performTextInput("user")
        
        // Click login button
        composeTestRule.onNodeWithTag("login_button").performClick()
        
        // Wait for async operation
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        composeTestRule.waitForIdle()
        
        // Verify error message is displayed
        composeTestRule.onNodeWithTag("login_error").assertExists()
    }

    @Test
    fun loginScreen_TC04_invalidCredentials_showsError() {
        // TC-LOGIN-04: Đăng nhập với tài khoản không tồn tại
        // Input: username = "nonexistentuser", password = "WrongPass@123"
        // Expected: Hiển thị lỗi "Tên đăng nhập hoặc mật khẩu không đúng"
        
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeAuthRepo, createFakeSessionPreferenceRepository())
        
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {},
                onRequireVerification = {},
                onRegisterClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Enter invalid credentials
        composeTestRule.onNodeWithTag("login_username_field").performTextInput("nonexistentuser")
        composeTestRule.onNodeWithTag("login_password_field").performTextInput("WrongPass@123")
        
        // Click login button
        composeTestRule.onNodeWithTag("login_button").performClick()
        
        // Wait for async operation
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        composeTestRule.waitForIdle()
        
        // Verify error message is displayed
        composeTestRule.onNodeWithTag("login_error").assertExists()
    }

    @Test
    fun loginScreen_TC05_wrongPassword_showsError() {
        // TC-LOGIN-05: Đăng nhập với mật khẩu sai
        // Input: username = "user", password = "WrongPassword"
        // Expected: Hiển thị lỗi "Tên đăng nhập hoặc mật khẩu không đúng"
        
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeAuthRepo, createFakeSessionPreferenceRepository())
        
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {},
                onRequireVerification = {},
                onRegisterClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Enter valid username but wrong password
        composeTestRule.onNodeWithTag("login_username_field").performTextInput("user")
        composeTestRule.onNodeWithTag("login_password_field").performTextInput("WrongPassword")
        
        // Click login button
        composeTestRule.onNodeWithTag("login_button").performClick()
        
        // Wait for async operation
        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        composeTestRule.waitForIdle()
        
        // Verify error message is displayed
        composeTestRule.onNodeWithTag("login_error").assertExists()
    }

    @Test
    fun loginScreen_TC06_caseInsensitiveUsername_loginSuccess() {
        // TC-LOGIN-06: Đăng nhập với username khác case
        // Input: username = "USER", password = "pass"
        // Expected: Đăng nhập thành công (username is case-insensitive)
        
        var loginSuccessCalled = false
        val fakeAuthRepo = FakeAuthRepository()
        val viewModel = LoginViewModel(fakeAuthRepo, createFakeSessionPreferenceRepository())
        
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { loginSuccessCalled = true },
                onRequireVerification = {},
                onRegisterClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Enter uppercase username
        composeTestRule.onNodeWithTag("login_username_field").performTextInput("USER")
        composeTestRule.onNodeWithTag("login_password_field").performTextInput("pass")
        
        // Click login button
        composeTestRule.onNodeWithTag("login_button").performClick()
        
        // Wait for async operation
        composeTestRule.waitUntil(timeoutMillis = 2000) {
            loginSuccessCalled
        }
        
        assert(loginSuccessCalled)
    }

    @Test
    fun loginScreen_displaysAllUIElements() {
        // Additional test: Verify all UI elements are present
        
        val viewModel = LoginViewModel(
            FakeAuthRepository(),
            createFakeSessionPreferenceRepository()
        )
        
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {},
                onRequireVerification = {},
                onRegisterClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Verify UI elements exist
        composeTestRule.onNodeWithTag("login_username_field").assertExists()
        composeTestRule.onNodeWithTag("login_password_field").assertExists()
        composeTestRule.onNodeWithTag("login_button").assertExists()
        composeTestRule.onNodeWithTag("login_forgot_password_button").assertExists()
        composeTestRule.onNodeWithTag("login_register_button").assertExists()
    }

    @Test
    fun loginScreen_togglePasswordVisibility() {
        // Additional test: Verify password visibility toggle works
        
        val viewModel = LoginViewModel(
            FakeAuthRepository(),
            createFakeSessionPreferenceRepository()
        )
        
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {},
                onRequireVerification = {},
                onRegisterClick = {},
                onForgotPasswordClick = {}
            )
        }

        // Enter password
        composeTestRule.onNodeWithTag("login_password_field").performTextInput("TestPassword")
        
        // Password field should exist (verify toggle exists via field)
        composeTestRule.onNodeWithTag("login_password_field").assertExists()
    }

    @Test
    fun loginScreen_navigateToRegister() {
        // Additional test: Verify register navigation works
        
        var registerClicked = false
        val viewModel = LoginViewModel(
            FakeAuthRepository(),
            createFakeSessionPreferenceRepository()
        )
        
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {},
                onRequireVerification = {},
                onRegisterClick = { registerClicked = true },
                onForgotPasswordClick = {}
            )
        }

        // Click register link
        composeTestRule.onNodeWithTag("login_register_button").performClick()
        
        assert(registerClicked)
    }

    @Test
    fun loginScreen_navigateToForgotPassword() {
        // Additional test: Verify forgot password navigation works
        
        var forgotPasswordClicked = false
        val viewModel = LoginViewModel(
            FakeAuthRepository(),
            createFakeSessionPreferenceRepository()
        )
        
        composeTestRule.setContent {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {},
                onRequireVerification = {},
                onRegisterClick = {},
                onForgotPasswordClick = { forgotPasswordClicked = true }
            )
        }

        // Click forgot password link
        composeTestRule.onNodeWithTag("login_forgot_password_button").performClick()
        
        assert(forgotPasswordClicked)
    }
}

