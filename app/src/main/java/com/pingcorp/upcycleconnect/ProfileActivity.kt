package com.pingcorp.upcycleconnect

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class ProfileActivity : BaseActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvBalance: TextView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        sessionManager = SessionManager(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupDrawer(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvUsername = findViewById(R.id.tvUsername)
        tvFirstName = findViewById(R.id.tvFirstName)
        tvLastName = findViewById(R.id.tvLastName)
        tvBalance = findViewById(R.id.tvBalance)


        displaySessionData()

        val token = sessionManager.getToken()
        val userId = sessionManager.getUserId()

        if (token != null && userId != null) {
            fetchUserProfile(token, userId)
        } else {
            Log.e("Profile", "No session found, redirecting to login")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val logoutBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.logoutBtn)
        val logoutProgress = findViewById<android.widget.ProgressBar>(R.id.logoutProgress)

        logoutBtn.setOnClickListener {
            logoutBtn.text = ""
            logoutBtn.isEnabled = false
            logoutProgress.visibility = android.view.View.VISIBLE

            logoutBtn.postDelayed({
                sessionManager.clearSession()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }, 600)
        }
    }

    private fun displaySessionData() {
        tvUsername.text = sessionManager.getUsername() ?: getString(R.string.loading)
        tvFirstName.text = sessionManager.getFirstName() ?: getString(R.string.loading)
        tvLastName.text = sessionManager.getLastName() ?: getString(R.string.loading)
        val balance = sessionManager.getBalance()
        tvBalance.text = String.format(Locale.getDefault(), "%.2f €", balance)
    }

    private fun fetchUserProfile(token: String, userId: String) {
        Companion.fetchUserProfile(token, userId) { json ->
            if (json != null) {
                val username = json.optString("username", "N/A")
                val firstName = json.optString("first_name", "N/A")
                val lastName = json.optString("last_name", "N/A")
                val balance = json.optDouble("balance", 0.0)

                sessionManager.saveSession(token, userId, username, firstName, lastName, balance)

                runOnUiThread {
                    tvUsername.text = username
                    tvFirstName.text = firstName
                    tvLastName.text = lastName
                    tvBalance.text = String.format(Locale.getDefault(), "%.2f €", balance)
                }
            }
        }
    }

    override fun getSelfNavDrawerItemId(): Int = R.id.nav_profile

    companion object {
        fun fetchUserProfile(token: String, userId: String, callback: (JSONObject?) -> Unit) {
            val apiUrl = BuildConfig.API_URL
            val base = if (apiUrl.startsWith("http")) apiUrl else "http://$apiUrl"
            val userUrl = if (base.endsWith("/")) "${base}users/$userId" else "$base/users/$userId"

            thread {
                try {
                    val url = URL(userUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Accept", "application/json")

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                        callback(JSONObject(responseText))
                    } else {
                        Log.e("Profile", "Failed to fetch profile: ${connection.responseCode}")
                        callback(null)
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e("Profile", "Error fetching profile: ${e.message}")
                    callback(null)
                }
            }
        }
    }
}