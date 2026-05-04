package com.pingcorp.upcycleconnect

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.IOException
import java.util.Locale

class ContainerActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var webViewMap: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_container)
        
        sessionManager = SessionManager(this)
        
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        webViewMap = findViewById(R.id.webViewMap)
        webViewMap.settings.javaScriptEnabled = true
        webViewMap.settings.userAgentString = "UpcycleConnect/1.0 (Android; Mobile)"
        webViewMap.webViewClient = WebViewClient()

        val containerId = intent.getStringExtra("CONTAINER_ID")
        if (containerId != null) {
            fetchContainerDetails(containerId)
        } else {
            Toast.makeText(this, "No container ID found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchContainerDetails(id: String) {
        val token = sessionManager.getToken()
        if (token == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getContainer(id, "Bearer $token")
                if (response.isSuccessful) {
                    val element = response.body()
                    if (element != null) {
                        val container = RetrofitClient.json.decodeFromJsonElement<Container>(element)
                        displayContainer(container)
                        updateMap(container)
                    }
                } else {
                    Toast.makeText(this@ContainerActivity, "Failed to fetch details", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ContainerActivity", "Error fetching container", e)
            }
        }
    }

    private fun displayContainer(container: Container) {
        findViewById<TextView>(R.id.textViewContainerName).text = container.name
        val address = "${container.number} ${container.road}, ${container.postalCode} ${container.city}"
        findViewById<TextView>(R.id.textViewAddress).text = address
        
        title = container.name
    }

    private fun updateMap(container: Container) {
        val address = "${container.number} ${container.road}, ${container.postalCode} ${container.city}"
        
        lifecycleScope.launch(Dispatchers.IO) {
            val geocoder = Geocoder(this@ContainerActivity, Locale.getDefault())
            try {
                val addresses: List<Address>? = geocoder.getFromLocationName(address, 1)
                if (!addresses.isNullOrEmpty()) {
                    val location = addresses[0]
                    withContext(Dispatchers.Main) {
                        loadLeafletMap(location.latitude, location.longitude, container.name)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Log.w("ContainerActivity", "Could not find location for address: $address")
                        // Load a default location or show message
                        loadLeafletMap(48.8566, 2.3522, "Paris (Location not found)") 
                    }
                }
            } catch (e: IOException) {
                Log.e("ContainerActivity", "Geocoding error", e)
            }
        }
    }

    private fun loadLeafletMap(lat: Double, lon: Double, name: String) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    #map { height: 100vh; width: 100vw; margin: 0; padding: 0; }
                    body { margin: 0; padding: 0; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map = L.map('map', {
                        zoomControl: true,
                        attributionControl: true
                    }).setView([$lat, $lon], 15);
                    
                    L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
                        subdomains: 'abcd',
                        maxZoom: 20
                    }).addTo(map);

                    L.marker([$lat, $lon]).addTo(map)
                        .bindPopup('$name')
                        .openPopup();
                </script>
            </body>
            </html>
        """.trimIndent()
        
        webViewMap.loadDataWithBaseURL("https://carto.com", html, "text/html", "UTF-8", null)
    }
}