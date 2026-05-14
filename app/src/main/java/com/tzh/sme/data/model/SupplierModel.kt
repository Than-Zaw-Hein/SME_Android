package com.tzh.sme.data.model

data class SupplierModel(
    val id: String = "",
    val name: String = "",
    val contact: String = "",
    val address: String = "",
    val category: String = "" // Optional: e.g., "Wholesale", "Manufacturer"
)
