package com.pingcorp.upcycleconnect

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
import com.onesignal.user.subscriptions.IPushSubscriptionObserver
import com.onesignal.user.subscriptions.PushSubscriptionChangedState

class UpcycleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        OneSignal.initWithContext(this, BuildConfig.ONESIGNAL_APP_ID)

        val sessionManager = SessionManager(this)

        OneSignal.User.pushSubscription.addObserver(object : IPushSubscriptionObserver {
            override fun onPushSubscriptionChange(state: PushSubscriptionChangedState) {
                if (state.current.optedIn) {
                    val userId = sessionManager.getUserId()
                    val token = sessionManager.getToken()
                    if (userId != null && token != null) {
                        OneSignalUtils.registerPlayerId(userId, token)
                    }
                }
            }
        })

        val userId = sessionManager.getUserId()
        val token = sessionManager.getToken()

        if (userId != null && token != null && OneSignal.User.pushSubscription.optedIn) {
            OneSignalUtils.registerPlayerId(userId, token)
        }
    }
}
