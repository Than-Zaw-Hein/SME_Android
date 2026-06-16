package com.tzh.sme.data

import com.tzh.sme.data.model.CategoryModel

object DefaultData {
    val defaultCategory = CategoryModel(
        id = "__all__",
        name = "All",
        icon = "📦"
    )

    val generalCategory = CategoryModel(
        id = "generalId0",
        name = "General",
        icon = "📦"
    )
}