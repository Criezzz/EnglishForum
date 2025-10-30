package com.example.englishforum.feature.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.launch
import com.example.englishforum.R
import com.example.englishforum.core.model.DEFAULT_SEED_COLOR
import com.example.englishforum.core.model.ThemeOption
import com.example.englishforum.core.ui.theme.EnglishForumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: ThemeOption,
    onThemeChange: (ThemeOption) -> Unit,
    isMaterialThemeEnabled: Boolean,
    onMaterialThemeToggle: (Boolean) -> Unit,
    seedColor: Long,
    onSeedColorChange: (Long) -> Unit,
    isAmoledEnabled: Boolean,
    onAmoledToggle: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onPasswordChange: (String, String) -> Unit = { _, _ -> },
    onEmailChange: (String) -> Unit = {},
    onEmailConfirm: (String) -> Unit = {}
) {
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var showEmailDialog by rememberSaveable { mutableStateOf(false) }
    var showSeedColorDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_section_look_and_feel),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                SettingsRow(
                    title = stringResource(R.string.settings_app_theme),
                    subtitle = themeOptionLabel(currentTheme),
                    leadingIconPainter = painterResource(id = R.drawable.ic_settings_theme),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = { showThemeDialog = true },
                )
            }

            item {
                SettingsRow(
                    title = stringResource(R.string.settings_material_theme_title),
                    subtitle = stringResource(
                        if (isMaterialThemeEnabled) {
                            R.string.settings_material_theme_subtitle_on
                        } else {
                            R.string.settings_material_theme_subtitle_off
                        }
                    ),
                    leadingIconPainter = rememberVectorPainter(image = Icons.Outlined.Style),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    toggleState = isMaterialThemeEnabled,
                    onToggle = onMaterialThemeToggle
                )
            }

            if (!isMaterialThemeEnabled) {
                item {
                    SettingsRow(
                        title = stringResource(R.string.settings_seed_color),
                        subtitle = formatSeedColorHex(seedColor),
                        leadingIconPainter = rememberVectorPainter(image = Icons.Outlined.Palette),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        onClick = { showSeedColorDialog = true },
                        trailingContent = {
                            SeedColorPreview(color = Color(colorLongToInt(seedColor)))
                        }
                    )
                }
            }

            item {
                SettingsRow(
                    title = stringResource(R.string.settings_amoled),
                    subtitle = stringResource(R.string.settings_amoled_subtitle),
                    leadingIconPainter = rememberVectorPainter(image = Icons.Outlined.DarkMode),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    toggleState = isAmoledEnabled,
                    onToggle = onAmoledToggle
                )
            }

            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_section_account),
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            item {
                SettingsRow(
                    title = stringResource(R.string.settings_change_password),
                    leadingIconPainter = rememberVectorPainter(image = Icons.Filled.Lock),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = { showPasswordDialog = true },
                )
            }

            item {
                SettingsRow(
                    title = stringResource(R.string.settings_change_email),
                    leadingIconPainter = painterResource(id = R.drawable.ic_settings_email),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    onClick = { showEmailDialog = true },
                )
            }

            item {
                SettingsSectionHeader(
                    title = stringResource(R.string.settings_section_actions),
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            item {
                SettingsRow(
                    title = stringResource(R.string.settings_logout),
                    titleColor = MaterialTheme.colorScheme.error,
                    iconTint = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    leadingIconPainter = painterResource(id = R.drawable.ic_settings_logout),
                    onClick = onLogoutClick
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeSelectionBottomSheet(
            initialSelection = currentTheme,
            onDismiss = { showThemeDialog = false },
            onConfirm = { option ->
                showThemeDialog = false
                onThemeChange(option)
            }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordBottomSheet(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { current, new ->
                showPasswordDialog = false
                onPasswordChange(current, new)
            },
            snackbarHostState = snackbarHostState
        )
    }

    if (showEmailDialog) {
        ChangeEmailBottomSheet(
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

    if (showSeedColorDialog) {
        SeedColorBottomSheet(
            selectedColor = seedColor,
            onSelect = onSeedColorChange,
            onDismiss = { showSeedColorDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelectionBottomSheet(
    initialSelection: ThemeOption,
    onDismiss: () -> Unit,
    onConfirm: (ThemeOption) -> Unit
) {
    var selection by remember(initialSelection) { mutableStateOf(initialSelection) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings_theme),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_theme_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Chọn giao diện ứng dụng",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Theme options
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption.entries.forEach { option ->
                    ThemeOptionCard(
                        themeOption = option,
                        isSelected = selection == option,
                        onClick = { selection = option }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.auth_cancel_action))
                }

                Button(
                    onClick = { onConfirm(selection) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.settings_theme_apply))
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    themeOption: ThemeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = themeOptionLabel(themeOption)
    val icon = when (themeOption) {
        ThemeOption.LIGHT -> Icons.Outlined.Style
        ThemeOption.DARK -> Icons.Outlined.DarkMode
        ThemeOption.FOLLOW_SYSTEM -> Icons.Outlined.Palette
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )
            
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = androidx.compose.material3.RadioButtonDefaults.colors(
                    selectedColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIconPainter: Painter? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
    toggleState: Boolean? = null,
    onToggle: ((Boolean) -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow
) {
    val baseModifier = modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.large)

    val interactiveModifier = when {
        toggleState != null && onToggle != null -> {
            baseModifier.clickable(role = Role.Switch) { onToggle(!toggleState) }
        }

        onClick != null -> baseModifier.clickable(onClick = onClick)
        else -> baseModifier
    }

    val resolvedTrailingContent = when {
        trailingContent != null -> trailingContent
        toggleState != null && onToggle != null -> {
            {
                Switch(
                    checked = toggleState,
                    onCheckedChange = onToggle
                )
            }
        }
        else -> null
    }

    ListItem(
        modifier = interactiveModifier,
        colors = ListItemDefaults.colors(
            containerColor = containerColor,
            headlineColor = titleColor,
            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            leadingIconColor = iconTint,
            trailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor
            )
        },
        supportingContent = if (!subtitle.isNullOrEmpty()) {
            {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            null
        },
        leadingContent = leadingIconPainter?.let { painter ->
            {
                Icon(
                    painter = painter,
                    contentDescription = null
                )
            }
        },
        trailingContent = resolvedTrailingContent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeedColorBottomSheet(
    selectedColor: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialColorInt = remember(selectedColor) { colorLongToInt(selectedColor) }
    var selectedHue by remember { mutableStateOf(0f) }
    var selectedSaturation by remember { mutableStateOf(1f) }
    var selectedLightness by remember { mutableStateOf(0.5f) }
    
    // Initialize from selected color
    LaunchedEffect(selectedColor) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(initialColorInt, hsv)
        selectedHue = hsv[0]
        selectedSaturation = hsv[1]
        selectedLightness = hsv[2]
    }

    val currentColorInt = AndroidColor.HSVToColor(
        floatArrayOf(selectedHue, selectedSaturation, selectedLightness)
    )
    val currentColor = Color(currentColorInt)
    val currentHex = formatColorHex(currentColorInt, includeAlpha = false)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with color preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    color = currentColor,
                    shape = CircleShape,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {}
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_seed_color_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currentHex,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }

            // Preset colors
            Text(
                text = "Màu gợi ý",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(PRESET_COLORS.size) { index ->
                    val presetColor = PRESET_COLORS[index]
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                val hsv = FloatArray(3)
                                AndroidColor.colorToHSV(presetColor, hsv)
                                selectedHue = hsv[0]
                                selectedSaturation = hsv[1]
                                selectedLightness = hsv[2]
                            },
                        color = Color(presetColor),
                        shape = CircleShape,
                        tonalElevation = 2.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            if (currentColorInt == presetColor) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                        )
                    ) {}
                }
            }

            // Sliders
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Hue slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Màu sắc (Hue)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedHue.toInt()}°",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = selectedHue,
                        onValueChange = { selectedHue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Saturation slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Độ bão hòa (Saturation)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(selectedSaturation * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = selectedSaturation,
                        onValueChange = { selectedSaturation = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Lightness slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Độ sáng (Brightness)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(selectedLightness * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = selectedLightness,
                        onValueChange = { selectedLightness = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.auth_cancel_action))
                }

                Button(
                    onClick = {
                        onSelect(colorIntToLong(currentColorInt))
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.settings_theme_apply))
                }
            }
        }
    }
}

private val PRESET_COLORS = listOf(
    0xFFEF5350.toInt(), // Red
    0xFFEC407A.toInt(), // Pink
    0xFFAB47BC.toInt(), // Purple
    0xFF7E57C2.toInt(), // Deep Purple
    0xFF5C6BC0.toInt(), // Indigo
    0xFF42A5F5.toInt(), // Blue
    0xFF29B6F6.toInt(), // Light Blue
    0xFF26C6DA.toInt(), // Cyan
    0xFF26A69A.toInt(), // Teal
    0xFF66BB6A.toInt(), // Green
    0xFF9CCC65.toInt(), // Light Green
    0xFFD4E157.toInt(), // Lime
    0xFFFFEE58.toInt(), // Yellow
    0xFFFFCA28.toInt(), // Amber
    0xFFFF7043.toInt(), // Deep Orange
    0xFF8D6E63.toInt(), // Brown
)

@Composable
private fun SeedColorPreview(color: Color) {
    Surface(
        modifier = Modifier.size(28.dp),
        color = color,
        shape = CircleShape,
        tonalElevation = 4.dp
    ) {}
}

private fun formatSeedColorHex(color: Long): String {
    val argb = colorLongToInt(color)
    val rgb = argb and 0x00FFFFFF
    return String.format("#%06X", rgb)
}

private fun formatColorHex(colorInt: Int, includeAlpha: Boolean = false): String {
    return if (includeAlpha) {
        String.format("#%08X", colorInt)
    } else {
        val rgb = colorInt and 0x00FFFFFF
        String.format("#%06X", rgb)
    }
}

private fun parseHexToColorInt(hex: String): Int? {
    return runCatching { AndroidColor.parseColor("#$hex") }.getOrNull()
}

private fun colorIntToLong(colorInt: Int): Long = colorInt.toLong() and 0xFFFFFFFFL

private fun colorLongToInt(colorLong: Long): Int = (colorLong and 0xFFFFFFFFL).toInt()

private const val HEX_LENGTH = 6
private val HEX_CHAR_SET: Set<Char> = (('0'..'9') + ('A'..'F')).toSet()

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .padding(bottom = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (currentPassword: String, newPassword: String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var currentPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_password_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Cập nhật mật khẩu của bạn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Form fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current password
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text(stringResource(R.string.settings_password_current_label)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                            Icon(
                                imageVector = if (currentPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (currentPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                            )
                        }
                    },
                    visualTransformation = if (currentPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium
                )

                // New password
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.settings_password_new_label)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (newPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                            )
                        }
                    },
                    visualTransformation = if (newPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium
                )

                // Confirm password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.settings_password_confirm_label)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu"
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = MaterialTheme.shapes.medium,
                    isError = confirmPassword.isNotEmpty() && newPassword != confirmPassword,
                    supportingText = if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                        { Text("Mật khẩu không khớp") }
                    } else null
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.auth_cancel_action))
                }

                Button(
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
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(text = stringResource(R.string.settings_password_change_button))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeEmailBottomSheet(
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header with icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = if (isOtpSent) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isOtpSent) Icons.Filled.CheckCircle else Icons.Filled.Email,
                            contentDescription = null,
                            tint = if (isOtpSent) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isOtpSent) "Xác nhận mã OTP" else stringResource(R.string.settings_email_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isOtpSent) "Nhập mã đã gửi đến email của bạn" else "Cập nhật địa chỉ email của bạn",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Step indicator
            if (!isOtpSent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step 1 - Active
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {}
                    
                    // Step 2 - Inactive
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {}
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step 1 - Completed
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = MaterialTheme.shapes.small
                    ) {}
                    
                    // Step 2 - Active
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {}
                }
            }

            // Form fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!isOtpSent) {
                    // Email input
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text(stringResource(R.string.settings_email_new_label)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            capitalization = KeyboardCapitalization.None
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    // Info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Mã xác nhận sẽ được gửi đến email mới của bạn",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                } else {
                    // OTP input
                    OutlinedTextField(
                        value = otp,
                        onValueChange = { otp = it },
                        label = { Text(stringResource(R.string.settings_email_otp_label)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = MaterialTheme.shapes.medium
                    )
                    
                    // Success card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Email đã được gửi",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Kiểm tra hộp thư của bạn tại: $newEmail",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.auth_cancel_action))
                }

                if (!isOtpSent) {
                    Button(
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
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(text = stringResource(R.string.settings_email_send_otp_button))
                        }
                    }
                } else {
                    Button(
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
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(text = stringResource(R.string.settings_email_confirm_button))
                        }
                    }
                }
            }
        }
    }
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
            isMaterialThemeEnabled = false,
            onMaterialThemeToggle = {},
            seedColor = DEFAULT_SEED_COLOR,
            onSeedColorChange = {},
            isAmoledEnabled = false,
            onAmoledToggle = {},
            onBackClick = {},
            onLogoutClick = {}
        )
    }
}
