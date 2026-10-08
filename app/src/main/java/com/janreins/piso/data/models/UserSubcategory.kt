package com.janreins.piso.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Custom user subcategory attached to a parent category by name and kind.
 *
 * The kind is needed because the same category name can exist for both income and expense
 * (e.g. "Other"); without it those two categories would share their subcategories.
 */
@Entity(tableName = "user_subcategories")
data class UserSubcategory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentCategoryName: String,
    val name: String,
    val isArchived: Boolean = false,
    val parentKind: String = "EXPENSE" // "INCOME" or "EXPENSE", same as the parent's UserCategory.kind
)
