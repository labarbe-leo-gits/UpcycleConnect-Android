package com.pingcorp.upcycleconnect

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import org.json.JSONObject
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

    private fun performLogin(username: String, password: String) {
        val loginBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.loginBtn)
        val loginProgress = findViewById<ProgressBar>(R.id.loginProgress)

        val apiUrl = BuildConfig.API_URL
        if (apiUrl.isEmpty()) {
            Log.e("Login", "API_URL non configurée")
            return
        }

        // Show loading state
        loginBtn.isEnabled = false
        loginBtn.text = ""
        loginProgress.visibility = View.VISIBLE

        val base = if (apiUrl.startsWith("http")) apiUrl else "http://$apiUrl"
        val loginUrl = if (base.endsWith("/")) "${base}login" else "$base/login"

        thread {
            try {
                val url = URL(loginUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                
                val jsonInputString = "{\"identifier\": \"$username\", \"password\": \"$password\"}"
                Log.d("Login", "Sending to $loginUrl: $jsonInputString")
                
                connection.outputStream.use { os ->
                    val input = jsonInputString.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseText)
                    val token = jsonResponse.getString("token")
                    val userObj = jsonResponse.getJSONObject("user")
                    val userId = userObj.getString("id")
                    val userType = userObj.optInt("user_type", -1)

                    if (userType != 2) {
                        runOnUiThread {
                            loginBtn.isEnabled = true
                            loginBtn.text = getString(R.string.login)
                            loginProgress.visibility = View.GONE

                            AlertDialog.Builder(this@MainActivity)
                                .setTitle("Accès refusé")
                                .setMessage("Cette application est réservée aux professionels et artisans.")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                        return@thread
                    }

                    runOnUiThread {
                        // Reset loading state
                        loginBtn.isEnabled = true
                        loginBtn.text = getString(R.string.login)
                        loginProgress.visibility = View.GONE

                        val intent = Intent(this@MainActivity, ProfileActivity::class.java)
                        intent.putExtra("token", token)
                        intent.putExtra("userId", userId)
                        
                        intent.putExtra("username", userObj.optString("username"))
                        intent.putExtra("first_name", userObj.optString("first_name"))
                        intent.putExtra("last_name", userObj.optString("last_name"))
                        intent.putExtra("balance", userObj.optDouble("balance", 0.0))

                        startActivity(intent)
                    }

                    Log.e("Login", "SUCCESS: Login succeeded for user $username")
                } else {
                    val errorDetail = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error detail"
                    Log.w("Login", "FAILED: Response code $responseCode. Detail: $errorDetail")
                    
                    runOnUiThread {
                        loginBtn.isEnabled = true
                        loginBtn.text = getString(R.string.login)
                        loginProgress.visibility = View.GONE
                        
                        AlertDialog.Builder(this@MainActivity)
                            .setTitle("Erreur de connexion")
                            .setMessage(errorDetail)
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                Log.e("Login", "Error during login: ${e.message}")
                runOnUiThread {
                    loginBtn.isEnabled = true
                    loginBtn.text = getString(R.string.login)
                    loginProgress.visibility = View.GONE
                    
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("Erreur")
                        .setMessage(e.message)
                        .setPositiveButton("OK", null)
                        .show()
                }
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

        // Login setup
        val usernameInput = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.usernameInput)
        val passwordInput = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.passwordInput)
        val loginBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.loginBtn)

        loginBtn.setOnClickListener {
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            
            if (username.isNotEmpty() && password.isNotEmpty()) {
                performLogin(username, password)
            } else {
                Log.w("Login", "Veuillez remplir tous les champs")
            }
        }
    }
}