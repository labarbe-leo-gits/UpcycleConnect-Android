package com.pingcorp.upcycleconnect

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.onesignal.OneSignal
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationSetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (OneSignal.Notifications.permission) {
            proceedToNextActivity()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_notification_setup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.grantPermissionBtn).setOnClickListener {
            requestNotificationPermission()
        }
    }

    private fun requestNotificationPermission() {
        CoroutineScope(Dispatchers.Main).launch {
            val success = OneSignal.Notifications.requestPermission(fallbackToSettings = true)
            if (success) {
                proceedToNextActivity()
            }
        }
    }

    private fun proceedToNextActivity() {
        val sessionManager = SessionManager(this)
        val intent = if (sessionManager.isLoggedIn()) {
            Intent(this, ProfileActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        if (OneSignal.Notifications.permission) {
            proceedToNextActivity()
        }
    }
}
