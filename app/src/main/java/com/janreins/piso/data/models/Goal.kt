package com.janreins.piso.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a savings goal (e.g., Emergency Fund, Laptop, Travel).
 */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val isCompleted: Boolean = false,
    val deadlineMillis: Long? = null,
    val accountId: Long? = null // Optional account where savings are kept
)
