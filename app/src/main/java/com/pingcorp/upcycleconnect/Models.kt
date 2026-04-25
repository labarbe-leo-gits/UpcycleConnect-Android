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
