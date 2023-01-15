package com.lyr.hotfix

import android.app.Application
import kotlinx.coroutines.*

class HotfixApp : Application() {

    override fun onCreate() {
        super.onCreate()
        loadDex()
    }

    private fun loadDex() = GlobalScope.launch {
        HotFix.patch(applicationContext, "", "")
    }
}