package com.example.englishforum

import android.app.Application
import com.example.englishforum.core.di.AppContainer
import com.example.englishforum.core.di.DefaultAppContainer

class EnglishForumApplication : Application() {
    val container: AppContainer by lazy { DefaultAppContainer(this) }
}
