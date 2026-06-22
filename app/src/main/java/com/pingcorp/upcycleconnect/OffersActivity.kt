package com.pingcorp.upcycleconnect

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.se.omapi.Session
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
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

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
                    val element = response.body()
                    val offers = if (element is JsonArray) {
                        RetrofitClient.json.decodeFromJsonElement<List<Annonce>>(element)
                    } else {
                        emptyList()
                    }
                    val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewOffers)
                    val adapter = OffersAdapter(offers) { offer ->
                        val intent = Intent(this@OffersActivity, CheckoutActivity::class.java)
                        intent.putExtra("PRODUCT_UUID", offer.id)
                        startActivity(intent)
                    }
                    recyclerView.adapter = adapter

                    val emptyState = findViewById<View>(R.id.emptyState)
                    if (offers.isEmpty()) {
                        emptyState.visibility = View.VISIBLE
                        findViewById<TextView>(R.id.emptyStateText).text = getString(R.string.empty_offers_message)
                        findViewById<Button>(R.id.emptyStateButton).apply {
                            text = getString(R.string.refresh_btn)
                            setOnClickListener { fetchOffers() }
                        }
                    } else {
                        emptyState.visibility = View.GONE
                    }
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

//    override fun getSelfNavDrawerItemId(): Int = R.id.nav_offers
}