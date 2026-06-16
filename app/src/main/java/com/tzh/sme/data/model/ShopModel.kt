package com.tzh.sme.data.model

data class ShopModel(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)
