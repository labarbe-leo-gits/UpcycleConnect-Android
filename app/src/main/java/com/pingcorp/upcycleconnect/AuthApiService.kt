package com.pingcorp.upcycleconnect

import com.pingcorp.upcycleconnect.Verify2FARequest
import com.pingcorp.upcycleconnect.Verify2FAResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface AuthApiService {
    @POST("/2fa/verify")
    suspend fun verify2FA(
        @Body request: Verify2FARequest
    ): Response<Verify2FAResponse>

    @POST("/users/{id}/2fa/setup")
    suspend fun setupMFA(
        @Path("id") userId: String,
        @Header("Authorization") token: String
    ): Response<MfaSetupResponse>

    @POST("/users/{id}/2fa/enable")
    suspend fun enableMFA(
        @Path("id") userId: String,
        @Header("Authorization") token: String,
        @Body request: MfaEnableRequest
    ): Response<MfaEnableResponse>

    @GET("/users/{id}/2fa-info")
    suspend fun getMFAInfo(
        @Path("id") userId: String,
        @Header("Authorization") token: String
    ): Response<MfaInfoResponse>

    @POST("/users/{id}/2fa/disable")
    suspend fun disableMFA(
        @Path("id") userId: String,
        @Header("Authorization") token: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<MfaEnableResponse>
}
