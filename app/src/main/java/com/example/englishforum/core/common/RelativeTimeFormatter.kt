package com.example.englishforum.core.common

fun formatRelativeTime(minutesAgo: Int): String {
    return when {
        minutesAgo < 1 -> "Vừa xong"
        minutesAgo < 60 -> "$minutesAgo phút trước"
        minutesAgo < 1_440 -> {
            val hours = minutesAgo / 60
            "$hours giờ trước"
        }
        else -> {
            val days = minutesAgo / 1_440
            "$days ngày trước"
        }
    }
}
