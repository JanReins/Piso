package com.janreins.piso.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Custom user subcategory attached to a parent category by name.
 */
@Entity(tableName = "user_subcategories")
data class UserSubcategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentCategoryName: String,
    val name: String,
    val isArchived: Boolean = false
)
