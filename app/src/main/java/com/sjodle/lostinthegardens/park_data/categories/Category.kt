package com.sjodle.lostinthegardens.park_data.categories

import androidx.compose.ui.graphics.Color
import org.json.JSONObject

data class CategoryColor(
    val red: Float,
    val green: Float,
    val blue: Float,
) {
    companion object {
        fun fromJsonObject(jsonObject: JSONObject): CategoryColor {
            return CategoryColor(
                red = jsonObject.getDouble("red").toFloat(),
                green = jsonObject.getDouble("green").toFloat(),
                blue = jsonObject.getDouble("blue").toFloat(),
            )
        }
    }

    val color: Color
        get() = Color(red, green, blue)
}

data class Category(
    val index: Int,
    val name: String,
    val color: CategoryColor,
) {
    companion object {
        fun fromJsonObject(jsonObject: JSONObject): Category {
            val index = jsonObject.getInt("index")
            val name = jsonObject.getString("name")
            val color = CategoryColor.fromJsonObject(jsonObject.getJSONObject("color"))
            return Category(index, name, color)
        }
    }
}

data class CategoryFile(
    val categories: Map<String, Category>
) {
    companion object {
        fun fromJson(json: String): CategoryFile {
            val categoryFile = JSONObject(json)
            val categories = mutableMapOf<String, Category>()
            categoryFile.keys().forEach {
                val category = Category.fromJsonObject(categoryFile.getJSONObject(it))
                categories.put(it, category)
            }
            return CategoryFile(categories)
        }
    }

    fun getCategory(key: String): Category? {
        return if (categories.contains(key)) {
            categories[key]
        } else {
            null
        }
    }
}
