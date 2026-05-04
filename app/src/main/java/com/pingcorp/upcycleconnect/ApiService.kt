package com.pingcorp.upcycleconnect

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import kotlinx.serialization.json.JsonElement

interface ApiService {
    @GET("/conteneurs")
    suspend fun getContainers(@Header("Authorization") token: String): Response<JsonElement>
    @GET("/conteneurs/{id}")
    suspend fun getContainer(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): Response<JsonElement>
    @GET("/annonces")
    suspend fun getAnnonces(@Header("Authorization") token: String): Response<JsonElement>
    @GET("/projects")
    suspend fun getProjects(@Header("Authorization") token: String): Response<JsonElement>
    @GET("/users/{id}/bans")
    suspend fun getUserBan(
        @Path("id") userId: String,
        @Header("Authorization") token: String
    ): Response<JsonElement>

    @GET("/users/{id}")
    suspend fun getUserProfile(
        @Path("id") userId: String,
        @Header("Authorization") token: String
    ): Response<JsonElement>
}
