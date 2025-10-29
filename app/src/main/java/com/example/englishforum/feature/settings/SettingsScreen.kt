package com.example.englishforum.feature.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
                    leadingIconPainter = painterResource(id = R.drawable.ic_settings_password),
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

    if (showSeedColorDialog) {
        SeedColorDialog(
            selectedColor = seedColor,
            onSelect = onSeedColorChange,
            onDismiss = { showSeedColorDialog = false }
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

@Composable
private fun SeedColorDialog(
    selectedColor: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialColorInt = remember(selectedColor) { colorLongToInt(selectedColor) }
    var selectedHue by remember { mutableStateOf(0f) }
    var selectedSaturation by remember { mutableStateOf(1f) }
    var selectedValue by remember { mutableStateOf(1f) }
    var alpha by remember { 
        mutableStateOf(((initialColorInt shr 24) and 0xFF) / 255f)
    }
    
    // Initialize from selected color
    remember(selectedColor) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(initialColorInt, hsv)
        selectedHue = hsv[0]
        selectedSaturation = hsv[1]
        selectedValue = hsv[2]
        alpha = ((initialColorInt shr 24) and 0xFF) / 255f
    }

    val currentColorInt = AndroidColor.HSVToColor(
        (alpha * 255).toInt(),
        floatArrayOf(selectedHue, selectedSaturation, selectedValue)
    )
    val currentColor = Color(currentColorInt)
    val currentHex = formatColorHex(currentColorInt, includeAlpha = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_seed_color_dialog_title))
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Color preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        color = currentColor,
                        shape = CircleShape,
                        tonalElevation = 4.dp,
                        shadowElevation = 2.dp
                    ) {}
                    Column {
                        Text(
                            text = currentHex,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Opacity: ${(alpha * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Color palette picker
                ColorPalettePicker(
                    selectedHue = selectedHue,
                    selectedSaturation = selectedSaturation,
                    selectedValue = selectedValue,
                    onColorSelected = { hue, saturation, value ->
                        selectedHue = hue
                        selectedSaturation = saturation
                        selectedValue = value
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                // Hue slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Hue",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    HueSlider(
                        hue = selectedHue,
                        onHueChange = { selectedHue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    )
                }

                // Transparency slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Transparency",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TransparencySlider(
                        alpha = alpha,
                        baseColor = Color(
                            AndroidColor.HSVToColor(
                                floatArrayOf(selectedHue, selectedSaturation, selectedValue)
                            )
                        ),
                        onAlphaChange = { alpha = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSelect(colorIntToLong(currentColorInt))
                    onDismiss()
                }
            ) {
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

@Composable
private fun ColorPalettePicker(
    selectedHue: Float,
    selectedSaturation: Float,
    selectedValue: Float,
    onColorSelected: (hue: Float, saturation: Float, value: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val pureHueColor = remember(selectedHue) {
        Color(AndroidColor.HSVToColor(floatArrayOf(selectedHue, 1f, 1f)))
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedHue) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val saturation = (offset.x / size.width).coerceIn(0f, 1f)
                            val value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                            onColorSelected(selectedHue, saturation, value)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                            val value = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                            onColorSelected(selectedHue, saturation, value)
                        }
                    )
                }
                .pointerInput(selectedHue) {
                    detectTapGestures { offset ->
                        val saturation = (offset.x / size.width).coerceIn(0f, 1f)
                        val value = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        onColorSelected(selectedHue, saturation, value)
                    }
                }
        ) {
            // Draw horizontal saturation gradient (white to pure hue)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, pureHueColor)
                )
            )

            // Draw vertical value gradient (transparent to black)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )

            // Draw selection indicator
            val selectedX = selectedSaturation * size.width
            val selectedY = (1f - selectedValue) * size.height
            drawCircle(
                color = Color.White,
                radius = 12f,
                center = Offset(selectedX, selectedY),
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = Color.Black,
                radius = 12f,
                center = Offset(selectedX, selectedY),
                style = Stroke(width = 1.5f)
            )
        }
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val hueColors = remember {
        (0..360 step 10).map { h ->
            Color(AndroidColor.HSVToColor(floatArrayOf(h.toFloat(), 1f, 1f)))
        }
    }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newHue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                            onHueChange(newHue)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val newHue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                            onHueChange(newHue)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newHue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                        onHueChange(newHue)
                    }
                }
        ) {
            // Draw hue gradient
            drawRect(
                brush = Brush.horizontalGradient(colors = hueColors)
            )

            // Draw selection indicator
            val selectedX = (hue / 360f) * size.width
            drawLine(
                color = Color.White,
                start = Offset(selectedX, 0f),
                end = Offset(selectedX, size.height),
                strokeWidth = 4f
            )
            drawLine(
                color = Color.Black,
                start = Offset(selectedX, 0f),
                end = Offset(selectedX, size.height),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun TransparencySlider(
    alpha: Float,
    baseColor: Color,
    onAlphaChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newAlpha = (offset.x / size.width).coerceIn(0f, 1f)
                            onAlphaChange(newAlpha)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val newAlpha = (change.position.x / size.width).coerceIn(0f, 1f)
                            onAlphaChange(newAlpha)
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newAlpha = (offset.x / size.width).coerceIn(0f, 1f)
                        onAlphaChange(newAlpha)
                    }
                }
        ) {
            // Draw checkerboard pattern for transparency background
            val checkerSize = 8f
            val numX = (size.width / checkerSize).toInt()
            val numY = (size.height / checkerSize).toInt()
            for (x in 0..numX) {
                for (y in 0..numY) {
                    if ((x + y) % 2 == 0) {
                        drawRect(
                            color = Color.LightGray,
                            topLeft = Offset(x * checkerSize, y * checkerSize),
                            size = androidx.compose.ui.geometry.Size(checkerSize, checkerSize)
                        )
                    }
                }
            }

            // Draw alpha gradient
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        baseColor.copy(alpha = 0f),
                        baseColor.copy(alpha = 1f)
                    )
                )
            )

            // Draw selection indicator
            val selectedX = alpha * size.width
            drawLine(
                color = Color.White,
                start = Offset(selectedX, 0f),
                end = Offset(selectedX, size.height),
                strokeWidth = 4f
            )
            drawLine(
                color = Color.Black,
                start = Offset(selectedX, 0f),
                end = Offset(selectedX, size.height),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
private fun SliderGroup(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
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
