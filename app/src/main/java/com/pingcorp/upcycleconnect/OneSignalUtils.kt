package com.pingcorp.upcycleconnect

import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object OneSignalUtils {
    fun registerPlayerId(userId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val playerId = OneSignal.User.pushSubscription.id
            if (!playerId.isNullOrEmpty()) {
                try {
                    RetrofitClient.api.updateUser(
                        userId,
                        UpdateUserDto(oneSignalPlayerId = playerId),
                        "Bearer $token"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
