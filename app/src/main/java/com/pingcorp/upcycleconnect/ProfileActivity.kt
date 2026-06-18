package com.pingcorp.upcycleconnect

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class ProfileActivity : BaseActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var etUsername: EditText
    private lateinit var btnEditUsername: ImageButton
    private lateinit var btnSaveUsername: ImageButton
    private lateinit var btnCancelUsername: ImageButton

    private lateinit var tvEmail: TextView
    private lateinit var etEmail: EditText
    private lateinit var btnEditEmail: ImageButton
    private lateinit var btnSaveEmail: ImageButton
    private lateinit var btnCancelEmail: ImageButton

    private lateinit var tvFirstName: TextView
    private lateinit var etFirstName: EditText
    private lateinit var btnEditFirstName: ImageButton
    private lateinit var btnSaveFirstName: ImageButton
    private lateinit var btnCancelFirstName: ImageButton

    private lateinit var tvLastName: TextView
    private lateinit var etLastName: EditText
    private lateinit var btnEditLastName: ImageButton
    private lateinit var btnSaveLastName: ImageButton
    private lateinit var btnCancelLastName: ImageButton

    private lateinit var tvCompanyName: TextView
    private lateinit var tvBalance: TextView
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        
        sessionManager = SessionManager(this)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupDrawer(toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvUsername = findViewById(R.id.tvUsername)
        etUsername = findViewById(R.id.etUsername)
        btnEditUsername = findViewById(R.id.btnEditUsername)
        btnSaveUsername = findViewById(R.id.btnSaveUsername)
        btnCancelUsername = findViewById(R.id.btnCancelUsername)

        tvEmail = findViewById(R.id.tvEmail)
        etEmail = findViewById(R.id.etEmail)
        btnEditEmail = findViewById(R.id.btnEditEmail)
        btnSaveEmail = findViewById(R.id.btnSaveEmail)
        btnCancelEmail = findViewById(R.id.btnCancelEmail)

        tvFirstName = findViewById(R.id.tvFirstName)
        etFirstName = findViewById(R.id.etFirstName)
        btnEditFirstName = findViewById(R.id.btnEditFirstName)
        btnSaveFirstName = findViewById(R.id.btnSaveFirstName)
        btnCancelFirstName = findViewById(R.id.btnCancelFirstName)

        tvLastName = findViewById(R.id.tvLastName)
        etLastName = findViewById(R.id.etLastName)
        btnEditLastName = findViewById(R.id.btnEditLastName)
        btnSaveLastName = findViewById(R.id.btnSaveLastName)
        btnCancelLastName = findViewById(R.id.btnCancelLastName)

        tvCompanyName = findViewById(R.id.tvCompanyName)
        tvBalance = findViewById(R.id.tvBalance)

        setupEditListeners()
        displaySessionData()

        val token = sessionManager.getToken()
        val userId = sessionManager.getUserId()

        if (token != null && userId != null) {
            fetchUserProfile(token, userId)
        } else {
            Log.e("Profile", "No session found, redirecting to login")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val logoutBtn = findViewById<com.google.android.material.button.MaterialButton>(R.id.logoutBtn)
        val logoutProgress = findViewById<android.widget.ProgressBar>(R.id.logoutProgress)
        val btnDeleteAccount = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeleteAccount)

        logoutBtn.setOnClickListener {
            logoutBtn.text = ""
            logoutBtn.isEnabled = false
            logoutProgress.visibility = android.view.View.VISIBLE

            logoutBtn.postDelayed({
                sessionManager.clearSession()
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }, 600)
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showDeleteAccountDialog() {
        val currentUsername = sessionManager.getUsername() ?: ""
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(R.string.delete_account_confirm_title)
        
        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(48, 24, 48, 24)
        
        val inputUsername = EditText(this)
        inputUsername.hint = getString(R.string.enter_username_confirm)
        container.addView(inputUsername, params)
        
        builder.setView(container)
        builder.setMessage(getString(R.string.delete_account_confirm_msg, currentUsername))
        
        builder.setPositiveButton(R.string.delete_confirm_btn) { _, _ ->
            val enteredUsername = inputUsername.text.toString()
            if (enteredUsername == currentUsername) {
                showSecurityPrompt(enteredUsername)
            } else {
                Toast.makeText(this, R.string.wrong_username_confirm, Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton(R.string.cancel_btn) { dialog, _ -> dialog.cancel() }
        
        builder.show()
    }

    private fun showSecurityPrompt(username: String, forceMfa: Boolean = false) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Security Verification")
        
        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        val params = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(48, 24, 48, 16)

        val inputPassword = EditText(this)
        inputPassword.hint = "Enter your password"
        inputPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        container.addView(inputPassword, params)

        var inputOtp: EditText? = null
        if (forceMfa || sessionManager.isMfaEnabled()) {
            inputOtp = EditText(this)
            inputOtp.hint = "MFA Code (6 digits)"
            inputOtp.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            val otpParams = android.widget.LinearLayout.LayoutParams(params)
            otpParams.setMargins(48, 16, 48, 24)
            container.addView(inputOtp, otpParams)
        }
        
        builder.setView(container)

        builder.setPositiveButton(R.string.delete_confirm_btn) { _, _ ->
            val password = inputPassword.text.toString()
            val otp = inputOtp?.text?.toString()
            if (password.isNotEmpty()) {
                val finalOtp = if (otp.isNullOrEmpty()) null else otp
                deleteAccount(username, password, finalOtp)
            } else {
                Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton(R.string.cancel_btn) { dialog, _ -> dialog.cancel() }
        builder.show()
    }

    private fun deleteAccount(username: String, password: String?, otp: String?) {
        val token = sessionManager.getToken() ?: return
        val userId = sessionManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.deleteUser(userId, DeleteUserRequest(username, password, otp), "Bearer $token")
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfileActivity, R.string.account_deleted, Toast.LENGTH_SHORT).show()
                    sessionManager.clearSession()
                    val intent = Intent(this@ProfileActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("Profile", "Failed to delete account: $errorBody")
                    
                    if (errorBody?.contains("MFA code is required") == true) {
                        runOnUiThread {
                            Toast.makeText(this@ProfileActivity, "MFA Code required", Toast.LENGTH_SHORT).show()
                            showSecurityPrompt(username, forceMfa = true)
                        }
                    } else {
                        val message = if (errorBody?.contains("Password") == true) "Incorrect password" else "Failed to delete account. Check your credentials."
                        Toast.makeText(this@ProfileActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Profile", "Error deleting account", e)
                Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupEditListeners() {
        btnEditUsername.setOnClickListener { toggleEditMode(true, tvUsername, etUsername, btnEditUsername, btnSaveUsername, btnCancelUsername) }
        btnCancelUsername.setOnClickListener { toggleEditMode(false, tvUsername, etUsername, btnEditUsername, btnSaveUsername, btnCancelUsername) }
        btnSaveUsername.setOnClickListener {
            updateUserInfo(UpdateUserDto(username = etUsername.text.toString()))
            toggleEditMode(false, tvUsername, etUsername, btnEditUsername, btnSaveUsername, btnCancelUsername)
        }

        btnEditEmail.setOnClickListener { toggleEditMode(true, tvEmail, etEmail, btnEditEmail, btnSaveEmail, btnCancelEmail) }
        btnCancelEmail.setOnClickListener { toggleEditMode(false, tvEmail, etEmail, btnEditEmail, btnSaveEmail, btnCancelEmail) }
        btnSaveEmail.setOnClickListener {
            updateUserInfo(UpdateUserDto(email = etEmail.text.toString()))
            toggleEditMode(false, tvEmail, etEmail, btnEditEmail, btnSaveEmail, btnCancelEmail)
        }

        btnEditFirstName.setOnClickListener { toggleEditMode(true, tvFirstName, etFirstName, btnEditFirstName, btnSaveFirstName, btnCancelFirstName) }
        btnCancelFirstName.setOnClickListener { toggleEditMode(false, tvFirstName, etFirstName, btnEditFirstName, btnSaveFirstName, btnCancelFirstName) }
        btnSaveFirstName.setOnClickListener {
            updateUserInfo(UpdateUserDto(firstName = etFirstName.text.toString()))
            toggleEditMode(false, tvFirstName, etFirstName, btnEditFirstName, btnSaveFirstName, btnCancelFirstName)
        }

        btnEditLastName.setOnClickListener { toggleEditMode(true, tvLastName, etLastName, btnEditLastName, btnSaveLastName, btnCancelLastName) }
        btnCancelLastName.setOnClickListener { toggleEditMode(false, tvLastName, etLastName, btnEditLastName, btnSaveLastName, btnCancelLastName) }
        btnSaveLastName.setOnClickListener {
            updateUserInfo(UpdateUserDto(lastName = etLastName.text.toString()))
            toggleEditMode(false, tvLastName, etLastName, btnEditLastName, btnSaveLastName, btnCancelLastName)
        }
    }

    private fun toggleEditMode(isEditing: Boolean, tv: TextView, et: EditText, btnEdit: ImageButton, btnSave: ImageButton, btnCancel: ImageButton) {
        if (isEditing) {
            et.setText(tv.text)
            tv.visibility = View.GONE
            btnEdit.visibility = View.GONE
            et.visibility = View.VISIBLE
            btnSave.visibility = View.VISIBLE
            btnCancel.visibility = View.VISIBLE
            et.requestFocus()
        } else {
            tv.visibility = View.VISIBLE
            btnEdit.visibility = View.VISIBLE
            et.visibility = View.GONE
            btnSave.visibility = View.GONE
            btnCancel.visibility = View.GONE
        }
    }

    private fun updateUserInfo(updateDto: UpdateUserDto) {
        val token = sessionManager.getToken() ?: return
        val userId = sessionManager.getUserId() ?: return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.api.updateUser(userId, updateDto, "Bearer $token")
                if (response.isSuccessful) {
                    // Refresh data from server to ensure session and UI are in sync
                    fetchUserProfile(token, userId)
                    runOnUiThread {
                        Toast.makeText(this@ProfileActivity, "Profile updated", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ProfileActivity, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("Profile", "Error updating profile", e)
                Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displaySessionData() {
        tvUsername.text = sessionManager.getUsername() ?: getString(R.string.loading)
        tvEmail.text = sessionManager.getEmail() ?: getString(R.string.loading)
        tvFirstName.text = sessionManager.getFirstName() ?: getString(R.string.loading)
        tvLastName.text = sessionManager.getLastName() ?: getString(R.string.loading)
        tvCompanyName.text = sessionManager.getCompanyName() ?: "N/A"
        val balance = sessionManager.getBalance()
        tvBalance.text = String.format(Locale.getDefault(), "%.2f €", balance)
    }

    private fun fetchUserProfile(token: String, userId: String) {
        Companion.fetchUserProfile(token, userId) { json ->
            if (json != null) {
                val username = json.optString("username", "N/A")
                val firstName = json.optString("first_name", "N/A")
                val lastName = json.optString("last_name", "N/A")
                val email = json.optString("email", "N/A")
                val companyName = json.optString("company_name", "N/A")
                val balance = json.optDouble("balance", 0.0)
                val mfaEnabled = json.optBoolean("mfa_enabled", false)

                sessionManager.saveSession(token, userId, username, firstName, lastName, email, companyName, balance, mfaEnabled)

                runOnUiThread {
                    displaySessionData()
                }
            }
        }
    }

    override fun getSelfNavDrawerItemId(): Int = R.id.nav_profile

    companion object {
        fun fetchUserProfile(token: String, userId: String, callback: (JSONObject?) -> Unit) {
            val apiUrl = BuildConfig.API_URL
            val base = if (apiUrl.startsWith("http")) apiUrl else "http://$apiUrl"
            val userUrl = if (base.endsWith("/")) "${base}users/$userId" else "$base/users/$userId"

            thread {
                try {
                    val url = URL(userUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Accept", "application/json")

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                        callback(JSONObject(responseText))
                    } else {
                        Log.e("Profile", "Failed to fetch profile: ${connection.responseCode}")
                        callback(null)
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    Log.e("Profile", "Error fetching profile: ${e.message}")
                    callback(null)
                }
            }
        }
    }
}