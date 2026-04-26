package com.pingcorp.upcycleconnect

import android.content.Context
import android.os.Bundle
import android.se.omapi.Session
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

class OffersActivity : BaseActivity() {
    private lateinit var sessionManager: SessionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_offers)

        sessionManager = SessionManager(this)
        
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupDrawer(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fetchOffers()
    }

    private fun fetchOffers(){
        val token = sessionManager.getToken()
        if (token == null){
            Toast.makeText(this, "No session found, redirecting to login", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch{
            try{
                val response = RetrofitClient.api.getAnnonces("Bearer $token")

                if (response.isSuccessful){
                    val offers = response.body() ?: emptyList()
                    val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewOffers)
                    recyclerView.adapter = OffersAdapter(offers)
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("OffersActivity", "Failed to fetch offers: ${response.code()} - $errorBody")
                    Toast.makeText(this@OffersActivity, "Failed to fetch offers", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception){
                Log.e("OffersActivity", "Exception fetching offers", e)
                Toast.makeText(this@OffersActivity, "Error fetching offers: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getSelfNavDrawerItemId(): Int = R.id.nav_offers
}