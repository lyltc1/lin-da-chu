package com.lyltc1.lindachu

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.lyltc1.lindachu.di.AppContainer

class LinDaChuApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SingletonImageLoader.setSafe {
            ImageLoader.Builder(this).build()
        }
    }
}
