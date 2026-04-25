package com.pingcorp.upcycleconnect

import com.pingcorp.upcycleconnect.Verify2FARequest
import com.pingcorp.upcycleconnect.Verify2FAResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("/2fa/verify")
    suspend fun verify2FA(
        @Body request: Verify2FARequest
    ): Response<Verify2FAResponse>
}