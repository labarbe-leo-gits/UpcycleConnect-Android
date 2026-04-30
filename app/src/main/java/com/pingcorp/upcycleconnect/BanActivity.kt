package com.pingcorp.upcycleconnect

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.decodeFromJsonElement

class BanActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ban)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val reason = intent.getStringExtra("BAN_REASON") ?: "Aucune raison spécifiée"
        val bannedById = intent.getStringExtra("BANNED_BY")
        val token = intent.getStringExtra("TOKEN")

        val tvBanReason = findViewById<TextView>(R.id.tvBanReason)
        val tvBannedBy = findViewById<TextView>(R.id.tvBannedBy)
        val btnBackToLogin = findViewById<MaterialButton>(R.id.btnBackToLogin)

        tvBanReason.text = reason

        if (bannedById != null && token != null) {
            fetchAdminDetails(token, bannedById, tvBannedBy)
        } else {
            tvBannedBy.text = "Administrateur inconnu"
        }

        btnBackToLogin.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun fetchAdminDetails(token: String, adminId: String, textView: TextView) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.api.getUserProfile(adminId, "Bearer $token")
                val element = response.body()
                if (response.isSuccessful && element != null && element !is kotlinx.serialization.json.JsonNull) {
                    val admin = RetrofitClient.json.decodeFromJsonElement<UserData>(element)
                    val fullName = "${admin.first_name ?: ""} ${admin.last_name ?: ""}".trim()
                    withContext(Dispatchers.Main) {
                        textView.text = if (fullName.isNotEmpty()) fullName else admin.username ?: admin.email
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        textView.text = "Erreur lors de la récupération de l'administrateur"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textView.text = "Erreur de connexion"
                }
            }
        }
    }
}
