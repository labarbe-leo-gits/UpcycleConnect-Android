package com.pingcorp.upcycleconnect

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement
import java.io.IOException
import java.util.Locale

class ContainerActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var webViewMap: WebView
    private lateinit var recyclerViewItems: RecyclerView
    private lateinit var adapter: ConteneurItemAdapter
    private lateinit var progressBarItems: ProgressBar
    private lateinit var btnLoadMore: Button
    
    private var allItems = mutableListOf<ConteneurItem>()
    private var displayedItems = mutableListOf<ConteneurItem>()
    private var currentPage = 0
    private val pageSize = 5

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

        recyclerViewItems = findViewById(R.id.recyclerViewItems)
        recyclerViewItems.layoutManager = LinearLayoutManager(this)
        adapter = ConteneurItemAdapter(displayedItems)
        recyclerViewItems.adapter = adapter

        progressBarItems = findViewById(R.id.progressBarItems)
        btnLoadMore = findViewById(R.id.btnLoadMore)

        btnLoadMore.setOnClickListener {
            loadNextPage()
        }

        val containerId = intent.getStringExtra("CONTAINER_ID")
        if (containerId != null) {
            fetchContainerDetails(containerId)
            fetchContainerItems(containerId)
        } else {
            Toast.makeText(this, "No container ID found", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchContainerDetails(id: String) {
        val token = sessionManager.getToken() ?: return
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
                }
            } catch (e: Exception) {
                Log.e("ContainerActivity", "Error fetching container", e)
            }
        }
    }

    private fun fetchContainerItems(id: String) {
        val token = sessionManager.getToken() ?: return
        progressBarItems.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.getContainerItems(id, "Bearer $token")
                progressBarItems.visibility = View.GONE
                
                if (response.isSuccessful) {
                    val element = response.body()
                    val items = if (element is JsonArray) {
                        RetrofitClient.json.decodeFromJsonElement<List<ConteneurItem>>(element)
                    } else emptyList()
                    
                    allItems.clear()
                    allItems.addAll(items)
                    currentPage = 0
                    displayedItems.clear()
                    loadNextPage()
                }
            } catch (e: Exception) {
                progressBarItems.visibility = View.GONE
                Log.e("ContainerActivity", "Error fetching items", e)
            }
        }
    }

    private fun loadNextPage() {
        val start = currentPage * pageSize
        if (start >= allItems.size) {
            btnLoadMore.visibility = View.GONE
            return
        }

        val end = minOf(start + pageSize, allItems.size)
        val newItems = allItems.subList(start, end)
        
        val prevSize = displayedItems.size
        displayedItems.addAll(newItems)
        adapter.notifyItemRangeInserted(prevSize, newItems.size)
        
        currentPage++
        
        if (displayedItems.size >= allItems.size) {
            btnLoadMore.visibility = View.GONE
        } else {
            btnLoadMore.visibility = View.VISIBLE
        }
    }

    private fun displayContainer(container: Container) {
        findViewById<TextView>(R.id.textViewContainerName).text = container.name
        val address = "${container.number} ${container.road}, ${container.postalCode} ${container.city}"
        findViewById<TextView>(R.id.textViewAddress).text = address
        findViewById<TextView>(R.id.textViewCapacity).text = getString(R.string.capacity_format, container.capacity)
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
