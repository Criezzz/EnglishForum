package com.example.englishforum.data.profile

import android.net.Uri
import java.io.File

data class ProfileAvatarImage(
    val originalUri: Uri,
    val file: File,
    val mimeType: String,
    val displayName: String? = null
) {
    val sizeBytes: Long get() = file.length()
}
