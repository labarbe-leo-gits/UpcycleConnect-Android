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

    @GET("/projects/{id}")
    suspend fun getProject(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): Response<Project>

    @GET("/projects/{id}/steps")
    suspend fun getProjectSteps(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): Response<List<ProjectStep>>

    @GET("/users/{id}/bans")
    suspend fun getUserBan(
        @Path("id") userId: String,
        @Header("Authorization") token: String
    ): Response<JsonElement>

    @GET("/users/{id}")
    suspend fun getUserProfile(
        @Path("id") userId: String,
        @Header("Authorization") token: String
    ): Response<User>

    @GET("/annonces/{id}")
    suspend fun getAnnonce(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): Response<Annonce>

    @GET("/conteneurs/{id}/items")
    suspend fun getContainerItems(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): Response<JsonElement>

    @GET("/deposits/{id}")
    suspend fun getDeposit(
        @Path("id") id: String,
        @Header("Authorization") token: String
    ): Response<ConteneurItem>

    @POST("pages/common/create-payment-intent")
    suspend fun createPaymentIntent(
        @Body body: PaymentIntentRequest,
        @Header("Authorization") token: String
    ): Response<PaymentIntentResponse>

    @POST("pages/common/verify-payment")
    suspend fun verifyPayment(
        @Body body: VerifyPaymentRequest,
        @Header("Authorization") token: String
    ): Response<VerifyPaymentResponse>

    @POST("/orders")
    suspend fun createOrder(
        @Body body: Order,
        @Header("Authorization") token: String
    ): Response<Order>

    @retrofit2.http.PATCH("/annonces/{id}")
    suspend fun updateAnnonceStatus(
        @Path("id") id: String,
        @Body body: UpdateAnnonceStatusDto,
        @Header("Authorization") token: String
    ): Response<JsonElement>

    @retrofit2.http.PATCH("/deposits/{id}/status")
    suspend fun updateDepositStatus(
        @Path("id") id: String,
        @Body body: StatusUpdate,
        @Header("Authorization") token: String
    ): Response<JsonElement>

    @POST("/projects")
    suspend fun createProject(
        @Body body: CreateProjectDto,
        @Header("Authorization") token: String
    ): Response<Project>

    @retrofit2.http.PATCH("/projects/{id}")
    suspend fun updateProject(
        @Path("id") id: String,
        @Body body: CreateProjectDto,
        @Header("Authorization") token: String
    ): Response<JsonElement>

    @POST("/projects/{id}/steps")
    suspend fun createProjectStep(
        @Path("id") projectId: String,
        @Body body: CreateProjectStepDto,
        @Header("Authorization") token: String
    ): Response<ProjectStep>

    @retrofit2.http.PATCH("/projects/{id}/steps/{sID}")
    suspend fun updateProjectStep(
        @Path("id") projectId: String,
        @Path("sID") stepId: String,
        @Body body: CreateProjectStepDto,
        @Header("Authorization") token: String
    ): Response<JsonElement>

    @retrofit2.http.DELETE("/projects/{id}/steps/{sID}")
    suspend fun deleteProjectStep(
        @Path("id") projectId: String,
        @Path("sID") stepId: String,
        @Header("Authorization") token: String
    ): Response<JsonElement>
}

interface GeminiApiService {
    @POST("v1beta/models/gemma-4-26b-a4b-it:generateContent")
    suspend fun generateContent(
        @retrofit2.http.Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}
