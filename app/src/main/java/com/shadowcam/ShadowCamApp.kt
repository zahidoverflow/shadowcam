package com.shadowcam

import android.app.Application

class ShadowCamApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDependenciesProvider.installDefault()
    }
}
