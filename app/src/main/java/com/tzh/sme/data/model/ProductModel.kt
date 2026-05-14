package com.tzh.sme.data.model

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class ProductModel(
    val id: String = "",
    val barcode: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val quantity: Int = 0,
//    @get:Exclude @set:Exclude var category: CategoryModel = CategoryModel(),
    val categoryId : String = "",
    val imagePaths: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
//    // Custom getter/setter to handle both String and Map types from Firestore
//    @get:PropertyName("category")
//    @set:PropertyName("category")
//    var categoryForFirestore: Any?
//        get() = category
//        set(value) {
//            category = when (value) {
//                is String -> CategoryModel(name = value)
//                is Map<*, *> -> {
//                    CategoryModel(
//                        name = value["name"] as? String ?: "",
//                        id = value["id"] as? String ?: "",
//                        icon = value["icon"] as? String ?: ""
//                    )
//                }
//                else -> CategoryModel()
//            }
//        }
}
