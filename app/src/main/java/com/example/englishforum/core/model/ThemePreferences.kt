package com.example.englishforum.core.model

const val DEFAULT_SEED_COLOR: Long = 0xFF66BB6AL // Green

data class ThemePreferences(
    val themeOption: ThemeOption = ThemeOption.FOLLOW_SYSTEM,
    val isMaterialThemeEnabled: Boolean = false,
    val isAmoledEnabled: Boolean = false,
    val seedColor: Long = DEFAULT_SEED_COLOR
)
