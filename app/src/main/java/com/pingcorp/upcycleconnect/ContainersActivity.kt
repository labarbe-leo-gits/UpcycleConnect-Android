package com.pingcorp.upcycleconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class ContainersActivity : BaseActivity() {
    private lateinit var sessionManager: SessionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_containers)

        sessionManager = SessionManager(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupDrawer(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fetchContainers()
    }

    override fun onResume() {
        super.onResume()
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
    }

    private fun fetchContainers(){
        val token = sessionManager.getToken()
        if (token == null){
            Toast.makeText(this, "No session found, redirecting to login", Toast.LENGTH_SHORT).show()
            return
        }

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch{
            try{
                val response = RetrofitClient.api.getContainers("Bearer $token")
                progressBar.visibility = View.GONE

                if (response.isSuccessful){
                    val element = response.body()
                    val containers = when (element) {
                        is JsonArray -> RetrofitClient.json.decodeFromJsonElement<List<Container>>(element)
                        is JsonObject -> {
                            val items = element["items"]
                            if (items is JsonArray) {
                                RetrofitClient.json.decodeFromJsonElement<List<Container>>(items)
                            } else {
                                emptyList()
                            }
                        }
                        else -> emptyList()
                    }

                    val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewContainers)
                    val progressBar = findViewById<ProgressBar>(R.id.progressBar)
                    
                    recyclerView.adapter = ContainerAdapter(containers) { container ->
                        progressBar.visibility = View.VISIBLE
                        val intent = Intent(this@ContainersActivity, ContainerActivity::class.java)
                        intent.putExtra("CONTAINER_ID", container.id)
                        startActivity(intent)
                    }
                    recyclerView.layoutManager = LinearLayoutManager(this@ContainersActivity)

                    val emptyState = findViewById<View>(R.id.emptyState)
                    if (containers.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                        findViewById<TextView>(R.id.emptyStateText).text = getString(R.string.empty_containers_message)
                        findViewById<Button>(R.id.emptyStateButton).apply {
                            text = getString(R.string.refresh_btn)
                            setOnClickListener { fetchContainers() }
                        }
                    } else {
                        emptyState.visibility = View.GONE
                    }
                } else {
                    progressBar.visibility = View.GONE
                    val errorBody = response.errorBody()?.string()
                    Log.e("ContainersActivity", "Failed to fetch containers: ${response.code()} - $errorBody")
                    Toast.makeText(this@ContainersActivity, "Failed to fetch containers", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception){
                progressBar.visibility = View.GONE
                Log.e("ContainersActivity", "Exception fetching containers", e)
                Toast.makeText(this@ContainersActivity, "Error fetching containers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getSelfNavDrawerItemId(): Int = R.id.nav_containers
}