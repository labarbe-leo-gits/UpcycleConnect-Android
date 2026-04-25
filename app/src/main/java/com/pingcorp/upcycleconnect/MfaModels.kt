package com.pingcorp.upcycleconnect

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
    val user_type: Int = 0
)