package com.pingcorp.upcycleconnect

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
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
    }
}