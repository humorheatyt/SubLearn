package com.sublearn.app

import android.app.Application
import com.sublearn.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

class SubLearnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Guard: Robolectric and multi-process scenarios can create the application more
        // than once per VM; Koin's global context allows a single start.
        if (GlobalContext.getOrNull() != null) return
        startKoin {
            androidContext(this@SubLearnApplication)
            modules(appModule)
        }
    }
}
