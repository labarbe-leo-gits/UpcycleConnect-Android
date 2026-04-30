package com.pingcorp.upcycleconnect

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("duration_minutes") val durationMin: Int,
    @SerialName("created_at") val createdAt: String
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
