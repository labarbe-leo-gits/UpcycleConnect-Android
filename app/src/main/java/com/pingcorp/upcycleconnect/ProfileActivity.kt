package com.pingcorp.upcycleconnect

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvFirstName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvBalance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvUsername = findViewById(R.id.tvUsername)
        tvFirstName = findViewById(R.id.tvFirstName)
        tvLastName = findViewById(R.id.tvLastName)
        tvBalance = findViewById(R.id.tvBalance)

        val token = intent.getStringExtra("token")
        val userId = intent.getStringExtra("userId")

        // On peut afficher les données déjà présentes dans l'intent en attendant l'appel API
        tvUsername.text = intent.getStringExtra("username") ?: getString(R.string.loading)
        tvFirstName.text = intent.getStringExtra("first_name") ?: getString(R.string.loading)
        tvLastName.text = intent.getStringExtra("last_name") ?: getString(R.string.loading)
        val initialBalance = intent.getDoubleExtra("balance", -1.0)
        if (initialBalance >= 0) {
            tvBalance.text = String.format(Locale.getDefault(), "%.2f €", initialBalance)
        }

        if (token != null && userId != null) {
            fetchUserProfile(token, userId)
        } else {
            Log.e("Profile", "Missing token or userId")
            finish()
        }
    }

    private fun fetchUserProfile(token: String, userId: String) {
        // Appelle la méthode publique du companion object
        Companion.fetchUserProfile(token, userId) { json ->
            if (json != null) {
                val username = json.optString("username", "N/A")
                val firstName = json.optString("first_name", "N/A")
                val lastName = json.optString("last_name", "N/A")
                val balance = json.optDouble("balance", 0.0)

                runOnUiThread {
                    tvUsername.text = username
                    tvFirstName.text = firstName
                    tvLastName.text = lastName
                    tvBalance.text = String.format(Locale.getDefault(), "%.2f €", balance)
                }
            }
        }
    }

    companion object {
        /**
         * Récupère le profil utilisateur depuis l'API.
         * Cette fonction peut être appelée depuis n'importe quelle activité.
         */
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