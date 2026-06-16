package com.tzh.sme.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SystemImage(
    val id: String = "",
    val url: String = "",
    val path: String = "",
    val relatedEntityId: String = "",
    val shopId: String = "",
    val timestamp: Double = 0.0
)
