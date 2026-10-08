package com.sublearn.app

import android.app.Application
import com.sublearn.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SubLearnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SubLearnApplication)
            modules(appModule)
        }
    }
}
