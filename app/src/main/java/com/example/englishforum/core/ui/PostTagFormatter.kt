package com.example.englishforum.core.ui

import androidx.annotation.StringRes
import com.example.englishforum.R
import com.example.englishforum.core.model.forum.PostTag

@StringRes
fun PostTag.toLabelResId(): Int {
    return when (this) {
        PostTag.Tutorial -> R.string.post_tag_tutorial
        PostTag.AskQuestion -> R.string.post_tag_question
        PostTag.Resource -> R.string.post_tag_resource
        PostTag.Experience -> R.string.post_tag_experience
    }
}
