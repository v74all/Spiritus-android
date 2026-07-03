package com.v7lthronyx.v7lpanel

import android.app.Application
import com.v7lthronyx.v7lpanel.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class V7LApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@V7LApplication)
            modules(appModule)
        }
    }
}
