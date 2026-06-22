package com.pingcorp.upcycleconnect

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.pingcorp.upcycleconnect.RetrofitClient
import com.pingcorp.upcycleconnect.Verify2FARequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

class MfaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mfa)

        val tempToken = intent.getStringExtra("TEMP_TOKEN") ?: ""
        val otpInput = findViewById<EditText>(R.id.otpInput)
        val verifyButton = findViewById<Button>(R.id.verifyButton)

        verifyButton.setOnClickListener {
            val code = otpInput.text.toString()
            if (code.length == 6){
                verifyCode(tempToken, code)
            }
        }
    }

    private fun verifyCode(tempToken: String, code: String){
        CoroutineScope(Dispatchers.IO).launch{
            try{
                val response = RetrofitClient.authApi.verify2FA(Verify2FARequest(tempToken, code))
                if (response.isSuccessful && response.body() != null){
                    val body = response.body()!!

                    val banResponse = RetrofitClient.api.getUserBan(body.user.id, "Bearer ${body.token}")
                    val element = banResponse.body()
                    
                    if (banResponse.isSuccessful && element is JsonArray && element.isNotEmpty()) {
                        val bans = RetrofitClient.json.decodeFromJsonElement<List<Ban>>(element)
                        val ban = bans[0]
                        withContext(Dispatchers.Main) {
                            val intent = Intent(this@MfaActivity, BanActivity::class.java)
                            intent.putExtra("BAN_REASON", ban.reason)
                            intent.putExtra("BANNED_BY", ban.bannedBy)
                            intent.putExtra("TOKEN", body.token)
                            startActivity(intent)
                            finish()
                        }
                        return@launch
                    }

                    val sessionManager = SessionManager(this@MfaActivity)
                    sessionManager.saveSession(
                        body.token,
                        body.user.id,
                        body.user.username,
                        body.user.first_name,
                        body.user.last_name,
                        body.user.email,
                        body.user.company_name,
                        body.user.balance,
                        body.user.mfa_enabled
                    )

                    OneSignalUtils.registerPlayerId(body.user.id, body.token)

                    withContext(Dispatchers.Main){
                        startActivity(Intent(this@MfaActivity, ProfileActivity::class.java))
                        finish()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("MFA", "Verification failed: ${response.code()} - $errorBody")
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@MfaActivity, R.string.invalid_code_error, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception){
                android.util.Log.e("MFA", "Connection error", e)
                withContext(Dispatchers.Main){
                    Toast.makeText(this@MfaActivity, getString(R.string.connection_error_prefix, e.message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}