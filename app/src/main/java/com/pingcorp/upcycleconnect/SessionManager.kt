package com.pingcorp.upcycleconnect

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FIRST_NAME = "first_name"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_COMPANY_NAME = "company_name"
        private const val KEY_BALANCE = "balance"
        private const val KEY_MFA_ENABLED = "mfa_enabled"
    }

    fun saveSession(token: String, userId: String, username: String? = null, firstName: String? = null, lastName: String? = null, email: String? = null, companyName: String? = null, balance: Double = 0.0, mfaEnabled: Boolean = false) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_FIRST_NAME, firstName)
            putString(KEY_LAST_NAME, lastName)
            putString(KEY_EMAIL, email)
            putString(KEY_COMPANY_NAME, companyName)
            putFloat(KEY_BALANCE, balance.toFloat())
            putBoolean(KEY_MFA_ENABLED, mfaEnabled)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)
    fun getFirstName(): String? = prefs.getString(KEY_FIRST_NAME, null)
    fun getLastName(): String? = prefs.getString(KEY_LAST_NAME, null)
    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun getCompanyName(): String? = prefs.getString(KEY_COMPANY_NAME, null)
    fun getBalance(): Double = prefs.getFloat(KEY_BALANCE, 0.0f).toDouble()
    fun isMfaEnabled(): Boolean = prefs.getBoolean(KEY_MFA_ENABLED, false)

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null
}