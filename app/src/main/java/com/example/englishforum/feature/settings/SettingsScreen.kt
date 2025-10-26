package com.example.englishforum.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.englishforum.R
import com.example.englishforum.core.model.ThemeOption
import com.example.englishforum.core.ui.theme.EnglishForumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeOption,
    onThemeChange: (ThemeOption) -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onPasswordChange: (String, String) -> Unit = { _, _ -> },
    onEmailChange: (String) -> Unit = {},
    onEmailConfirm: (String) -> Unit = {}
) {
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var showEmailDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp)
        ) {
            item {
                SettingsRow(
                    title = stringResource(R.string.settings_theme),
                    subtitle = themeOptionLabel(currentTheme),
                    leadingPainterRes = R.drawable.ic_settings_theme,
                    onClick = { showThemeDialog = true },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            item {
                SettingsRow(
                    title = stringResource(R.string.settings_change_password),
                    leadingPainterRes = R.drawable.ic_settings_password,
                    onClick = { showPasswordDialog = true },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            item {
                SettingsRow(
                    title = stringResource(R.string.settings_change_email),
                    leadingPainterRes = R.drawable.ic_settings_email,
                    onClick = { showEmailDialog = true },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            item {
                SettingsRow(
                    title = stringResource(R.string.settings_logout),
                    titleColor = MaterialTheme.colorScheme.error,
                    leadingPainterRes = R.drawable.ic_settings_logout,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = onLogoutClick
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            initialSelection = currentTheme,
            onDismiss = { showThemeDialog = false },
            onConfirm = { option ->
                showThemeDialog = false
                onThemeChange(option)
            }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { current, new ->
                showPasswordDialog = false
                onPasswordChange(current, new)
            },
            snackbarHostState = snackbarHostState
        )
    }

    if (showEmailDialog) {
        ChangeEmailDialog(
            onDismiss = { showEmailDialog = false },
            onSendOtp = { email ->
                onEmailChange(email)
            },
            onConfirm = { otp ->
                showEmailDialog = false
                onEmailConfirm(otp)
            },
            snackbarHostState = snackbarHostState
        )
    }
}

@Composable
private fun ThemeSelectionDialog(
    initialSelection: ThemeOption,
    onDismiss: () -> Unit,
    onConfirm: (ThemeOption) -> Unit
) {
    var selection by remember(initialSelection) { mutableStateOf(initialSelection) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_theme_dialog_title))
        },
        text = {
            Column {
                ThemeOption.entries.forEach { option ->
                    ThemeOptionRow(
                        themeOption = option,
                        isSelected = selection == option,
                        onClick = { selection = option }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selection) }) {
                Text(text = stringResource(R.string.settings_theme_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.auth_cancel_action))
            }
        }
    )
}

@Composable
private fun ThemeOptionRow(
    themeOption: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = themeOptionLabel(themeOption)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingPainterRes: Int,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val shape = MaterialTheme.shapes.large
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = MaterialTheme.shapes.medium,
            color = iconTint.copy(alpha = 0.12f)
        ) {
            Icon(
                painter = painterResource(id = leadingPainterRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.padding(10.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = titleColor
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String, newPassword: String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_password_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.settings_password_current_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.settings_password_new_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.settings_password_confirm_label)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Vui lòng điền đầy đủ thông tin"
                                )
                            }
                        }
                        newPassword != confirmPassword -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Mật khẩu mới không khớp"
                                )
                            }
                        }
                        else -> {
                            isLoading = true
                            onConfirm(currentPassword, newPassword)
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(R.string.settings_password_change_button))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(text = stringResource(R.string.auth_cancel_action))
            }
        }
    )
}

@Composable
private fun ChangeEmailDialog(
    onDismiss: () -> Unit,
    onSendOtp: (email: String) -> Unit,
    onConfirm: (otp: String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var newEmail by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var isOtpSent by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_email_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text(stringResource(R.string.settings_email_new_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading && !isOtpSent
                )
                
                if (isOtpSent) {
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text(stringResource(R.string.settings_email_otp_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                }
            }
        },
        confirmButton = {
            if (!isOtpSent) {
                TextButton(
                    onClick = {
                        if (newEmail.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Vui lòng nhập email"
                                )
                            }
                        } else {
                            isLoading = true
                            onSendOtp(newEmail)
                            isOtpSent = true
                            isLoading = false
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Mã xác nhận đã được gửi đến email mới"
                                )
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(R.string.settings_email_send_otp_button))
                    }
                }
            } else {
                TextButton(
                    onClick = {
                        if (otp.isBlank()) {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Vui lòng nhập mã xác nhận"
                                )
                            }
                        } else {
                            isLoading = true
                            onConfirm(otp)
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(R.string.settings_email_confirm_button))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(text = stringResource(R.string.auth_cancel_action))
            }
        }
    )
}

@Composable
private fun themeOptionLabel(option: ThemeOption): String {
    return when (option) {
        ThemeOption.FOLLOW_SYSTEM -> stringResource(R.string.settings_theme_follow_system)
        ThemeOption.LIGHT -> stringResource(R.string.settings_theme_light)
        ThemeOption.DARK -> stringResource(R.string.settings_theme_dark)
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    EnglishForumTheme {
        SettingsScreen(
            currentTheme = ThemeOption.FOLLOW_SYSTEM,
            onThemeChange = {},
            onBackClick = {},
            onLogoutClick = {}
        )
    }
}
