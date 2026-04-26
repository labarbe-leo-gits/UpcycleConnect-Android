package com.pingcorp.upcycleconnect

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ApiService {
    @GET("/conteneurs")
    suspend fun getContainers(@Header("Authorization") token: String): Response<List<Container>>
    @GET("/annonces")
    suspend fun getAnnonces(@Header("Authorization") token: String): Response<List<Annonce>>
    @GET("/users/{id}/bans")
    suspend fun getUserBan(
        @Path("id") userId: String,
        @Header("Authorization") token: String
    ): Response<List<Ban>>

    @GET("/users/{id}")
    suspend fun getUserProfile(
        @Path("id") userId: String,
        @Header("Authorization") token: String
    ): Response<UserData>
}
