package com.janreins.piso.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Monthly budget limit for a category in a specific month (yyyy-MM).
 */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String,
    val limitAmount: Double,
    val monthKey: String // yyyy-MM
)
