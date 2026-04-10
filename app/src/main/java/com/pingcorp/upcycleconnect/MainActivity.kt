package com.pingcorp.upcycleconnect

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }

    private fun checkApiHealth() {
        val apiUrl = BuildConfig.API_URL
        if (apiUrl.isEmpty()) {
            Log.e("HealthCheck", "API_URL non configurée")
            return
        }

        val base = if (apiUrl.startsWith("http")) apiUrl else "http://$apiUrl"
        val fullUrl = if (base.endsWith("/")) base else "$base/"
        
        thread {
            try {
                val url = URL(fullUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                val responseCode = connection.responseCode
                Log.i("HealthCheck", "API Response Code: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.i("HealthCheck", "L'API répond correctement sur /")
                } else {
                    Log.w("HealthCheck", "L'API a répondu avec le code: $responseCode")
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("HealthCheck", "Erreur lors de la connexion à l'API ($fullUrl): ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val apiKey = BuildConfig.API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_API_KEY_HERE") {
            Log.e("HealthCheck", "ERREUR : Clé API non configurée dans local.properties")
        } else {
            Log.i("HealthCheck", "Configuration : OK")
        }

        if (isNetworkAvailable()) {
            Log.i("HealthCheck", "Connexion Internet : OK")
        } else {
            Log.w("HealthCheck", "Connexion Internet : AUCUNE")
            AlertDialog.Builder(this)
                .setTitle("Pas de connexion")
                .setMessage("L'application nécessite internet pour fonctionner correctement.")
                .setPositiveButton("OK", null)
                .show()
        }

        // Query API_URL on /
        checkApiHealth()

        // Get loginBtn item from layout
        val loginBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.loginBtn)

        // Click listeners
        loginBtn.setOnClickListener {
            // Should start an api request having the following request body :
            // {
        //  'username': 'username',
        //  'password': 'password'
        // }

        }

    }
}