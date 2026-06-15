package com.pingcorp.upcycleconnect

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class UpcycleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
