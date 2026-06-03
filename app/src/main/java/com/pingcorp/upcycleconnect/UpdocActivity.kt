package com.pingcorp.upcycleconnect

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

class UpdocActivity : BaseActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var recyclerView: RecyclerView
    private val updocsList = mutableListOf<Project>()
    private lateinit var adapter: UpdocAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_updoc)

        sessionManager = SessionManager(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupDrawer(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.recyclerViewUpdocs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UpdocAdapter(updocsList) { project ->
            val intent = Intent(this, UpdocEditorActivity::class.java)
            intent.putExtra("PROJECT_ID", project.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddUpdoc).setOnClickListener {
            val intent = Intent(this, UpdocEditorActivity::class.java)
            startActivity(intent)
        }

        fetchUpdocs()
    }

    private fun fetchUpdocs(){
        val token = sessionManager.getToken()
        if (token == null){
            Toast.makeText(this, "No session found, redirecting to login", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch{
            try{
                Log.d("UpdocActivity", "Fetching updocs...")
                val response = RetrofitClient.api.getProjects("Bearer $token")

                if (response.isSuccessful){
                    val element = response.body()
                    Log.d("UpdocActivity", "Response body: $element")
                    val updocs = when (element) {
                        is JsonArray -> RetrofitClient.json.decodeFromJsonElement<List<Project>>(element)
                        is JsonObject -> {
                            val items = element["items"]
                            if (items is JsonArray) {
                                RetrofitClient.json.decodeFromJsonElement<List<Project>>(items)
                            } else {
                                Log.w("UpdocActivity", "Items is not a JsonArray: $items")
                                emptyList()
                            }
                        }
                        else -> {
                            Log.w("UpdocActivity", "Element is not JsonArray or JsonObject: $element")
                            emptyList()
                        }
                    }

                    Log.d("UpdocActivity", "Parsed ${updocs.size} updocs")
                    updocsList.clear()
                    updocsList.addAll(updocs)
                    adapter.notifyDataSetChanged()

                    val emptyState = findViewById<View>(R.id.emptyState)
                    if (updocsList.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                        findViewById<TextView>(R.id.emptyStateText).text = getString(R.string.empty_updoc_message)
                        findViewById<Button>(R.id.emptyStateButton).apply {
                            text = getString(R.string.create_updoc_btn)
                            setOnClickListener {
                                startActivity(Intent(this@UpdocActivity, UpdocEditorActivity::class.java))
                            }
                        }
                    } else {
                        emptyState.visibility = View.GONE
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("UpdocActivity", "Failed to fetch Updocs: ${response.code()} - $errorBody")
                    Toast.makeText(this@UpdocActivity, "Failed to fetch Updocs", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception){
                Log.e("UpdocActivity", "Exception fetching Updocs", e)
                Toast.makeText(this@UpdocActivity, "Error fetching Updocs: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getSelfNavDrawerItemId(): Int = R.id.nav_updoc
}