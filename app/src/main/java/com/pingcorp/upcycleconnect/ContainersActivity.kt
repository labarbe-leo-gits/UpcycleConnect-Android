package com.pingcorp.upcycleconnect

import android.content.Context
import android.os.Bundle
import android.util.Log
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

    private fun fetchContainers(){
        val token = sessionManager.getToken()
        if (token == null){
            Toast.makeText(this, "No session found, redirecting to login", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch{
            try{
                val response = RetrofitClient.api.getContainers("Bearer $token")

                if (response.isSuccessful){
                    val element = response.body()
                    val containers = if (element is JsonArray) {
                        RetrofitClient.json.decodeFromJsonElement<List<Container>>(element)
                    } else {
                        emptyList()
                    }
//                    Log.d("ContainersActivity", "Fetched ${containers?.size} containers")
//                    Toast.makeText(this@ContainersActivity, "Fetched ${containers?.size} containers", Toast.LENGTH_SHORT).show()

                    val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewContainers)

                    recyclerView.adapter = ContainerAdapter(containers)

                    recyclerView.layoutManager = LinearLayoutManager(this@ContainersActivity)

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ContainersActivity", "Failed to fetch containers: ${response.code()} - $errorBody")
                    Toast.makeText(this@ContainersActivity, "Failed to fetch containers", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception){
                Log.e("ContainersActivity", "Exception fetching containers", e)
                Toast.makeText(this@ContainersActivity, "Error fetching containers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}