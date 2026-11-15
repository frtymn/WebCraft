package com.webcraft.ide

import android.app.Application
import com.webcraft.ide.ui.ServiceLocator

class WebCraftApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        // CRITICAL: initialize the service locator before any ViewModel
        // (including the ones restored after process death) is created.
        // ViewModels read ServiceLocator.repository in their property
        // initializers; without this call the app crashes at startup with
        // UninitializedPropertyAccessException.
        ServiceLocator.init(this)
    }

    companion object {
        lateinit var instance: WebCraftApp
            private set
    }
}
