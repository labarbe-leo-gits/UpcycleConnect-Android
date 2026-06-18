package com.pingcorp.upcycleconnect

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Verify2FARequest(
    val temp_token: String,
    val code: String
)

@Serializable
data class Verify2FAResponse(
    val token: String,
    val user: UserData
)

@Serializable
data class UserData(
    val id: String,
    val email: String,
    val username: String? = null,
    val first_name: String? = null,
    val last_name: String? = null,
    val balance: Double = 0.0,
    val user_type: Int = 0,
    val mfa_enabled: Boolean = false,
    val company_name: String? = null
)

@Serializable
data class MfaSetupResponse(
    val secret: String,
    @SerialName("otp_url") val otpUrl: String
)

@Serializable
data class MfaEnableRequest(
    val secret: String,
    val code: String
)

@Serializable
data class MfaEnableResponse(
    val success: Boolean,
    val message: String? = null
)

@Serializable
data class MfaInfoResponse(
    @SerialName("enabled") val mfaEnabled: Boolean
)
