package com.example.englishforum.data.profile

import android.net.Uri

data class ProfileAvatarImage(
    val uri: Uri,
    val displayName: String? = null
)
