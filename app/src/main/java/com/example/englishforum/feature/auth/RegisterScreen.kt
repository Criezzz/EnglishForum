package com.example.englishforum.feature.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.englishforum.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onRegisterSuccess: () -> Unit,
    onCancel: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showDatePicker by remember { mutableStateOf(false) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val uiState = viewModel.uiState

    // Handle successful registration
    LaunchedEffect(uiState.isRegistrationComplete) {
        if (uiState.isRegistrationComplete) {
            onRegisterSuccess()
        }
    }

    val initialMillis = remember(uiState.dob) {
        try {
            if (uiState.dob.isNotBlank()) {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.parse(uiState.dob)?.time
            } else null
        } catch (_: Exception) {
            null
        }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) {
                        val cal = Calendar.getInstance().apply { timeInMillis = selected }
                        val picked = String.format(
                            Locale.getDefault(),
                            "%02d/%02d/%04d",
                            cal.get(Calendar.DAY_OF_MONTH),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.YEAR)
                        )
                        viewModel.onDobChange(picked)
                    }
                    showDatePicker = false
                }) { Text(text = stringResource(R.string.auth_ok_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = stringResource(R.string.auth_cancel_action))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = stringResource(R.string.auth_logo_content_description),
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (!uiState.isOtpRequested) {
            RegisterFormStep(
                uiState = uiState,
                viewModel = viewModel,
                passwordVisible = passwordVisible,
                onPasswordVisibleChange = { passwordVisible = it },
                confirmPasswordVisible = confirmPasswordVisible,
                onConfirmPasswordVisibleChange = { confirmPasswordVisible = it },
                onShowDatePicker = { showDatePicker = true },
                onCancel = onCancel
            )
        } else {
            OtpVerificationStep(
                uiState = uiState,
                viewModel = viewModel,
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun RegisterFormStep(
    uiState: RegisterUiState,
    viewModel: RegisterViewModel,
    passwordVisible: Boolean,
    onPasswordVisibleChange: (Boolean) -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibleChange: (Boolean) -> Unit,
    onShowDatePicker: () -> Unit,
    onCancel: () -> Unit
) {

    OutlinedTextField(
        value = uiState.name,
        onValueChange = { viewModel.onNameChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(R.string.auth_full_name_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { })
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = uiState.phone,
        onValueChange = { viewModel.onPhoneChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(R.string.auth_phone_number_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { })
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = uiState.email,
        onValueChange = { viewModel.onEmailChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(R.string.auth_email_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { })
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = uiState.password,
        onValueChange = viewModel::onPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(R.string.auth_password_label)) },
        singleLine = true,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val hideText = stringResource(R.string.auth_toggle_hide)
            val showText = stringResource(R.string.auth_toggle_show)
            Text(
                text = if (passwordVisible) hideText else showText,
                modifier = Modifier
                    .clickable { onPasswordVisibleChange(!passwordVisible) }
                    .padding(end = 8.dp)
            )
        },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { })
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = uiState.confirmPassword,
        onValueChange = viewModel::onConfirmPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(R.string.auth_confirm_password_label)) },
        singleLine = true,
        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val hideText = stringResource(R.string.auth_toggle_hide)
            val showText = stringResource(R.string.auth_toggle_show)
            Text(
                text = if (confirmPasswordVisible) hideText else showText,
                modifier = Modifier
                    .clickable { onConfirmPasswordVisibleChange(!confirmPasswordVisible) }
                    .padding(end = 8.dp)
            )
        },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onShowDatePicker() })
    )

    Spacer(modifier = Modifier.height(12.dp))

    val dateInteractionSource = remember { MutableInteractionSource() }

    OutlinedTextField(
        value = uiState.dob,
        onValueChange = { /* read-only */ },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = dateInteractionSource,
                indication = null
            ) { onShowDatePicker() },
        label = { Text(text = stringResource(R.string.auth_date_of_birth_label)) },
        readOnly = true,
        singleLine = true,
        trailingIcon = {
            TextButton(onClick = { onShowDatePicker() }) {
                Text(text = stringResource(R.string.auth_select_action))
            }
        },
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { viewModel.register() })
    )

    Spacer(modifier = Modifier.height(16.dp))

    uiState.errorMessage?.let { err ->
        Text(text = err, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
    }

    uiState.successMessage?.let { msg ->
        Text(text = msg, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
    }

    Button(
        onClick = { viewModel.register() },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        enabled = !uiState.isLoading
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(text = stringResource(R.string.auth_register))
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    TextButton(onClick = onCancel) {
        Text(text = stringResource(R.string.auth_cancel_action))
    }
}

@Composable
private fun OtpVerificationStep(
    uiState: RegisterUiState,
    viewModel: RegisterViewModel,
    onCancel: () -> Unit
) {
    Text(
        text = "Xác thực OTP",
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Nhập mã OTP đã được gửi đến ${uiState.email}",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedTextField(
        value = uiState.otp,
        onValueChange = { viewModel.onOtpChange(it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = "Mã OTP") },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { /* OTP auto-verifies when complete */ })
    )

    Spacer(modifier = Modifier.height(16.dp))

    uiState.otpErrorMessage?.let { err ->
        Text(text = err, color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
    }

    uiState.successMessage?.let { msg ->
        Text(text = msg, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
    }

    if (uiState.isRegistering) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Đang đăng ký...")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Resend OTP button
    if (uiState.otpSecondsRemaining > 0) {
        Text(
            text = "Gửi lại mã sau ${uiState.otpSecondsRemaining}s",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    } else {
        TextButton(onClick = { viewModel.requestNewOtp() }) {
            Text(text = "Gửi lại mã OTP")
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    TextButton(onClick = onCancel) {
        Text(text = stringResource(R.string.auth_cancel_action))
    }
}
