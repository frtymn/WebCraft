package com.webcraft.ide

import android.app.Application

class WebCraftApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: WebCraftApp
            private set
    }
}
