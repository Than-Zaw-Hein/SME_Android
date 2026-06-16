package com.tzh.sme.data.model


data class ProductModel(
    val id: String = "",
    val barcode: String = "",
    val name: String = "",
    val description: String = "",
    val sellPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val quantity: Int = 0,
    val minStockLevel: Int = 0,
    val categoryId : String = "",
    val supplierId : String = "",
    val imagePaths: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)
