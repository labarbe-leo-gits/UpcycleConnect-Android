package com.pingcorp.upcycleconnect

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.Serial

@Serializable
data class Container(
    val id: String,
    val name: String,
    @SerialName("city") val city: String,
    @SerialName("road") val road: String,
    @SerialName("postal_code") val postalCode: String,
    @SerialName("number") val number: String,
    @SerialName("capacity") val capacity: Int,
    @SerialName("current_fill") val currentFill: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class Annonce(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val status: Int,
    @SerialName("view_count") val viewCount: Int,
    val description: String? = null,
    val price: Double? = null,
    @SerialName("poids_materiaux") val poidsMateriaux: Double? = null,
    @SerialName("facteur_id") val facteurId: String? = null,
    @SerialName("type_materiaux") val typeMateriaux: String? = null,
    @SerialName("estimation_score") val estimationScore: Double? = null,
    @SerialName("upcycling_score") val upcyclingScore: Double? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("item_state") val itemState: Int,
    @SerialName("ad_campaign_id") val adCampaignId: String? = null,
    @SerialName("seller_user_type") val sellerUserType: Int? = null,
    @SerialName("seller_username") val sellerUsername: String? = null,
    @SerialName("seller_rating") val sellerRating: Double? = null,
    @SerialName("item_state_label") val itemStateLabel: String? = null,
    val promoted: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class Ban(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("reason") val reason: String,
    @SerialName("banned_at") val bannedAt: String,
    @SerialName("banned_by") val bannedBy: String,
    @SerialName("duration_days") val duration: Int

)

@Serializable
data class Project(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("annonce_id") val annonceId: String? = null,
    val title: String,
    val description: String,
    val status: Int,
    @SerialName("ai_generated") val aiGenerated: Int,
    val views: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ProjectStep(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("step_order") val stepOrder: Int,
    val title: String,
    val description: String,
    @SerialName("duration_minutes") val durationMin: Int? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ProjectComment(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("parent_id") val parentId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ProjectLikes(
    val id: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ProjectStepMaterial(
    @SerialName("step_id") val stepId: String,
    @SerialName("facteur_id") val facteurId: String,
    val quantity: Float,
    val nom: String
)

@Serializable
data class MaterialFactor(
    val id: String,
    val nom: String,
    @SerialName("facteur_co2") val factor: Float,
    @SerialName("facteur_energie") val energy: Float
)

@Serializable
data class Deposit(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("conteneur_id") val conteneurId: String,
    @SerialName("object_name") val objectName: String,
    @SerialName("object_description") val objectDescription: String,
    @SerialName("object_state") val objectState: Int? = null,
    @SerialName("status") val status: Int,
    @SerialName("barcode") val barcode: String? = null,
    @SerialName("retrieval_code") val retrievalCode: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class UpdateDepositStatusDto(
    val status: Int,
    val id: String? = null
)

@Serializable
data class DepositFile(
    val id: String,
    @SerialName("deposit_id") val depositId: String,
    val filename: String,
    @SerialName("original_name") val originalName: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class DepositFileInput(
    val filename: String,
    @SerialName("original_name") val originalName: String
)

@Serializable
data class ConteneurItem(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("conteneur_id") val conteneurId: String,
    @SerialName("object_name") val objectName: String,
    @SerialName("object_description") val objectDescription: String,
    @SerialName("object_state") val objectState: Int? = null,
    @SerialName("status") val status: Int,
    @SerialName("barcode") val barcode: String? = null,
    @SerialName("retrieval_code") val retrievalCode: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("files") val files: List<DepositFile>
)

@Serializable
data class User(
    val id: String,
    val username: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String? = null,
    @SerialName("company_name") val companyName: String? = null
)

@Serializable
data class UpdateUserDto(
    val username: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String? = null
)

@Serializable
data class DeleteUserRequest(
    val username: String,
    val password: String? = null,
    @SerialName("mfa_code") val code: String? = null
)

@Serializable
data class Notification(
    val id: String,
    @SerialName("annonce_id") val annonceId: String,
    @SerialName("user_id") val userId: String,
    val message: String,
    val read: Boolean,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class PaymentIntentRequest(
    @SerialName("product_uuid") val productUuid: String,
    val token: String? = null
)

@Serializable
data class PaymentIntentResponse(
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("publishable_key") val publishableKey: String? = null
)

@Serializable
data class VerifyPaymentRequest(
    @SerialName("payment_intent") val paymentIntent: String,
    @SerialName("product_uuid") val productUuid: String,
    val token: String? = null
)

@Serializable
data class VerifyPaymentResponse(
    val success: Boolean,
    val message: String? = null
)

@Serializable
data class Order(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String? = null,
    val amount: Double,
    @SerialName("transaction_id") val transactionId: String,
    val status: Int,
    @SerialName("event_id") val eventId: String? = null,
    @SerialName("event_availability_id") val eventAvailabilityId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class UpdateAnnonceStatusDto(
    val status: Int
)

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiConfig? = null
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String,
    val thought: Boolean? = null
)

@Serializable
data class GeminiConfig(
    val temperature: Float? = null,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent
)

@Serializable
data class CreateProjectDto(
    @SerialName("user_id") val userId: String,
    val title: String,
    val description: String,
    val status: Int = 1,
    @SerialName("ai_generated") val aiGenerated: Int = 0,
    @SerialName("annonce_id") val annonceId: String? = null
)

@Serializable
data class CreateProjectStepDto(
    @SerialName("id") val id: String = "00000000-0000-0000-0000-000000000000",
    @SerialName("project_id") val projectId: String,
    @SerialName("step_order") val stepOrder: Int,
    val title: String,
    val description: String,
    @SerialName("duration_minutes") val durationMin: Int? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class AiDetectionResponse(
    @SerialName("ai_generated") val aiGenerated: Boolean
)
