package com.example.englishforum.core.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoteIconButton(
    icon: ImageVector,
    contentDescription: String?,
    selected: Boolean,
    onClick: () -> Unit,
    buttonSize: Dp = 36.dp,
    modifier: Modifier = Modifier,
    enforceMinimumTouchTarget: Boolean = true,
    selectedColorOverride: Color? = null
) {
    val selectedColor = selectedColorOverride ?: MaterialTheme.colorScheme.primary
    val defaultTint = LocalContentColor.current

    val buttonColors = IconButtonDefaults.iconButtonColors(
        containerColor = Color.Transparent,
        contentColor = if (selected) selectedColor else defaultTint
    )

    val iconButton: @Composable () -> Unit = {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(buttonSize),
            colors = buttonColors
        ) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }

    if (enforceMinimumTouchTarget) {
        iconButton()
    } else {
        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            iconButton()
        }
    }
}
